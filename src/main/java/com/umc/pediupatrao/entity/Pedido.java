package com.umc.pediupatrao.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    private String id;
    private String clienteId;
    private TipoAtendimento tipoAtendimento;
    private List<ItemPedido> itens = new ArrayList<>();
    private String observacao;

    // Valores calculados no servidor (PedidoService) — nunca aceitos da requisição
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal;            // Soma dos subtotais dos itens
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal descontoPercentual;  // 0 a 20 (aplicado pelo GERENTE)
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorDesconto;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorTotal;          // subtotal - valorDesconto

    private StatusPedido status;

    // Entrada do pedido (preenchidos pelo servidor)
    private String registradoPor;           // Username do usuário autenticado
    private LocalDateTime dataHoraEntrada;

    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;

    private String canceladoPor;
    private LocalDateTime dataHoraCancelamento;
    private String justificativaCancelamento;
}
