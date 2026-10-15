package com.umc.pediupatrao.dto;

public class CampoAlteradoDTO {
    private String campo;
    private String valorAnterior;
    private String valorNovo;

    public CampoAlteradoDTO() {

    }

    public CampoAlteradoDTO(String campo, String valorAnterior, String valorNovo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }
}
