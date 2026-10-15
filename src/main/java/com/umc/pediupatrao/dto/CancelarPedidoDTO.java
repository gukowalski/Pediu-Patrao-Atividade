package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelarPedidoDTO {

    @NotBlank(message = "A justificativa é obrigatória")
    @Size(min = 10, max = 500, message = "A justificativa deve ter entre 10 e 500 caracteres")
    private String justificativa;

    public CancelarPedidoDTO() {

    }

    public CancelarPedidoDTO(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
