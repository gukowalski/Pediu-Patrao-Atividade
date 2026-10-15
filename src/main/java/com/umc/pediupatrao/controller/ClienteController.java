package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PreAuthorize("hasAnyRole('ROLE_GERENTE', 'ROLE_ATENDENTE')")
    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
    }

    @PreAuthorize("hasRole('ROLE_GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCliente(@PathVariable String id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ROLE_ATENDENTE')")
    @PostMapping
    public ResponseEntity<Cliente> salvarCliente(@RequestBody Cliente cliente) {
        Cliente salvo = clienteService.salvar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PreAuthorize("hasRole('ROLE_ATENDENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizarCliente(@PathVariable String id, @RequestBody Cliente cliente) {
        cliente.setId(id);
        Cliente atualizado = clienteService.salvar(cliente);
        return ResponseEntity.ok(atualizado);
    }
}
