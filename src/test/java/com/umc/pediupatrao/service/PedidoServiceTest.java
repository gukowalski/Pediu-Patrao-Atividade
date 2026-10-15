package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.InsertItemPedidoDTO;
import com.umc.pediupatrao.dto.InsertPedidoDTO;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.ConflictException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("gerente01", null, "ROLE_GERENTE"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void criarPedidoCalculaTotalComPrecoDoProduto() {
        Produto produto = new Produto();
        produto.setId("p1");
        produto.setNome("Pizza Calabresa");
        produto.setPreco(45.90);
        produto.setAtivo(true);

        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        InsertPedidoDTO dto = new InsertPedidoDTO("c1", TipoAtendimento.ENTREGA,
                List.of(new InsertItemPedidoDTO("p1", 2)), null);
        PedidoDTO result = pedidoService.criarPedido(dto);

        assertEquals(new BigDecimal("91.80"), result.getValorTotal());
        assertEquals(StatusPedido.RECEBIDO, result.getStatus());
        assertEquals("gerente01", result.getRegistradoPor());
        assertNotNull(result.getDataHoraEntrada());
        verify(auditoriaService).registrar(eq(TipoOperacao.CRIACAO_PEDIDO), eq("Pedido"), any(), anyList(), isNull());
    }

    @Test
    void criarPedidoComProdutoInativoLancaExcecao() {
        Produto produto = new Produto();
        produto.setId("p1");
        produto.setNome("Pizza");
        produto.setPreco(40.0);
        produto.setAtivo(false);

        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto));

        InsertPedidoDTO dto = new InsertPedidoDTO("c1", TipoAtendimento.ENTREGA,
                List.of(new InsertItemPedidoDTO("p1", 1)), null);

        assertThrows(RegraNegocioException.class, () -> pedidoService.criarPedido(dto));
    }

    @Test
    void transicaoInvalidaLancaConflito() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.EM_PREPARACAO)));

        assertThrows(ConflictException.class, () -> pedidoService.avancarStatus("1", StatusPedido.FINALIZADO));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void saidaParaEntregaRegistraResponsavelEDataHora() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.PRONTO)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO result = pedidoService.avancarStatus("1", StatusPedido.SAIU_PARA_ENTREGA);

        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, result.getStatus());
        assertEquals("gerente01", result.getResponsavelSaida());
        assertNotNull(result.getDataHoraSaida());
    }

    @Test
    void descontoAcimaDeVintePorCentoLancaExcecao() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.RECEBIDO)));

        assertThrows(RegraNegocioException.class, () -> pedidoService.aplicarDesconto("1", new BigDecimal("25")));
    }

    @Test
    void descontoRecalculaTotalNoServidor() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.RECEBIDO)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO result = pedidoService.aplicarDesconto("1", new BigDecimal("10"));

        assertEquals(new BigDecimal("10.00"), result.getValorDesconto());
        assertEquals(new BigDecimal("90.00"), result.getValorTotal());
        verify(auditoriaService).registrar(eq(TipoOperacao.DESCONTO_PEDIDO), eq("Pedido"), eq("1"), anyList(), isNull());
    }

    @Test
    void descontoAposSaidaLancaConflito() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.SAIU_PARA_ENTREGA)));

        assertThrows(ConflictException.class, () -> pedidoService.aplicarDesconto("1", new BigDecimal("10")));
    }

    @Test
    void cancelamentoPreservaValoresERegistraJustificativa() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.PRONTO)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO result = pedidoService.cancelar("1", "Cliente desistiu do pedido");

        assertEquals(StatusPedido.CANCELADO, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getValorTotal());
        assertEquals("gerente01", result.getCanceladoPor());
        assertEquals("Cliente desistiu do pedido", result.getJustificativaCancelamento());
        verify(auditoriaService).registrar(eq(TipoOperacao.CANCELAMENTO_PEDIDO), eq("Pedido"), eq("1"),
                anyList(), eq("Cliente desistiu do pedido"));
    }

    @Test
    void cancelamentoAposSaidaLancaConflito() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.RETIRADO)));

        assertThrows(ConflictException.class, () -> pedidoService.cancelar("1", "Cliente desistiu do pedido"));
    }

    @Test
    void pedidoCanceladoNaoPodeMudarDeStatus() {
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido(StatusPedido.CANCELADO)));

        assertThrows(ConflictException.class, () -> pedidoService.avancarStatus("1", StatusPedido.EM_PREPARACAO));
    }

    private Pedido pedido(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("1");
        pedido.setTipoAtendimento(TipoAtendimento.ENTREGA);
        pedido.setItens(new ArrayList<>(List.of(
                new ItemPedido("p1", "Pizza", new BigDecimal("50.00"), 2, new BigDecimal("100.00")))));
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        pedido.setValorDesconto(new BigDecimal("0.00"));
        pedido.setValorTotal(new BigDecimal("100.00"));
        pedido.setStatus(status);
        return pedido;
    }
}
