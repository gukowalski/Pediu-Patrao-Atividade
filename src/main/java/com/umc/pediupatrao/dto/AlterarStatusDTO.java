package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import jakarta.validation.constraints.NotNull;

public class AlterarStatusDTO {

    @NotNull(message = "O novo status é obrigatório")
    private StatusPedido novoStatus;

    public AlterarStatusDTO() {

    }

    public AlterarStatusDTO(StatusPedido novoStatus) {
        this.novoStatus = novoStatus;
    }

    public StatusPedido getNovoStatus() {
        return novoStatus;
    }
}
