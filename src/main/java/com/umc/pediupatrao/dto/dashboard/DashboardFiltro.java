package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Filtros aceitos por todos os endpoints do dashboard (RF05).
 * As datas chegam normalizadas pelo DashboardService (padrão: últimos 30 dias).
 */
public record DashboardFiltro(LocalDate dataInicio,
                              LocalDate dataFim,
                              TipoAtendimento tipoAtendimento,
                              StatusPedido status,
                              String categoria) {

    public LocalDateTime inicio() {
        return dataInicio.atStartOfDay();
    }

    // Fim exclusivo: o dia final é considerado inteiro
    public LocalDateTime fimExclusivo() {
        return dataFim.plusDays(1).atStartOfDay();
    }

    public long dias() {
        return ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
    }

    // Período imediatamente anterior, com a mesma duração, usado no comparativo dos indicadores
    public DashboardFiltro periodoAnterior() {
        LocalDate fimAnterior = dataInicio.minusDays(1);
        return new DashboardFiltro(fimAnterior.minusDays(dias() - 1), fimAnterior, tipoAtendimento, status, categoria);
    }

    public PeriodoDTO periodo() {
        return new PeriodoDTO(dataInicio, dataFim, dias());
    }
}
