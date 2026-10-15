package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InsertUsuarioDTO {

    @NotBlank(message = "O username é obrigatório")
    @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres")
    private String username;
    @Size(max = 100, message = "A senha deve ter no máximo 100 caracteres")
    private String password;
    @NotNull(message = "O perfil é obrigatório (ADMIN, GERENTE ou ATENDENTE)")
    private Role role;

    public InsertUsuarioDTO() {

    }

    public InsertUsuarioDTO(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
