package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** RF04 — produtos mais vendidos no período. */
public record RankingProdutosDTO(PeriodoDTO periodo,
                                 CriterioRanking criterio,
                                 long totalItensVendidos,
                                 List<ProdutoRankingDTO> itens,
                                 LocalDateTime geradoEm) {

    public record ProdutoRankingDTO(int posicao, String produtoId, String nome, String categoria,
                                    long quantidade, long pedidos, BigDecimal faturamento,
                                    BigDecimal participacao) {
    }
}
