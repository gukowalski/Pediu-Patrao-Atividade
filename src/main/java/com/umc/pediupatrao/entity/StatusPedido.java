package com.umc.pediupatrao.entity;

import java.util.List;

public enum StatusPedido {

    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<StatusPedido> proximosPermitidos(TipoAtendimento tipoAtendimento) {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> tipoAtendimento == TipoAtendimento.RETIRADA
                    ? List.of(RETIRADO)
                    : List.of(SAIU_PARA_ENTREGA);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public boolean podeAvancarPara(StatusPedido novoStatus, TipoAtendimento tipoAtendimento) {
        return proximosPermitidos(tipoAtendimento).contains(novoStatus);
    }

    public boolean isSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    public boolean isAntesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
