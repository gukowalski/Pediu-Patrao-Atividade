package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Role;

public class UsuarioDTO {
    private String id;
    private String username;
    private Role role;

    public UsuarioDTO() {

    }

    public UsuarioDTO(String id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
}
