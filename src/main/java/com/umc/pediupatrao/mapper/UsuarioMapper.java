package com.umc.pediupatrao.mapper;

import com.umc.pediupatrao.dto.InsertUsuarioDTO;
import com.umc.pediupatrao.dto.UsuarioDTO;
import com.umc.pediupatrao.entity.Usuario;

public class UsuarioMapper {

    public static Usuario toEntity(InsertUsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        usuario.setRole(dto.getRole());
        return usuario;
    }

    public static UsuarioDTO toDto(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getRole());
    }
}
