package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AuditoriaDTO;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listar() {
        return ResponseEntity.ok(auditoriaService.listar());
    }
}
