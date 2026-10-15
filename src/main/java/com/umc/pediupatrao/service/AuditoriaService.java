package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.AuditoriaDTO;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.mapper.AuditoriaMapper;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(TipoOperacao operacao, String entidade, String entidadeId,
                          List<CampoAlterado> alteracoes, String justificativa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String perfil = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .findFirst()
                .orElse(null);

        Auditoria auditoria = new Auditoria(LocalDateTime.now(), authentication.getName(), perfil,
                operacao, entidade, entidadeId, alteracoes, justificativa);
        auditoriaRepository.save(auditoria);
    }

    public List<AuditoriaDTO> listar() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc().stream()
                .map(AuditoriaMapper::toDto)
                .toList();
    }
}
