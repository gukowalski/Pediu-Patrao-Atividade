package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.ResourceNotFoundException;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class ClienteService {

    private static final String ENTIDADE = "Cliente";

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        Cliente anterior = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        clienteRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, ENTIDADE, id, compararCampos(anterior, null), null);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);
            auditoriaService.registrar(TipoOperacao.CRIACAO_CLIENTE, ENTIDADE, salvo.getId(),
                    compararCampos(null, salvo), null);
            return salvo;
        }

        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para atualização."));
        List<CampoAlterado> alteracoes = compararCampos(anterior, cliente);

        Cliente salvo = clienteRepository.save(cliente);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, ENTIDADE, salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    public List<CampoAlterado> compararCampos(Cliente anterior, Cliente novo) {
        List<CampoAlterado> alteracoes = new ArrayList<>();
        adicionarSeAlterado(alteracoes, "nome", valor(anterior, Cliente::getNome), valor(novo, Cliente::getNome));
        adicionarSeAlterado(alteracoes, "telefone", valor(anterior, Cliente::getTelefone), valor(novo, Cliente::getTelefone));
        adicionarSeAlterado(alteracoes, "cep", valor(anterior, Cliente::getCep), valor(novo, Cliente::getCep));
        adicionarSeAlterado(alteracoes, "logradouro", valor(anterior, Cliente::getLogradouro), valor(novo, Cliente::getLogradouro));
        adicionarSeAlterado(alteracoes, "numero", valor(anterior, Cliente::getNumero), valor(novo, Cliente::getNumero));
        adicionarSeAlterado(alteracoes, "complemento", valor(anterior, Cliente::getComplemento), valor(novo, Cliente::getComplemento));
        adicionarSeAlterado(alteracoes, "bairro", valor(anterior, Cliente::getBairro), valor(novo, Cliente::getBairro));
        adicionarSeAlterado(alteracoes, "cidade", valor(anterior, Cliente::getCidade), valor(novo, Cliente::getCidade));
        adicionarSeAlterado(alteracoes, "estado", valor(anterior, Cliente::getEstado), valor(novo, Cliente::getEstado));
        return alteracoes;
    }

    private void adicionarSeAlterado(List<CampoAlterado> alteracoes, String campo, String anterior, String novo) {
        if (!Objects.equals(anterior, novo)) {
            alteracoes.add(new CampoAlterado(campo, anterior, novo));
        }
    }

    private String valor(Cliente cliente, Function<Cliente, String> getter) {
        if (cliente == null) {
            return null;
        }
        String valor = getter.apply(cliente);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

}
