package com.umc.pediupatrao.entity;

public enum Role {

    ADMIN("Administrador"),
    GERENTE("Gerente"),
    ATENDENTE("Atendente");

    private final String descricao;

    Role(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
