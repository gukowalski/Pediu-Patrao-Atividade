package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoOperacao;

import java.time.LocalDateTime;
import java.util.List;

public class AuditoriaDTO {
    private String id;
    private LocalDateTime dataHora;
    private String usuario;
    private String perfil;
    private TipoOperacao operacao;
    private String entidade;
    private String entidadeId;
    private List<CampoAlteradoDTO> alteracoes;
    private String justificativa;

    public AuditoriaDTO() {

    }

    public AuditoriaDTO(String id, LocalDateTime dataHora, String usuario, String perfil, TipoOperacao operacao,
                        String entidade, String entidadeId, List<CampoAlteradoDTO> alteracoes, String justificativa) {
        this.id = id;
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

    public List<CampoAlteradoDTO> getAlteracoes() {
        return alteracoes;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
