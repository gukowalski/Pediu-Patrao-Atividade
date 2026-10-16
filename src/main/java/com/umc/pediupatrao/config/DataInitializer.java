package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Role;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder,
                                   ProdutoRepository produtoRepo, ClienteRepository clienteRepo,
                                   PedidoRepository pedidoRepo) {
        return args -> {
            criarUsuario(repo, encoder, "admin", Role.ADMIN);
            criarUsuario(repo, encoder, "gerente01", Role.GERENTE);
            criarUsuario(repo, encoder, "atendente01", Role.ATENDENTE);

            if (produtoRepo.count() == 0 && clienteRepo.count() == 0 && pedidoRepo.count() == 0) {
                popularDemonstracao(produtoRepo, clienteRepo, pedidoRepo);
            }
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

    private void popularDemonstracao(ProdutoRepository produtoRepo, ClienteRepository clienteRepo,
                                     PedidoRepository pedidoRepo) {
        Produto calabresa = produtoRepo.save(produto("Pizza Calabresa", "Pizza", 45.90, true));
        Produto margherita = produtoRepo.save(produto("Pizza Margherita", "Pizza", 42.50, true));
        Produto portuguesa = produtoRepo.save(produto("Pizza Portuguesa", "Pizza", 48.90, true));
        Produto esfiha = produtoRepo.save(produto("Esfiha de Carne", "Esfiha", 6.50, true));
        Produto refrigerante = produtoRepo.save(produto("Refrigerante 2L", "Bebida", 12.00, true));
        Produto xBurguer = produtoRepo.save(produto("X-Burguer", "Lanche", 22.00, true));
        produtoRepo.save(produto("Pizza Especial (inativa)", "Pizza", 55.00, false));

        Cliente maria = clienteRepo.save(cliente("Maria Silva", "11988887777", "08780000",
                "Rua das Flores", "100", null, "Centro"));
        Cliente joao = clienteRepo.save(cliente("João Souza", "11977776666", "08710000",
                "Av. Voluntário Fernando Pinheiro Franco", "250", null, "Centro"));
        Cliente ana = clienteRepo.save(cliente("Ana Costa", "11966665555", "08730000",
                "Rua Ipiranga", "45", "Apto 12", "Mogilar"));
        Cliente carlos = clienteRepo.save(cliente("Carlos Pereira", "11955554444", "08750000",
                "Rua Navajas", "800", null, "Braz Cubas"));

        LocalDateTime agora = LocalDateTime.now();
        pedidoRepo.save(pedido(maria, TipoAtendimento.ENTREGA, StatusPedido.RECEBIDO, agora.minusMinutes(5),
                item(calabresa, 2), item(refrigerante, 1)));
        pedidoRepo.save(pedido(joao, TipoAtendimento.ENTREGA, StatusPedido.RECEBIDO, agora.minusMinutes(4),
                item(margherita, 1), item(esfiha, 6)));
        pedidoRepo.save(pedido(ana, TipoAtendimento.ENTREGA, StatusPedido.EM_PREPARACAO, agora.minusMinutes(20),
                item(portuguesa, 1)));
        pedidoRepo.save(pedido(carlos, TipoAtendimento.RETIRADA, StatusPedido.PRONTO, agora.minusMinutes(30),
                item(xBurguer, 2), item(refrigerante, 1)));
        pedidoRepo.save(pedido(maria, TipoAtendimento.ENTREGA, StatusPedido.SAIU_PARA_ENTREGA, agora.minusMinutes(45),
                item(calabresa, 1), item(esfiha, 4)));
        pedidoRepo.save(pedido(joao, TipoAtendimento.RETIRADA, StatusPedido.FINALIZADO, agora.minusMinutes(90),
                item(margherita, 2)));

        System.out.println("Dados de demonstração criados: 7 produtos, 4 clientes e 6 pedidos");
    }

    private Produto produto(String nome, String tipo, double preco, boolean ativo) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }

    private Cliente cliente(String nome, String telefone, String cep, String logradouro,
                            String numero, String complemento, String bairro) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setCep(cep);
        cliente.setLogradouro(logradouro);
        cliente.setNumero(numero);
        cliente.setComplemento(complemento);
        cliente.setBairro(bairro);
        cliente.setCidade("Mogi das Cruzes");
        cliente.setEstado("SP");
        return cliente;
    }

    private ItemPedido item(Produto produto, int quantidade) {
        BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_EVEN);
        return new ItemPedido(produto.getId(), produto.getNome(), preco, quantidade,
                preco.multiply(BigDecimal.valueOf(quantidade)));
    }

    private Pedido pedido(Cliente cliente, TipoAtendimento tipo, StatusPedido status,
                          LocalDateTime entrada, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setTipoAtendimento(tipo);
        pedido.setItens(new ArrayList<>(List.of(itens)));

        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        pedido.setValorDesconto(BigDecimal.ZERO.setScale(2));
        pedido.setValorTotal(subtotal);

        pedido.setStatus(status);
        pedido.setRegistradoPor("atendente01");
        pedido.setDataHoraEntrada(entrada);

        if (status == StatusPedido.SAIU_PARA_ENTREGA || status == StatusPedido.RETIRADO
                || status == StatusPedido.FINALIZADO) {
            pedido.setResponsavelSaida("atendente01");
            pedido.setDataHoraSaida(entrada.plusMinutes(25));
        }
        return pedido;
    }
}
