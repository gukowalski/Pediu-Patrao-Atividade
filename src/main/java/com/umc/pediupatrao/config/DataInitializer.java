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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final int DIAS_HISTORICO = 90;
    // Horas de funcionamento e o "peso" de cada uma (picos no almoço e no jantar)
    private static final int[] HORAS = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] PESO_HORAS = {2, 5, 4, 2, 1, 1, 1, 3, 7, 9, 7, 4, 2};

    @Value("${pediupatrao.seed.historico:true}")
    private boolean gerarHistorico;

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

            // Histórico para o dashboard: só é gerado se ainda não existir pedido anterior a ontem
            if (gerarHistorico && !pedidoRepo.existsByDataHoraEntradaBefore(LocalDate.now().minusDays(1).atStartOfDay())) {
                popularHistorico(produtoRepo, clienteRepo, pedidoRepo);
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
        produtoRepo.save(produto("Pizza Quatro Queijos", "Pizza", 52.90, true));
        produtoRepo.save(produto("Pizza Frango com Catupiry", "Pizza", 49.90, true));
        produtoRepo.save(produto("Esfiha de Queijo", "Esfiha", 6.00, true));
        produtoRepo.save(produto("Suco Natural 500ml", "Bebida", 9.50, true));
        produtoRepo.save(produto("X-Salada", "Lanche", 24.00, true));
        produtoRepo.save(produto("Brownie", "Sobremesa", 14.00, true));
        produtoRepo.save(produto("Pizza Especial (inativa)", "Pizza", 55.00, false));

        Cliente maria = clienteRepo.save(cliente("Maria Silva", "11988887777", "08780000",
                "Rua das Flores", "100", null, "Centro"));
        Cliente joao = clienteRepo.save(cliente("João Souza", "11977776666", "08710000",
                "Av. Voluntário Fernando Pinheiro Franco", "250", null, "Centro"));
        Cliente ana = clienteRepo.save(cliente("Ana Costa", "11966665555", "08730000",
                "Rua Ipiranga", "45", "Apto 12", "Mogilar"));
        Cliente carlos = clienteRepo.save(cliente("Carlos Pereira", "11955554444", "08750000",
                "Rua Navajas", "800", null, "Braz Cubas"));
        clienteRepo.save(cliente("Beatriz Lima", "11944443333", "08770000",
                "Rua Dr. Deodato Wertheimer", "1200", null, "Centro"));
        clienteRepo.save(cliente("Rafael Oliveira", "11933332222", "08740000",
                "Av. Japão", "310", "Casa 2", "Vila Oliveira"));

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

        System.out.println("Dados de demonstração criados: 13 produtos, 6 clientes e 6 pedidos");
    }

    /**
     * Gera pedidos finalizados/cancelados dos últimos 90 dias para alimentar o dashboard.
     * Usa semente fixa (resultado reproduzível), mais movimento no fim de semana e no jantar
     * e um leve crescimento ao longo do período, para que as tendências apareçam nos gráficos.
     */
    private void popularHistorico(ProdutoRepository produtoRepo, ClienteRepository clienteRepo,
                                  PedidoRepository pedidoRepo) {
        List<Produto> produtos = produtoRepo.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getAtivo()) && p.getPreco() != null && p.getPreco() > 0)
                .toList();
        List<Cliente> clientes = clienteRepo.findAll();
        if (produtos.isEmpty() || clientes.isEmpty()) {
            return;
        }

        Random random = new Random(2026);
        LocalDate hoje = LocalDate.now();
        List<Pedido> pedidos = new ArrayList<>();

        for (int d = DIAS_HISTORICO; d >= 1; d--) {
            LocalDate dia = hoje.minusDays(d);
            int base = switch (dia.getDayOfWeek()) {
                case FRIDAY, SATURDAY -> 15;
                case SUNDAY -> 12;
                case MONDAY -> 5;
                default -> 8;
            };
            double crescimento = 1 + 0.35 * (DIAS_HISTORICO - d) / DIAS_HISTORICO;
            int quantidade = Math.max(1, (int) Math.round(base * crescimento) + random.nextInt(5) - 2);

            for (int i = 0; i < quantidade; i++) {
                LocalDateTime entrada = dia.atTime(sortearHora(random), random.nextInt(60));
                pedidos.add(pedidoHistorico(random, entrada, produtos, clientes));
            }
        }

        pedidoRepo.saveAll(pedidos);
        System.out.println("Histórico do dashboard criado: " + pedidos.size() + " pedidos em " + DIAS_HISTORICO + " dias");
    }

    private Pedido pedidoHistorico(Random random, LocalDateTime entrada, List<Produto> produtos, List<Cliente> clientes) {
        TipoAtendimento tipo = random.nextInt(100) < 65 ? TipoAtendimento.ENTREGA : TipoAtendimento.RETIRADA;

        // Produtos do início da lista são os mais populares (peso decrescente)
        int quantidadeProdutos = 1 + random.nextInt(3);
        Set<Produto> escolhidos = new LinkedHashSet<>();
        while (escolhidos.size() < Math.min(quantidadeProdutos, produtos.size())) {
            escolhidos.add(produtos.get(sortearIndice(random, produtos.size())));
        }
        List<ItemPedido> itens = escolhidos.stream().map(p -> {
            boolean pequeno = "Esfiha".equals(p.getTipo()) || "Bebida".equals(p.getTipo());
            int qtd = pequeno ? 1 + random.nextInt(6) : 1 + random.nextInt(2);
            return item(p, qtd);
        }).toList();

        Pedido pedido = pedido(clientes.get(random.nextInt(clientes.size())), tipo, StatusPedido.FINALIZADO,
                entrada, itens.toArray(ItemPedido[]::new));
        pedido.setRegistradoPor(random.nextBoolean() ? "atendente01" : "gerente01");

        if (random.nextInt(100) < 10) {
            BigDecimal percentual = random.nextBoolean() ? new BigDecimal("5.00") : new BigDecimal("10.00");
            BigDecimal desconto = pedido.getSubtotal().multiply(percentual)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            pedido.setDescontoPercentual(percentual);
            pedido.setValorDesconto(desconto);
            pedido.setValorTotal(pedido.getSubtotal().subtract(desconto));
        }

        if (random.nextInt(100) < 7) {
            pedido.setStatus(StatusPedido.CANCELADO);
            pedido.setResponsavelSaida(null);
            pedido.setDataHoraSaida(null);
            pedido.setCanceladoPor("gerente01");
            pedido.setDataHoraCancelamento(entrada.plusMinutes(8));
            pedido.setJustificativaCancelamento("Cliente desistiu do pedido");
        } else {
            int minutos = tipo == TipoAtendimento.ENTREGA ? 25 + random.nextInt(26) : 15 + random.nextInt(16);
            pedido.setDataHoraSaida(entrada.plusMinutes(minutos));
        }
        return pedido;
    }

    private int sortearHora(Random random) {
        int total = 0;
        for (int peso : PESO_HORAS) {
            total += peso;
        }
        int sorteio = random.nextInt(total);
        for (int i = 0; i < HORAS.length; i++) {
            sorteio -= PESO_HORAS[i];
            if (sorteio < 0) {
                return HORAS[i];
            }
        }
        return HORAS[HORAS.length - 1];
    }

    private int sortearIndice(Random random, int tamanho) {
        int total = tamanho * (tamanho + 1) / 2;
        int sorteio = random.nextInt(total);
        for (int i = 0; i < tamanho; i++) {
            sorteio -= tamanho - i;
            if (sorteio < 0) {
                return i;
            }
        }
        return tamanho - 1;
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
                preco.multiply(BigDecimal.valueOf(quantidade)), produto.getTipo());
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
