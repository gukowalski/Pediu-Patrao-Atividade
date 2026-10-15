package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DescontoPedidoDTO {

    @NotNull(message = "O percentual é obrigatório")
    @DecimalMin(value = "0", inclusive = false, message = "O desconto deve ser maior que 0%")
    @DecimalMax(value = "20", message = "O desconto é limitado a 20%")
    private BigDecimal percentual;

    public DescontoPedidoDTO() {

    }

    public DescontoPedidoDTO(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }
}
