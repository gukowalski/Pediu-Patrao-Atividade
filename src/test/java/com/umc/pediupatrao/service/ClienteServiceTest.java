package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void comparaSomenteOsCamposAlterados() {
        Cliente anterior = cliente("Rua das Flores", "11999994321");
        Cliente novo = cliente("Av. Brasil", "11999994321");

        List<CampoAlterado> alteracoes = clienteService.compararCampos(anterior, novo);

        assertEquals(1, alteracoes.size());
        assertEquals("logradouro", alteracoes.get(0).getCampo());
        assertEquals("Rua das Flores", alteracoes.get(0).getValorAnterior());
        assertEquals("Av. Brasil", alteracoes.get(0).getValorNovo());
    }

    @Test
    void alteracaoDeEnderecoGeraAuditoria() {
        Cliente anterior = cliente("Rua das Flores", "11999994321");
        Cliente novo = cliente("Av. Brasil", "11988887777");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.salvar(novo);

        verify(auditoriaService).registrar(eq(TipoOperacao.ALTERACAO_CLIENTE), eq("Cliente"), eq("c1"), anyList(), isNull());
    }

    @Test
    void alteracaoSemMudancasNaoGeraAuditoria() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente("Rua das Flores", "11999994321")));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.salvar(cliente("Rua das Flores", "11999994321"));

        verify(auditoriaService, never()).registrar(any(), any(), any(), anyList(), any());
    }

    private Cliente cliente(String logradouro, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("João da Silva");
        cliente.setTelefone(telefone);
        cliente.setLogradouro(logradouro);
        cliente.setNumero("100");
        cliente.setCidade("Mogi das Cruzes");
        cliente.setEstado("SP");
        return cliente;
    }
}
