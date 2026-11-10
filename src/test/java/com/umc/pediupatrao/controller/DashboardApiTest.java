package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.DashboardFiltro;
import com.umc.pediupatrao.dto.dashboard.PeriodoDTO;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosDTO;
import com.umc.pediupatrao.dto.dashboard.ResumoVendasDTO;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes da camada HTTP da API do dashboard: autorização, parâmetros e formato das respostas. */
@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
class DashboardApiTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 11, 9);
    private static final DashboardFiltro FILTRO = new DashboardFiltro(INICIO, FIM, null, null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @BeforeEach
    void setUp() {
        when(dashboardService.criarFiltro(any(), any(), any(), any(), any())).thenReturn(FILTRO);
    }

    @Test
    void semLoginRetorna401() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAcessaODashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para o seu perfil"));
        mockMvc.perform(get("/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteConsultaResumo() throws Exception {
        when(dashboardService.resumo(FILTRO)).thenReturn(resumo());

        mockMvc.perform(get("/api/dashboard/resumo")
                        .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-09")
                        .param("tipoAtendimento", "ENTREGA").param("status", "FINALIZADO").param("categoria", "Pizza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(1250.50))
                .andExpect(jsonPath("$.ticketMedio").value(62.53))
                .andExpect(jsonPath("$.periodo.inicio").value("2026-11-01"))
                .andExpect(jsonPath("$.comparativo.variacaoFaturamento").value(12.5))
                .andExpect(jsonPath("$.geradoEm").value("2026-11-09T21:00:00"));

        verify(dashboardService).criarFiltro(INICIO, FIM, TipoAtendimento.ENTREGA, StatusPedido.FINALIZADO, "Pizza");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsultaRankingComParametrosPadrao() throws Exception {
        when(dashboardService.maisVendidos(FILTRO, 10, CriterioRanking.QUANTIDADE)).thenReturn(
                new RankingProdutosDTO(FILTRO.periodo(), CriterioRanking.QUANTIDADE, 8, List.of(
                        new RankingProdutosDTO.ProdutoRankingDTO(1, "p1", "Pizza Calabresa", "Pizza", 8, 5,
                                new BigDecimal("367.20"), new BigDecimal("100.0"))),
                        LocalDateTime.of(2026, 11, 9, 21, 0)));

        mockMvc.perform(get("/api/dashboard/produtos/mais-vendidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criterio").value("QUANTIDADE"))
                .andExpect(jsonPath("$.itens[0].nome").value("Pizza Calabresa"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(8));

        verify(dashboardService).criarFiltro(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dataEmFormatoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").param("dataInicio", "01/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("dataInicio")))
                .andExpect(jsonPath("$.message").value(containsString("yyyy-MM-dd")));
        verify(dashboardService, never()).resumo(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void enumInvalidoRetorna400ComValoresAceitos() throws Exception {
        mockMvc.perform(get("/api/dashboard/pedidos/status").param("status", "ENTREGUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Valores aceitos")));
        mockMvc.perform(get("/api/dashboard/vendas/evolucao").param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("[DIA, SEMANA, MES]")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void periodoInvalidoRetorna400() throws Exception {
        when(dashboardService.criarFiltro(eq(FIM), eq(INICIO), any(), any(), any()))
                .thenThrow(new RegraNegocioException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get("/api/dashboard/resumo").param("dataInicio", "2026-11-09").param("dataFim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("A data inicial não pode ser posterior à data final"))
                .andExpect(jsonPath("$.path").value("/api/dashboard/resumo"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void limiteInvalidoRetorna400() throws Exception {
        when(dashboardService.pedidosRecentes(any(), anyInt()))
                .thenThrow(new RegraNegocioException("O limite deve estar entre 1 e 50"));

        mockMvc.perform(get("/api/dashboard/pedidos/recentes").param("limite", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O limite deve estar entre 1 e 50"));
        mockMvc.perform(get("/api/dashboard/pedidos/recentes").param("limite", "dez"))
                .andExpect(status().isBadRequest());
    }

    private ResumoVendasDTO resumo() {
        PeriodoDTO periodo = FILTRO.periodo();
        return new ResumoVendasDTO(periodo, new BigDecimal("1250.50"), 22, 20, new BigDecimal("62.53"), 41,
                new BigDecimal("15.00"), 2, new BigDecimal("9.1"), new BigDecimal("80.00"), 3,
                new BigDecimal("31.5"),
                new ResumoVendasDTO.ComparativoDTO(FILTRO.periodoAnterior().periodo(), true, new BigDecimal("1111.56"),
                        new BigDecimal("12.5"), 18, new BigDecimal("11.1"), new BigDecimal("61.75"),
                        new BigDecimal("1.3"), new BigDecimal("5.0")),
                LocalDateTime.of(2026, 11, 9, 21, 0));
    }
}
