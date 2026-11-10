package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF02 — distribuição das vendas por hora do dia e por dia da semana (horários de pico). */
public record DistribuicaoVendasDTO(PeriodoDTO periodo,
                                    List<HoraDTO> porHora,
                                    List<DiaSemanaDTO> porDiaSemana,
                                    LocalDateTime geradoEm) {

    public record HoraDTO(int hora, String rotulo, long pedidos, BigDecimal faturamento) {
    }

    public record DiaSemanaDTO(String diaSemana, String rotulo, long pedidos, BigDecimal faturamento,
                               BigDecimal ticketMedio) {
    }
}
