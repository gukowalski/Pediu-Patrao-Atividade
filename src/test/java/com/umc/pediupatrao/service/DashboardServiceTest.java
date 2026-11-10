package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.DashboardFiltro;
import com.umc.pediupatrao.dto.dashboard.DistribuicaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.EvolucaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.PedidosRecentesDTO;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosDTO;
import com.umc.pediupatrao.dto.dashboard.ResumoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.StatusPedidosDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoriaDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorTipoAtendimentoDTO;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.DashboardRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes das regras de cálculo do dashboard. O DashboardRepository é simulado por uma lista em memória
 * que aplica o mesmo filtro de período e status da consulta real ao MongoDB.
 */
class DashboardServiceTest {

    // "Agora" fixo: segunda-feira, 09/11/2026 às 21:00
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 9, 21, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();

    private final List<Pedido> banco = new ArrayList<>();
    private ClienteRepository clienteRepository;
    private DashboardRepository dashboardRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        dashboardRepository = mock(DashboardRepository.class);
        // Histórico começa bem antes dos períodos testados: comparativo com base completa
        when(dashboardRepository.dataPrimeiroPedido()).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 1, 0, 0)));
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        clienteRepository = mock(ClienteRepository.class);

        when(dashboardRepository.buscarPedidos(any(), any())).thenAnswer(inv -> {
            DashboardFiltro f = inv.getArgument(0);
            return banco.stream()
                    .filter(p -> p.getDataHoraEntrada() == null
                            || (!p.getDataHoraEntrada().isBefore(f.inicio()) && p.getDataHoraEntrada().isBefore(f.fimExclusivo())))
                    .filter(p -> f.status() == null || f.status() == p.getStatus())
                    .filter(p -> f.tipoAtendimento() == null || f.tipoAtendimento() == p.getTipoAtendimento())
                    .toList();
        });
        when(produtoRepository.findAll()).thenReturn(List.of(
                produto("pz", "Pizza Calabresa", "Pizza"),
                produto("rf", "Refrigerante 2L", "Bebida"),
                produto("es", "Esfiha de Carne", "Esfiha")));

        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new DashboardService(dashboardRepository, produtoRepository, clienteRepository, clock);
    }

    // ------------------------------------------------------------------ filtros

    @Test
    void filtroPadraoSaoOsUltimos30DiasAteHoje() {
        DashboardFiltro filtro = service.criarFiltro(null, null, null, null, "  ");

        assertEquals(HOJE, filtro.dataFim());
        assertEquals(HOJE.minusDays(29), filtro.dataInicio());
        assertEquals(30, filtro.dias());
        assertNull(filtro.categoria());
    }

    @Test
    void filtroRejeitaDataInicialPosteriorAFinal() {
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> service.criarFiltro(HOJE, HOJE.minusDays(1), null, null, null));
        assertEquals("A data inicial não pode ser posterior à data final", e.getMessage());
    }

    @Test
    void filtroRejeitaPeriodoMaiorQue366Dias() {
        assertThrows(RegraNegocioException.class,
                () -> service.criarFiltro(HOJE.minusDays(366), HOJE, null, null, null));
        // 366 dias exatos é permitido
        assertEquals(366, service.criarFiltro(HOJE.minusDays(365), HOJE, null, null, null).dias());
    }

    @Test
    void periodoAnteriorTemAMesmaDuracao() {
        DashboardFiltro anterior = service.criarFiltro(HOJE.minusDays(6), HOJE, null, null, null).periodoAnterior();

        assertEquals(HOJE.minusDays(13), anterior.dataInicio());
        assertEquals(HOJE.minusDays(7), anterior.dataFim());
    }

    // ------------------------------------------------------------------ RF01

    @Test
    void resumoIgnoraCanceladosNoFaturamentoECalculaIndicadores() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "100.00", "10.00",
                30, item("pz", 2, "45.00", "Pizza"), item("rf", 1, "10.00", "Bebida")));
        banco.add(pedido(HOJE.atTime(20, 0), StatusPedido.FINALIZADO, TipoAtendimento.RETIRADA, "50.00", "0.00",
                20, item("pz", 1, "50.00", "Pizza")));
        banco.add(pedido(HOJE.atTime(20, 30), StatusPedido.CANCELADO, TipoAtendimento.ENTREGA, "80.00", "0.00",
                null, item("pz", 2, "40.00", "Pizza")));
        banco.add(pedido(HOJE.atTime(20, 45), StatusPedido.EM_PREPARACAO, TipoAtendimento.ENTREGA, "30.00", "0.00",
                null, item("es", 5, "6.00", "Esfiha")));
        // Período anterior (ontem): 1 pedido de R$ 90,00
        banco.add(pedido(HOJE.minusDays(1).atTime(20, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA,
                "90.00", "0.00", 25, item("pz", 2, "45.00", "Pizza")));

        ResumoVendasDTO r = service.resumo(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(new BigDecimal("180.00"), r.faturamento());         // 100 + 50 + 30
        assertEquals(4, r.totalPedidos());
        assertEquals(3, r.pedidosValidos());
        assertEquals(new BigDecimal("60.00"), r.ticketMedio());          // 180 / 3
        assertEquals(9, r.itensVendidos());                               // 3 + 1 + 5 (cancelado fora)
        assertEquals(new BigDecimal("10.00"), r.totalDescontos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(new BigDecimal("25.0"), r.taxaCancelamento());       // 1 de 4
        assertEquals(new BigDecimal("80.00"), r.valorCancelado());
        assertEquals(1, r.pedidosEmAndamento());
        assertEquals(new BigDecimal("25.0"), r.tempoMedioAtendimentoMinutos()); // (30 + 20) / 2

        assertEquals(new BigDecimal("90.00"), r.comparativo().faturamentoAnterior());
        assertEquals(new BigDecimal("100.0"), r.comparativo().variacaoFaturamento()); // 180 vs 90
        assertEquals(new BigDecimal("200.0"), r.comparativo().variacaoPedidos());     // 3 vs 1
        assertEquals(AGORA, r.geradoEm());
    }

    @Test
    void comparativoSemHistoricoSuficienteNaoCalculaVariacao() {
        // Primeiro pedido registrado é de ontem: o período anterior (7 dias antes) está incompleto
        when(dashboardRepository.dataPrimeiroPedido()).thenReturn(Optional.of(HOJE.minusDays(1).atTime(12, 0)));
        banco.add(pedido(HOJE.minusDays(1).atTime(12, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "10.00", "0", 20));
        banco.add(pedido(HOJE.atTime(12, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "500.00", "0", 20));

        ResumoVendasDTO r = service.resumo(service.criarFiltro(HOJE, HOJE, null, null, null));
        assertTrue(r.comparativo().baseCompleta()); // período anterior = ontem, já coberto pelo histórico

        ResumoVendasDTO semana = service.resumo(service.criarFiltro(HOJE.minusDays(6), HOJE, null, null, null));
        assertFalse(semana.comparativo().baseCompleta());
        assertNull(semana.comparativo().variacaoFaturamento());
        assertNull(semana.comparativo().variacaoPedidos());
    }

    @Test
    void resumoSemPedidosRetornaZerosSemErro() {
        ResumoVendasDTO r = service.resumo(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(new BigDecimal("0.00"), r.faturamento());
        assertEquals(new BigDecimal("0.00"), r.ticketMedio());
        assertEquals(new BigDecimal("0.0"), r.taxaCancelamento());
        assertNull(r.tempoMedioAtendimentoMinutos());
        assertNull(r.comparativo().variacaoFaturamento()); // sem base de comparação
    }

    @Test
    void resumoToleraPedidosComDadosIncompletos() {
        Pedido semValores = pedido(HOJE.atTime(12, 0), StatusPedido.RECEBIDO, TipoAtendimento.ENTREGA, "0", "0", null);
        semValores.setValorTotal(null);
        semValores.setValorDesconto(null);
        semValores.setItens(null);
        Pedido semData = pedido(null, StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "999.00", "0", null);
        banco.add(semValores);
        banco.add(semData);

        ResumoVendasDTO r = service.resumo(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(1, r.totalPedidos());                       // o pedido sem data é descartado
        assertEquals(new BigDecimal("0.00"), r.faturamento());
        assertEquals(0, r.itensVendidos());
    }

    @Test
    void variacaoPercentual() {
        assertEquals(new BigDecimal("-25.0"), DashboardService.variacao(new BigDecimal("75"), new BigDecimal("100")));
        assertNull(DashboardService.variacao(BigDecimal.TEN, BigDecimal.ZERO));
    }

    // ------------------------------------------------------------------ RF02

    @Test
    void evolucaoDiariaPreencheDiasSemVenda() {
        banco.add(pedido(HOJE.minusDays(2).atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "40.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "60.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 30), StatusPedido.CANCELADO, TipoAtendimento.ENTREGA, "10.00", "0", null));

        EvolucaoVendasDTO r = service.evolucao(service.criarFiltro(HOJE.minusDays(2), HOJE, null, null, null), Agrupamento.DIA);

        assertEquals(3, r.pontos().size());
        assertEquals("07/11", r.pontos().get(0).rotulo());
        assertEquals(new BigDecimal("40.00"), r.pontos().get(0).faturamento());
        assertEquals(0, r.pontos().get(1).pedidos());
        assertEquals(new BigDecimal("0.00"), r.pontos().get(1).faturamento());
        assertEquals(1, r.pontos().get(2).pedidos());
        assertEquals(1, r.pontos().get(2).cancelados());
        assertEquals(new BigDecimal("60.00"), r.pontos().get(2).faturamento());
    }

    @Test
    void evolucaoSemanalAgrupaPelaSegundaFeira() {
        // 02/11/2026 e 09/11/2026 são segundas-feiras
        banco.add(pedido(LocalDate.of(2026, 11, 3).atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "10.00", "0", 20));
        banco.add(pedido(LocalDate.of(2026, 11, 8).atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "20.00", "0", 20));
        banco.add(pedido(LocalDate.of(2026, 11, 9).atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "30.00", "0", 20));

        EvolucaoVendasDTO r = service.evolucao(
                service.criarFiltro(LocalDate.of(2026, 11, 3), HOJE, null, null, null), Agrupamento.SEMANA);

        assertEquals(2, r.pontos().size());
        assertEquals(LocalDate.of(2026, 11, 2), r.pontos().get(0).inicio());
        assertTrue(r.pontos().get(0).parcial());   // período começa na terça, 03/11
        assertTrue(r.pontos().get(1).parcial());   // semana de 09/11 só tem a segunda-feira
        assertFalse(service.evolucao(service.criarFiltro(HOJE.minusDays(2), HOJE, null, null, null),
                Agrupamento.DIA).pontos().get(0).parcial());
        assertEquals(new BigDecimal("30.00"), r.pontos().get(0).faturamento());
        assertEquals(new BigDecimal("15.00"), r.pontos().get(0).ticketMedio());
        assertEquals(new BigDecimal("30.00"), r.pontos().get(1).faturamento());
    }

    @Test
    void distribuicaoPorHoraEDiaDaSemana() {
        banco.add(pedido(HOJE.atTime(19, 10), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "50.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 50), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "30.00", "0", 20));
        banco.add(pedido(HOJE.atTime(12, 0), StatusPedido.CANCELADO, TipoAtendimento.ENTREGA, "30.00", "0", null));

        DistribuicaoVendasDTO r = service.distribuicao(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(24, r.porHora().size());
        assertEquals(2, r.porHora().get(19).pedidos());
        assertEquals(new BigDecimal("80.00"), r.porHora().get(19).faturamento());
        assertEquals(0, r.porHora().get(12).pedidos()); // cancelado não conta
        assertEquals("MONDAY", r.porDiaSemana().get(0).diaSemana());
        assertEquals(new BigDecimal("40.00"), r.porDiaSemana().get(0).ticketMedio());
    }

    @Test
    void vendasPorCategoriaUsaCategoriaDoItemOuDoCadastro() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "110.00", "0", 20,
                item("pz", 2, "45.00", "Pizza"),
                item("rf", 2, "10.00", null))); // pedido antigo sem categoria no item -> usa o cadastro

        VendasPorCategoriaDTO r = service.porCategoria(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(2, r.itens().size());
        assertEquals("Pizza", r.itens().get(0).categoria());
        assertEquals(new BigDecimal("90.00"), r.itens().get(0).faturamento());
        assertEquals(new BigDecimal("81.8"), r.itens().get(0).participacao());
        assertEquals("Bebida", r.itens().get(1).categoria());

        VendasPorCategoriaDTO soBebida = service.porCategoria(service.criarFiltro(HOJE, HOJE, null, null, "Bebida"));
        assertEquals(1, soBebida.itens().size());
        assertEquals(new BigDecimal("100.0"), soBebida.itens().get(0).participacao());
    }

    @Test
    void vendasPorTipoAtendimentoSempreTrazEntregaERetirada() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "60.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 5), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "40.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 9), StatusPedido.FINALIZADO, TipoAtendimento.RETIRADA, "20.00", "0", 20));

        VendasPorTipoAtendimentoDTO r = service.porTipoAtendimento(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(2, r.itens().size());
        assertEquals(TipoAtendimento.ENTREGA, r.itens().get(0).tipoAtendimento());
        assertEquals(new BigDecimal("50.00"), r.itens().get(0).ticketMedio());
        assertEquals(new BigDecimal("66.7"), r.itens().get(0).participacao());
        assertEquals(new BigDecimal("33.3"), r.itens().get(1).participacao());
    }

    // ------------------------------------------------------------------ RF03

    @Test
    void statusTrazTodosOsStatusComPercentual() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.RECEBIDO, TipoAtendimento.ENTREGA, "10.00", "0", null));
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.RECEBIDO, TipoAtendimento.ENTREGA, "10.00", "0", null));
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "10.00", "0", 20));
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.CANCELADO, TipoAtendimento.ENTREGA, "10.00", "0", null));

        StatusPedidosDTO r = service.statusPedidos(service.criarFiltro(HOJE, HOJE, null, null, null));

        assertEquals(StatusPedido.values().length, r.itens().size());
        assertEquals(4, r.total());
        assertEquals(2, r.emAndamento());
        assertEquals(1, r.finalizados());
        assertEquals(1, r.cancelados());
        StatusPedidosDTO.StatusDTO recebido = r.itens().get(0);
        assertEquals(StatusPedido.RECEBIDO, recebido.status());
        assertEquals("EM_ANDAMENTO", recebido.grupo());
        assertEquals(new BigDecimal("50.0"), recebido.percentual());
        assertEquals(0, r.itens().get(StatusPedido.PRONTO.ordinal()).quantidade());
    }

    @Test
    void pedidosRecentesOrdenadosEComAtraso() {
        Pedido antigo = pedido(AGORA.minusMinutes(55), StatusPedido.EM_PREPARACAO, TipoAtendimento.ENTREGA, "10.00", "0", null);
        antigo.setClienteId("c1");
        Pedido novo = pedido(AGORA.minusMinutes(5), StatusPedido.RECEBIDO, TipoAtendimento.ENTREGA, "20.00", "0", null);
        novo.setClienteId("c1");
        Pedido finalizado = pedido(AGORA.minusMinutes(120), StatusPedido.FINALIZADO, TipoAtendimento.RETIRADA, "30.00", "0", 20);
        finalizado.setClienteId("removido");
        banco.addAll(List.of(antigo, novo, finalizado));
        Cliente maria = new Cliente();
        maria.setId("c1");
        maria.setNome("Maria Silva");
        when(clienteRepository.findAllById(any())).thenReturn(List.of(maria));

        PedidosRecentesDTO r = service.pedidosRecentes(service.criarFiltro(HOJE, HOJE, null, null, null), 10);

        assertEquals(3, r.itens().size());
        assertEquals(novo.getId(), r.itens().get(0).id());
        assertEquals("Maria Silva", r.itens().get(0).cliente());
        assertEquals(5L, r.itens().get(0).minutosEmAberto());
        assertFalse(r.itens().get(0).atrasado());
        assertTrue(r.itens().get(1).atrasado());                 // 55 min em preparação
        assertNull(r.itens().get(2).minutosEmAberto());          // finalizado não fica "em aberto"
        assertEquals("Cliente não encontrado", r.itens().get(2).cliente());
    }

    @Test
    void limiteForaDoIntervaloEhRejeitado() {
        DashboardFiltro filtro = service.criarFiltro(HOJE, HOJE, null, null, null);
        assertThrows(RegraNegocioException.class, () -> service.pedidosRecentes(filtro, 0));
        assertThrows(RegraNegocioException.class, () -> service.maisVendidos(filtro, 51, CriterioRanking.QUANTIDADE));
    }

    // ------------------------------------------------------------------ RF04

    @Test
    void rankingPorQuantidadeEPorFaturamento() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "0", "0", 20,
                item("pz", 1, "45.00", "Pizza"), item("es", 6, "6.00", "Esfiha")));
        banco.add(pedido(HOJE.atTime(19, 30), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "0", "0", 20,
                item("pz", 1, "45.00", "Pizza"), item("es", 2, "6.00", "Esfiha")));
        // Cancelado não entra no ranking
        banco.add(pedido(HOJE.atTime(20, 0), StatusPedido.CANCELADO, TipoAtendimento.ENTREGA, "0", "0", null,
                item("pz", 10, "45.00", "Pizza")));

        DashboardFiltro filtro = service.criarFiltro(HOJE, HOJE, null, null, null);
        RankingProdutosDTO porQuantidade = service.maisVendidos(filtro, 10, CriterioRanking.QUANTIDADE);
        RankingProdutosDTO porFaturamento = service.maisVendidos(filtro, 10, CriterioRanking.FATURAMENTO);

        assertEquals(10, porQuantidade.totalItensVendidos());
        assertEquals("es", porQuantidade.itens().get(0).produtoId());
        assertEquals(8, porQuantidade.itens().get(0).quantidade());
        assertEquals(2, porQuantidade.itens().get(0).pedidos());
        assertEquals(new BigDecimal("80.0"), porQuantidade.itens().get(0).participacao());
        assertEquals(1, porQuantidade.itens().get(0).posicao());

        assertEquals("pz", porFaturamento.itens().get(0).produtoId());
        assertEquals(new BigDecimal("90.00"), porFaturamento.itens().get(0).faturamento());
        assertEquals(1, service.maisVendidos(filtro, 1, CriterioRanking.QUANTIDADE).itens().size());
    }

    @Test
    void rankingComFiltroDeCategoriaConsideraSoOsItensDaCategoria() {
        banco.add(pedido(HOJE.atTime(19, 0), StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA, "0", "0", 20,
                item("pz", 1, "45.00", "Pizza"), item("es", 6, "6.00", "Esfiha")));

        RankingProdutosDTO r = service.maisVendidos(service.criarFiltro(HOJE, HOJE, null, null, "Pizza"), 10, null);

        assertEquals(CriterioRanking.QUANTIDADE, r.criterio());
        assertEquals(1, r.itens().size());
        assertEquals("Pizza", r.itens().get(0).categoria());
    }

    // ------------------------------------------------------------------ auxiliares

    private int sequencia = 0;

    private Pedido pedido(LocalDateTime entrada, StatusPedido status, TipoAtendimento tipo, String total,
                          String desconto, Integer minutosAteSaida, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setId("p" + (++sequencia));
        p.setDataHoraEntrada(entrada);
        p.setStatus(status);
        p.setTipoAtendimento(tipo);
        p.setValorTotal(new BigDecimal(total));
        p.setValorDesconto(new BigDecimal(desconto));
        p.setItens(new ArrayList<>(List.of(itens)));
        if (minutosAteSaida != null && entrada != null) {
            p.setDataHoraSaida(entrada.plusMinutes(minutosAteSaida));
        }
        return p;
    }

    private ItemPedido item(String produtoId, int quantidade, String preco, String categoria) {
        BigDecimal unitario = new BigDecimal(preco);
        return new ItemPedido(produtoId, "Produto " + produtoId, unitario, quantidade,
                unitario.multiply(BigDecimal.valueOf(quantidade)), categoria);
    }

    private Produto produto(String id, String nome, String tipo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(10.0);
        p.setAtivo(true);
        return p;
    }
}
