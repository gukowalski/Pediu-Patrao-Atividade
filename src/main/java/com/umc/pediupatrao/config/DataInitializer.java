package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    // Cria um usuário de cada perfil caso ainda não existam no banco
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarUsuario(repo, encoder, "admin", Role.ADMIN);
            criarUsuario(repo, encoder, "gerente01", Role.GERENTE);
            criarUsuario(repo, encoder, "atendente01", Role.ATENDENTE);
        };
    }

    private void criarUsuario(UsuarioRepository repo, PasswordEncoder encoder, String username, Role role) {
        if (repo.findByUsername(username).isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setPassword(encoder.encode("teste123"));
            user.setRole(role);

            repo.save(user);

            System.out.println("Usuário " + username + " (" + role + ") criado!");
        }
    }
}
