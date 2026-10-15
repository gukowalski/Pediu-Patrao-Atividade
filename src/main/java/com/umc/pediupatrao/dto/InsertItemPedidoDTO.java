package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InsertItemPedidoDTO {

    @NotBlank(message = "O produto é obrigatório")
    private String produtoId;
    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima é 1")
    @Max(value = 50, message = "A quantidade máxima por item é 50")
    private Integer quantidade;

    public InsertItemPedidoDTO() {

    }

    public InsertItemPedidoDTO(String produtoId, Integer quantidade) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }
}
