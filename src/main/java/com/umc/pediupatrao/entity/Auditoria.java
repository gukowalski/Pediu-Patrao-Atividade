package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "auditoria")
public class Auditoria {

    @Id
    private String id;
    private final LocalDateTime dataHora;
    private final String usuario;
    private final String perfil;
    private final TipoOperacao operacao;
    private final String entidade;
    private final String entidadeId;
    private final List<CampoAlterado> alteracoes;
    private final String justificativa;

    public Auditoria(LocalDateTime dataHora, String usuario, String perfil, TipoOperacao operacao,
                     String entidade, String entidadeId, List<CampoAlterado> alteracoes, String justificativa) {
        this.dataHora = dataHora;
        this.usuario = usuario;
        this.perfil = perfil;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes;
        this.justificativa = justificativa;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public TipoOperacao getOperacao() {
        return operacao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public List<CampoAlterado> getAlteracoes() {
        return alteracoes;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
