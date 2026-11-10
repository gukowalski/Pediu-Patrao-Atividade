package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.TipoAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF02 — comparação entre pedidos de entrega e de retirada. */
public record VendasPorTipoAtendimentoDTO(PeriodoDTO periodo,
                                          List<TipoDTO> itens,
                                          LocalDateTime geradoEm) {

    public record TipoDTO(TipoAtendimento tipoAtendimento, String descricao, long pedidos,
                          BigDecimal faturamento, BigDecimal ticketMedio, BigDecimal participacao) {
    }
}
