package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.InsertUsuarioDTO;
import com.umc.pediupatrao.dto.UsuarioDTO;
import com.umc.pediupatrao.exception.ResourceNotFoundException;
import com.umc.pediupatrao.mapper.UsuarioMapper;
import com.umc.pediupatrao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.StreamSupport;

@PreAuthorize("hasRole('ROLE_ADMIN')")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Endpoint para listar todos os usuários
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        List<UsuarioDTO> result = StreamSupport.stream(usuarioService.listarTodos().spliterator(), false)
                .map(UsuarioMapper::toDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    // Endpoint para buscar um usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable String id) {
        UsuarioDTO result = usuarioService.buscarPorId(id)
                .map(UsuarioMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return ResponseEntity.ok(result);
    }

    // Endpoint para criar um novo usuário
    @PostMapping
    public ResponseEntity<UsuarioDTO> criarUsuario(@Valid @RequestBody InsertUsuarioDTO dto) {
        UsuarioDTO result = UsuarioMapper.toDto(usuarioService.salvarUsuario(UsuarioMapper.toEntity(dto)));

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(result.getId()).toUri();
        return ResponseEntity.created(uri).body(result);
    }

    // Endpoint para atualizar um usuário
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable String id, @Valid @RequestBody InsertUsuarioDTO dto) {
        UsuarioDTO result = UsuarioMapper.toDto(usuarioService.atualizarUsuario(id, UsuarioMapper.toEntity(dto)));
        return ResponseEntity.ok(result);
    }

    // Endpoint para deletar um usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
