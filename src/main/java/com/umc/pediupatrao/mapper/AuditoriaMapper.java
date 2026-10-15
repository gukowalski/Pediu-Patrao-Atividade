package com.umc.pediupatrao.mapper;

import com.umc.pediupatrao.dto.AuditoriaDTO;
import com.umc.pediupatrao.dto.CampoAlteradoDTO;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.CampoAlterado;

import java.util.List;

public class AuditoriaMapper {

    public static AuditoriaDTO toDto(Auditoria auditoria) {
        List<CampoAlteradoDTO> alteracoes = auditoria.getAlteracoes() == null
                ? List.of()
                : auditoria.getAlteracoes().stream().map(AuditoriaMapper::campoToDto).toList();

        return new AuditoriaDTO(
                auditoria.getId(),
                auditoria.getDataHora(),
                auditoria.getUsuario(),
                auditoria.getPerfil(),
                auditoria.getOperacao(),
                auditoria.getEntidade(),
                auditoria.getEntidadeId(),
                alteracoes,
                auditoria.getJustificativa());
    }

    public static CampoAlteradoDTO campoToDto(CampoAlterado campo) {
        if ("telefone".equals(campo.getCampo())) {
            return new CampoAlteradoDTO(campo.getCampo(),
                    mascararTelefone(campo.getValorAnterior()),
                    mascararTelefone(campo.getValorNovo()));
        }
        return new CampoAlteradoDTO(campo.getCampo(), campo.getValorAnterior(), campo.getValorNovo());
    }

    public static String mascararTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "****";
        }
        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }
}
