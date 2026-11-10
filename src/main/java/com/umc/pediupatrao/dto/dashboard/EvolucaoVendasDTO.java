package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** RF02 — série temporal de vendas agrupada por dia, semana ou mês (períodos sem venda vêm zerados). */
public record EvolucaoVendasDTO(PeriodoDTO periodo,
                                Agrupamento agrupamento,
                                List<PontoDTO> pontos,
                                LocalDateTime geradoEm) {

    /** @param parcial true quando a semana/mês não está inteiro dentro do período filtrado */
    public record PontoDTO(LocalDate inicio,
                           String rotulo,
                           boolean parcial,
                           long pedidos,
                           long cancelados,
                           BigDecimal faturamento,
                           BigDecimal ticketMedio) {
    }
}
