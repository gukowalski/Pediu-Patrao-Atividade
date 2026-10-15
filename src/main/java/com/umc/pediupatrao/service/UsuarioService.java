package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.ConflictException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ResourceNotFoundException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public Usuario salvarUsuario(Usuario usuario) {
        if (usuario.getRole() == null) {
            throw new RegraNegocioException("O perfil é obrigatório");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new RegraNegocioException("A senha é obrigatória");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new ConflictException("Username já cadastrado");
        }

        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        return usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (usuario.getUsername().equals(usernameAutenticado())) {
            throw new RegraNegocioException("Você não pode excluir o próprio usuário");
        }
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (usuarioAtualizado.getRole() == null) {
            throw new RegraNegocioException("O perfil é obrigatório");
        }

        if (usuarioExistente.getUsername().equals(usernameAutenticado())
                && usuarioExistente.getRole() != usuarioAtualizado.getRole()) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil");
        }

        usuarioRepository.findByUsername(usuarioAtualizado.getUsername())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflictException("Username já cadastrado");
                });

        usuarioExistente.setUsername(usuarioAtualizado.getUsername());
        usuarioExistente.setRole(usuarioAtualizado.getRole());

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        return usuarioRepository.save(usuarioExistente);
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
