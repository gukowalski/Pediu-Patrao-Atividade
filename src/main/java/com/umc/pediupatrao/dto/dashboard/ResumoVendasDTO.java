package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RF01 — indicadores gerais de vendas do período, com comparativo do período anterior. */
public record ResumoVendasDTO(PeriodoDTO periodo,
                              BigDecimal faturamento,
                              long totalPedidos,
                              long pedidosValidos,
                              BigDecimal ticketMedio,
                              long itensVendidos,
                              BigDecimal totalDescontos,
                              long pedidosCancelados,
                              BigDecimal taxaCancelamento,
                              BigDecimal valorCancelado,
                              long pedidosEmAndamento,
                              BigDecimal tempoMedioAtendimentoMinutos,
                              ComparativoDTO comparativo,
                              LocalDateTime geradoEm) {

    /**
     * @param baseCompleta false quando o período anterior começa antes do primeiro pedido registrado;
     *                     nesse caso as variações vêm nulas para não sugerir crescimentos irreais
     */
    public record ComparativoDTO(PeriodoDTO periodoAnterior,
                                 boolean baseCompleta,
                                 BigDecimal faturamentoAnterior,
                                 BigDecimal variacaoFaturamento,
                                 long pedidosValidosAnterior,
                                 BigDecimal variacaoPedidos,
                                 BigDecimal ticketMedioAnterior,
                                 BigDecimal variacaoTicketMedio,
                                 BigDecimal taxaCancelamentoAnterior) {
    }
}
