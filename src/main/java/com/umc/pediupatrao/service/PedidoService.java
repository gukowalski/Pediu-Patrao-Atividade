package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.InsertItemPedidoDTO;
import com.umc.pediupatrao.dto.InsertPedidoDTO;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.ConflictException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ResourceNotFoundException;
import com.umc.pediupatrao.mapper.PedidoMapper;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");
    private static final String ENTIDADE = "Pedido";

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private AuditoriaService auditoriaService;

    public PedidoDTO criarPedido(InsertPedidoDTO dto) {
        if (!clienteRepository.existsById(dto.getClienteId())) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }

        Pedido pedido = PedidoMapper.toEntity(dto);
        pedido.setItens(dto.getItens().stream().map(this::montarItem).toList());
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        calcularValores(pedido);

        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setRegistradoPor(usernameAutenticado());
        pedido.setDataHoraEntrada(LocalDateTime.now());

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.CRIACAO_PEDIDO, ENTIDADE, salvo.getId(), List.of(
                CampoAlterado.de("status", null, salvo.getStatus()),
                CampoAlterado.de("itens", null, descreverItens(salvo)),
                CampoAlterado.de("valorTotal", null, salvo.getValorTotal())), null);

        return PedidoMapper.toDto(salvo);
    }

    public List<PedidoDTO> listarPedidos() {
        List<Pedido> result = pedidoRepository.findAll();
        return result.stream().map(PedidoMapper::toDto).toList();
    }

    public PedidoDTO buscarPorId(String id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        return PedidoMapper.toDto(pedido);
    }

    public PedidoDTO avancarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = buscarPedido(id);
        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == null || !statusAnterior.podeAvancarPara(novoStatus, pedido.getTipoAtendimento())) {
            throw new ConflictException("Não é possível alterar de " + statusAnterior + " para " + novoStatus);
        }

        String usuario = usernameAutenticado();
        List<CampoAlterado> alteracoes = new ArrayList<>();
        alteracoes.add(CampoAlterado.de("status", statusAnterior, novoStatus));

        pedido.setStatus(novoStatus);
        if (novoStatus.isSaida()) {
            pedido.setResponsavelSaida(usuario);
            pedido.setDataHoraSaida(LocalDateTime.now());
            alteracoes.add(CampoAlterado.de("responsavelSaida", null, pedido.getResponsavelSaida()));
            alteracoes.add(CampoAlterado.de("dataHoraSaida", null, pedido.getDataHoraSaida()));
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_STATUS_PEDIDO, ENTIDADE, salvo.getId(), alteracoes, null);
        return PedidoMapper.toDto(salvo);
    }

    public PedidoDTO cancelar(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == null || !statusAnterior.isAntesDaSaida()) {
            throw new ConflictException("Pedido com status " + statusAnterior + " não pode ser cancelado");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraNegocioException("A justificativa é obrigatória");
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoPor(usernameAutenticado());
        pedido.setDataHoraCancelamento(LocalDateTime.now());
        pedido.setJustificativaCancelamento(justificativa);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, ENTIDADE, salvo.getId(),
                List.of(CampoAlterado.de("status", statusAnterior, StatusPedido.CANCELADO)), justificativa);
        return PedidoMapper.toDto(salvo);
    }

    public PedidoDTO aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscarPedido(id);

        if (pedido.getStatus() == null || !pedido.getStatus().isAntesDaSaida()) {
            throw new ConflictException("Desconto não permitido para pedido com status " + pedido.getStatus());
        }
        if (percentual == null || percentual.signum() <= 0 || percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new RegraNegocioException("O desconto deve ser maior que 0% e no máximo 20%");
        }

        BigDecimal percentualAnterior = pedido.getDescontoPercentual();
        BigDecimal valorDescontoAnterior = pedido.getValorDesconto();
        BigDecimal valorTotalAnterior = pedido.getValorTotal();

        pedido.setDescontoPercentual(percentual.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN));
        calcularValores(pedido);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.DESCONTO_PEDIDO, ENTIDADE, salvo.getId(), List.of(
                CampoAlterado.de("descontoPercentual", percentualAnterior, salvo.getDescontoPercentual()),
                CampoAlterado.de("valorDesconto", valorDescontoAnterior, salvo.getValorDesconto()),
                CampoAlterado.de("valorTotal", valorTotalAnterior, salvo.getValorTotal())), null);
        return PedidoMapper.toDto(salvo);
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String descreverItens(Pedido pedido) {
        return pedido.getItens().stream()
                .map(item -> item.getQuantidade() + "x " + item.getNomeProduto())
                .collect(Collectors.joining(", "));
    }

    // Preço e nome vêm do cadastro do produto, nunca da requisição
    private ItemPedido montarItem(InsertItemPedidoDTO dto) {
        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + dto.getProdutoId()));

        if (Boolean.FALSE.equals(produto.getAtivo())) {
            throw new RegraNegocioException("Produto inativo: " + produto.getNome());
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("Produto sem preço válido: " + produto.getNome());
        }

        BigDecimal precoUnitario = BigDecimal.valueOf(produto.getPreco())
                .setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
        BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(dto.getQuantidade()));

        return new ItemPedido(produto.getId(), produto.getNome(), precoUnitario, dto.getQuantidade(), subtotal);
    }

    // Recalcula subtotal, desconto e total; será reaproveitado no desconto do GERENTE (RF04)
    private void calcularValores(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);

        BigDecimal valorDesconto = subtotal.multiply(pedido.getDescontoPercentual())
                .divide(CEM, CASAS_DECIMAIS, RoundingMode.HALF_EVEN);

        pedido.setSubtotal(subtotal);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(subtotal.subtract(valorDesconto));
    }
}
