package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.InsertPedidoDTO;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PreAuthorize("hasAnyRole('ROLE_GERENTE', 'ROLE_ATENDENTE')")
    @PostMapping
    public ResponseEntity<PedidoDTO> criarPedido(@Valid @RequestBody InsertPedidoDTO dto) {
        PedidoDTO result = pedidoService.criarPedido(dto);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(result.getId()).toUri();
        return ResponseEntity.created(uri).body(result);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_ATENDENTE')")
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarPedidos() {
        List<PedidoDTO> result = pedidoService.listarPedidos();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_ATENDENTE')")
    @GetMapping(value = "/{id}")
    public ResponseEntity<PedidoDTO> buscarPorId(@PathVariable String id) {
        PedidoDTO result = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, status);
    }
    
    
}
