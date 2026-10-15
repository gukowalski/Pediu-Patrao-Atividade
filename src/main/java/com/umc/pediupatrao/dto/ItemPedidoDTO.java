package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public class ItemPedidoDTO {
    private String produtoId;
    private String nomeProduto;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal subtotal;

    public ItemPedidoDTO() {

    }

    public ItemPedidoDTO(String produtoId, String nomeProduto, BigDecimal precoUnitario,
                         Integer quantidade, BigDecimal subtotal) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.subtotal = subtotal;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
