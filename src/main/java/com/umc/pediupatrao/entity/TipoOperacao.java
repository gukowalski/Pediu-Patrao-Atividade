package com.umc.pediupatrao.entity;

public enum TipoOperacao {

    CRIACAO_PEDIDO("Criação de pedido"),
    ALTERACAO_STATUS_PEDIDO("Alteração de status do pedido"),
    DESCONTO_PEDIDO("Desconto no pedido"),
    CANCELAMENTO_PEDIDO("Cancelamento de pedido"),
    CRIACAO_CLIENTE("Criação de cliente"),
    ALTERACAO_CLIENTE("Alteração de cliente"),
    EXCLUSAO_CLIENTE("Exclusão de cliente");

    private final String descricao;

    TipoOperacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
