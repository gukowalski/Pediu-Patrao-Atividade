package com.umc.pediupatrao.entity;

public class CampoAlterado {

    private final String campo;
    private final String valorAnterior;
    private final String valorNovo;

    public CampoAlterado(String campo, String valorAnterior, String valorNovo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public static CampoAlterado de(String campo, Object valorAnterior, Object valorNovo) {
        return new CampoAlterado(campo,
                valorAnterior != null ? valorAnterior.toString() : null,
                valorNovo != null ? valorNovo.toString() : null);
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
