package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.InsertItemPedidoDTO;
import com.umc.pediupatrao.dto.InsertPedidoDTO;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
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
import java.util.List;

@Service
public class PedidoService {

    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    public PedidoDTO criarPedido(InsertPedidoDTO dto) {
        if (!clienteRepository.existsById(dto.getClienteId())) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }

        Pedido pedido = PedidoMapper.toEntity(dto);
        pedido.setItens(dto.getItens().stream().map(this::montarItem).toList());
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        calcularValores(pedido);

        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setRegistradoPor(SecurityContextHolder.getContext().getAuthentication().getName());
        pedido.setDataHoraEntrada(LocalDateTime.now());

        return PedidoMapper.toDto(pedidoRepository.save(pedido));
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

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        try {
            pedido.setStatus(StatusPedido.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Status inválido: " + status);
        }
        return pedidoRepository.save(pedido);
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
