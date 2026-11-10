package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.DashboardFiltro;
import com.umc.pediupatrao.dto.dashboard.DistribuicaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.EvolucaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.FiltrosDashboardDTO;
import com.umc.pediupatrao.dto.dashboard.PedidosRecentesDTO;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosDTO;
import com.umc.pediupatrao.dto.dashboard.ResumoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.StatusPedidosDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoriaDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorTipoAtendimentoDTO;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API do Dashboard Administrativo (somente leitura).
 * Acesso restrito a quem administra o estabelecimento: ADMIN e GERENTE.
 *
 * <p>Filtros comuns (RF05), todos opcionais: dataInicio e dataFim (yyyy-MM-dd, padrão: últimos 30 dias),
 * tipoAtendimento (ENTREGA | RETIRADA), status (StatusPedido) e categoria (tipo do produto).
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** RF05 — opções para montar os filtros da tela. */
    @GetMapping("/filtros")
    public ResponseEntity<FiltrosDashboardDTO> filtros() {
        return ResponseEntity.ok(dashboardService.opcoesDeFiltro());
    }

    /** RF01 — indicadores gerais de vendas. */
    @GetMapping("/resumo")
    public ResponseEntity<ResumoVendasDTO> resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.resumo(filtro));
    }

    /** RF02 — evolução das vendas no tempo. */
    @GetMapping("/vendas/evolucao")
    public ResponseEntity<EvolucaoVendasDTO> evolucao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "DIA") Agrupamento agrupamento) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.evolucao(filtro, agrupamento));
    }

    /** RF02 — vendas por hora do dia e por dia da semana. */
    @GetMapping("/vendas/distribuicao")
    public ResponseEntity<DistribuicaoVendasDTO> distribuicao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.distribuicao(filtro));
    }

    /** RF02 — vendas por categoria de produto. */
    @GetMapping("/vendas/por-categoria")
    public ResponseEntity<VendasPorCategoriaDTO> porCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.porCategoria(filtro));
    }

    /** RF02 — entrega x retirada. */
    @GetMapping("/vendas/por-tipo-atendimento")
    public ResponseEntity<VendasPorTipoAtendimentoDTO> porTipoAtendimento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.porTipoAtendimento(filtro));
    }

    /** RF03 — quantidade de pedidos por status. */
    @GetMapping("/pedidos/status")
    public ResponseEntity<StatusPedidosDTO> statusPedidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.statusPedidos(filtro));
    }

    /** RF03 — últimos pedidos, com destaque para os atrasados. */
    @GetMapping("/pedidos/recentes")
    public ResponseEntity<PedidosRecentesDTO> pedidosRecentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "10") int limite) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.pedidosRecentes(filtro, limite));
    }

    /** RF04 — ranking dos produtos mais vendidos. */
    @GetMapping("/produtos/mais-vendidos")
    public ResponseEntity<RankingProdutosDTO> maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "QUANTIDADE") CriterioRanking criterio) {
        DashboardFiltro filtro = dashboardService.criarFiltro(dataInicio, dataFim, tipoAtendimento, status, categoria);
        return ResponseEntity.ok(dashboardService.maisVendidos(filtro, limite, criterio));
    }
}
