package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF03 — quantidade de pedidos em cada status (todos os status aparecem, mesmo zerados). */
public record StatusPedidosDTO(PeriodoDTO periodo,
                               long total,
                               long emAndamento,
                               long finalizados,
                               long cancelados,
                               List<StatusDTO> itens,
                               LocalDateTime geradoEm) {

    public record StatusDTO(StatusPedido status, String descricao, String grupo, long quantidade,
                            BigDecimal percentual, BigDecimal valorTotal) {
    }
}
