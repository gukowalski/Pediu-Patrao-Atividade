package com.umc.pediupatrao.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

// Embutido no documento do pedido (não tem coleção própria)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;       // Cópia do nome no momento do pedido
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal precoUnitario; // Cópia do preço do Produto — nunca vem da requisição
    private int quantidade;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal;      // precoUnitario x quantidade
    private String categoria;         // Cópia do tipo do Produto (Pizza, Bebida...) usada no dashboard

    public ItemPedido(String produtoId, String nomeProduto, BigDecimal precoUnitario, int quantidade,
                      BigDecimal subtotal) {
        this(produtoId, nomeProduto, precoUnitario, quantidade, subtotal, null);
    }
}
