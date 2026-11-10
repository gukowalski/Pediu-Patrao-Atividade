package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF02 — vendas agrupadas pela categoria do produto (Pizza, Bebida...). Valores dos itens, antes do desconto. */
public record VendasPorCategoriaDTO(PeriodoDTO periodo,
                                    BigDecimal faturamentoItens,
                                    List<CategoriaDTO> itens,
                                    LocalDateTime geradoEm) {

    public record CategoriaDTO(String categoria, long quantidade, BigDecimal faturamento,
                               BigDecimal participacao) {
    }
}
