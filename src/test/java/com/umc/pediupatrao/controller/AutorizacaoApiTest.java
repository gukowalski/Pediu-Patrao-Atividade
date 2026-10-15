package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PedidoController.class, UsuarioController.class, AuditoriaController.class})
@Import(SecurityConfig.class)
class AutorizacaoApiTest {

    private static final String CANCELAMENTO = "{\"justificativa\": \"Cliente desistiu do pedido\"}";
    private static final String DESCONTO = "{\"percentual\": 10}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private AuditoriaService auditoriaService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void apiSemLoginRetorna401() throws Exception {
        mockMvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeCancelarPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/cancelar").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeAplicarDesconto() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/desconto").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeCancelarPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/cancelar").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeCancelarPedido() throws Exception {
        when(pedidoService.cancelar(eq("1"), any())).thenReturn(new PedidoDTO());

        mockMvc.perform(post("/api/pedidos/1/cancelar").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void descontoAcimaDoLimiteRetorna422() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/desconto").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\": 25}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void requisicaoSemTokenCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/cancelar")
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeCriarUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"novo\", \"password\": \"123456\", \"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listagemDeUsuariosNaoExpoeSenha() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setUsername("gerente01");
        usuario.setPassword("$2a$10$hash");
        usuario.setRole(Role.GERENTE);
        when(usuarioService.listarTodos()).thenReturn(List.of(usuario));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("gerente01"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeConsultarAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void auditoriaNaoPodeSerExcluida() throws Exception {
        mockMvc.perform(delete("/api/auditoria").with(csrf())).andExpect(status().isMethodNotAllowed());
    }
}
