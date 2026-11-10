package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF03 — últimos pedidos do período, destacando os que estão em andamento há muito tempo. */
public record PedidosRecentesDTO(PeriodoDTO periodo,
                                 long limiteAtrasoMinutos,
                                 List<PedidoResumoDTO> itens,
                                 LocalDateTime geradoEm) {

    public record PedidoResumoDTO(String id, String cliente, TipoAtendimento tipoAtendimento,
                                  StatusPedido status, String statusDescricao, int quantidadeItens,
                                  BigDecimal valorTotal, LocalDateTime dataHoraEntrada,
                                  Long minutosEmAberto, boolean atrasado) {
    }
}
