package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.DashboardFiltro;
import com.umc.pediupatrao.dto.dashboard.DistribuicaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.EvolucaoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.FiltrosDashboardDTO;
import com.umc.pediupatrao.dto.dashboard.PedidosRecentesDTO;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosDTO;
import com.umc.pediupatrao.dto.dashboard.ResumoVendasDTO;
import com.umc.pediupatrao.dto.dashboard.StatusPedidosDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoriaDTO;
import com.umc.pediupatrao.dto.dashboard.VendasPorTipoAtendimentoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.DashboardRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras de negócio do Dashboard Administrativo.
 *
 * <p>Convenções adotadas em todos os indicadores:
 * <ul>
 *   <li>Faturamento = soma do valorTotal (já com desconto) dos pedidos não cancelados;</li>
 *   <li>Pedidos cancelados entram apenas nas contagens de cancelamento, nunca no faturamento;</li>
 *   <li>Pedidos sem data de entrada ou sem valores (dados antigos/incompletos) são tratados como zero
 *       e não derrubam o cálculo;</li>
 *   <li>Valores monetários com 2 casas e percentuais com 1 casa (HALF_EVEN).</li>
 * </ul>
 */
@Service
public class DashboardService {

    public static final int DIAS_PERIODO_PADRAO = 30;
    public static final int INTERVALO_MAXIMO_DIAS = 366;
    public static final int LIMITE_MAXIMO = 50;
    public static final long MINUTOS_ATRASO = 40;
    public static final String SEM_CATEGORIA = "Sem categoria";

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMM/yy", PT_BR);
    private static final String[] DIAS_SEMANA = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    private final DashboardRepository dashboardRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final Clock clock;

    public DashboardService(DashboardRepository dashboardRepository, ProdutoRepository produtoRepository,
                            ClienteRepository clienteRepository, Clock clock) {
        this.dashboardRepository = dashboardRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.clock = clock;
    }

    // =====================================================================
    // RF05 — Filtros
    // =====================================================================

    /** Aplica os valores padrão e valida o período informado. */
    public DashboardFiltro criarFiltro(LocalDate dataInicio, LocalDate dataFim, TipoAtendimento tipoAtendimento,
                                       StatusPedido status, String categoria) {
        LocalDate fim = dataFim != null ? dataFim : hoje();
        LocalDate inicio = dataInicio != null ? dataInicio : fim.minusDays(DIAS_PERIODO_PADRAO - 1L);

        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final");
        }
        DashboardFiltro filtro = new DashboardFiltro(inicio, fim, tipoAtendimento, status,
                categoria == null || categoria.isBlank() ? null : categoria.trim());
        if (filtro.dias() > INTERVALO_MAXIMO_DIAS) {
            throw new RegraNegocioException("O período consultado não pode ultrapassar " + INTERVALO_MAXIMO_DIAS + " dias");
        }
        return filtro;
    }

    public FiltrosDashboardDTO opcoesDeFiltro() {
        DashboardFiltro padrao = criarFiltro(null, null, null, null, null);
        List<FiltrosDashboardDTO.OpcaoDTO> status = Arrays.stream(StatusPedido.values())
                .map(s -> new FiltrosDashboardDTO.OpcaoDTO(s.name(), s.getDescricao())).toList();
        List<FiltrosDashboardDTO.OpcaoDTO> tipos = Arrays.stream(TipoAtendimento.values())
                .map(t -> new FiltrosDashboardDTO.OpcaoDTO(t.name(), t.getDescricao())).toList();
        Set<String> categorias = new TreeSet<>();
        produtoRepository.findAll().stream()
                .map(Produto::getTipo)
                .filter(t -> t != null && !t.isBlank())
                .forEach(categorias::add);

        return new FiltrosDashboardDTO(padrao.periodo(),
                dashboardRepository.dataPrimeiroPedido().map(LocalDateTime::toLocalDate).orElse(null),
                dashboardRepository.dataUltimoPedido().map(LocalDateTime::toLocalDate).orElse(null),
                INTERVALO_MAXIMO_DIAS, status, tipos, List.copyOf(categorias),
                List.of(Agrupamento.values()), List.of(CriterioRanking.values()));
    }

    // =====================================================================
    // RF01 — Indicadores gerais
    // =====================================================================

    public ResumoVendasDTO resumo(DashboardFiltro filtro) {
        Indicadores atual = Indicadores.de(buscar(filtro));
        DashboardFiltro filtroAnterior = filtro.periodoAnterior();
        Indicadores anterior = Indicadores.de(buscar(filtroAnterior));

        // Se o histórico começa no meio do período anterior, a comparação seria distorcida (ex.: +6.000%)
        boolean baseCompleta = dashboardRepository.dataPrimeiroPedido()
                .map(primeiro -> !primeiro.toLocalDate().isAfter(filtroAnterior.dataInicio()))
                .orElse(false);

        ResumoVendasDTO.ComparativoDTO comparativo = new ResumoVendasDTO.ComparativoDTO(
                filtroAnterior.periodo(),
                baseCompleta,
                anterior.faturamento,
                baseCompleta ? variacao(atual.faturamento, anterior.faturamento) : null,
                anterior.pedidosValidos,
                baseCompleta ? variacao(BigDecimal.valueOf(atual.pedidosValidos),
                        BigDecimal.valueOf(anterior.pedidosValidos)) : null,
                anterior.ticketMedio(),
                baseCompleta ? variacao(atual.ticketMedio(), anterior.ticketMedio()) : null,
                anterior.taxaCancelamento());

        return new ResumoVendasDTO(filtro.periodo(), atual.faturamento, atual.totalPedidos, atual.pedidosValidos,
                atual.ticketMedio(), atual.itensVendidos, atual.descontos, atual.cancelados,
                atual.taxaCancelamento(), atual.valorCancelado, atual.emAndamento,
                atual.tempoMedioAtendimento(), comparativo, agora());
    }

    // =====================================================================
    // RF02 — Análise de vendas
    // =====================================================================

    public EvolucaoVendasDTO evolucao(DashboardFiltro filtro, Agrupamento agrupamento) {
        Agrupamento agrup = agrupamento != null ? agrupamento : Agrupamento.DIA;

        // Cria todos os períodos do intervalo, inclusive os sem venda, para o gráfico não "pular" datas
        Map<LocalDate, List<Pedido>> porPeriodo = new LinkedHashMap<>();
        LocalDate chaveFinal = chave(filtro.dataFim(), agrup);
        for (LocalDate c = chave(filtro.dataInicio(), agrup); !c.isAfter(chaveFinal); c = proxima(c, agrup)) {
            porPeriodo.put(c, new ArrayList<>());
        }
        for (Pedido p : buscar(filtro)) {
            porPeriodo.computeIfAbsent(chave(p.getDataHoraEntrada().toLocalDate(), agrup), k -> new ArrayList<>()).add(p);
        }

        List<EvolucaoVendasDTO.PontoDTO> pontos = porPeriodo.entrySet().stream().map(e -> {
            Indicadores ind = Indicadores.de(e.getValue());
            boolean parcial = e.getKey().isBefore(filtro.dataInicio())
                    || proxima(e.getKey(), agrup).minusDays(1).isAfter(filtro.dataFim());
            return new EvolucaoVendasDTO.PontoDTO(e.getKey(), rotulo(e.getKey(), agrup), parcial, ind.pedidosValidos,
                    ind.cancelados, ind.faturamento, ind.ticketMedio());
        }).toList();

        return new EvolucaoVendasDTO(filtro.periodo(), agrup, pontos, agora());
    }

    public DistribuicaoVendasDTO distribuicao(DashboardFiltro filtro) {
        List<Pedido> validos = validos(buscar(filtro));

        Map<Integer, List<Pedido>> porHora = validos.stream()
                .collect(Collectors.groupingBy(p -> p.getDataHoraEntrada().getHour()));
        List<DistribuicaoVendasDTO.HoraDTO> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            List<Pedido> lista = porHora.getOrDefault(h, List.of());
            horas.add(new DistribuicaoVendasDTO.HoraDTO(h, String.format("%02dh", h), lista.size(), somaTotal(lista)));
        }

        Map<DayOfWeek, List<Pedido>> porDia = validos.stream()
                .collect(Collectors.groupingBy(p -> p.getDataHoraEntrada().getDayOfWeek()));
        List<DistribuicaoVendasDTO.DiaSemanaDTO> dias = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            List<Pedido> lista = porDia.getOrDefault(dia, List.of());
            BigDecimal total = somaTotal(lista);
            dias.add(new DistribuicaoVendasDTO.DiaSemanaDTO(dia.name(), DIAS_SEMANA[dia.ordinal()], lista.size(),
                    total, media(total, lista.size())));
        }
        return new DistribuicaoVendasDTO(filtro.periodo(), horas, dias, agora());
    }

    public VendasPorCategoriaDTO porCategoria(DashboardFiltro filtro) {
        Map<String, String> categoriaPorProduto = categoriaPorProduto();
        Map<String, long[]> quantidades = new TreeMap<>();
        Map<String, BigDecimal> valores = new TreeMap<>();

        for (Pedido p : validos(buscar(filtro))) {
            for (ItemPedido item : itens(p)) {
                String categoria = categoria(item, categoriaPorProduto);
                if (filtro.categoria() != null && !filtro.categoria().equals(categoria)) {
                    continue;
                }
                quantidades.computeIfAbsent(categoria, k -> new long[1])[0] += item.getQuantidade();
                valores.merge(categoria, valor(item.getSubtotal()), BigDecimal::add);
            }
        }

        BigDecimal total = valores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<VendasPorCategoriaDTO.CategoriaDTO> itens = valores.entrySet().stream()
                .map(e -> new VendasPorCategoriaDTO.CategoriaDTO(e.getKey(), quantidades.get(e.getKey())[0],
                        dinheiro(e.getValue()), percentual(e.getValue(), total)))
                .sorted(Comparator.comparing(VendasPorCategoriaDTO.CategoriaDTO::faturamento).reversed())
                .toList();
        return new VendasPorCategoriaDTO(filtro.periodo(), dinheiro(total), itens, agora());
    }

    public VendasPorTipoAtendimentoDTO porTipoAtendimento(DashboardFiltro filtro) {
        List<Pedido> validos = validos(buscar(filtro));
        Map<TipoAtendimento, List<Pedido>> porTipo = validos.stream()
                .filter(p -> p.getTipoAtendimento() != null)
                .collect(Collectors.groupingBy(Pedido::getTipoAtendimento));

        List<VendasPorTipoAtendimentoDTO.TipoDTO> itens = Arrays.stream(TipoAtendimento.values()).map(tipo -> {
            List<Pedido> lista = porTipo.getOrDefault(tipo, List.of());
            BigDecimal total = somaTotal(lista);
            return new VendasPorTipoAtendimentoDTO.TipoDTO(tipo, tipo.getDescricao(), lista.size(), total,
                    media(total, lista.size()),
                    percentual(BigDecimal.valueOf(lista.size()), BigDecimal.valueOf(validos.size())));
        }).toList();
        return new VendasPorTipoAtendimentoDTO(filtro.periodo(), itens, agora());
    }

    // =====================================================================
    // RF03 — Situação dos pedidos
    // =====================================================================

    public StatusPedidosDTO statusPedidos(DashboardFiltro filtro) {
        List<Pedido> pedidos = buscar(filtro);
        Map<StatusPedido, List<Pedido>> porStatus = pedidos.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(Pedido::getStatus));

        List<StatusPedidosDTO.StatusDTO> itens = Arrays.stream(StatusPedido.values()).map(s -> {
            List<Pedido> lista = porStatus.getOrDefault(s, List.of());
            return new StatusPedidosDTO.StatusDTO(s, s.getDescricao(), grupo(s), lista.size(),
                    percentual(BigDecimal.valueOf(lista.size()), BigDecimal.valueOf(pedidos.size())),
                    somaTotal(lista));
        }).toList();

        long emAndamento = pedidos.stream().filter(DashboardService::emAndamento).count();
        long finalizados = porStatus.getOrDefault(StatusPedido.FINALIZADO, List.of()).size();
        long cancelados = porStatus.getOrDefault(StatusPedido.CANCELADO, List.of()).size();
        return new StatusPedidosDTO(filtro.periodo(), pedidos.size(), emAndamento, finalizados, cancelados,
                itens, agora());
    }

    public PedidosRecentesDTO pedidosRecentes(DashboardFiltro filtro, int limite) {
        validarLimite(limite);
        List<Pedido> recentes = buscar(filtro).stream()
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrada).reversed())
                .limit(limite)
                .toList();

        Set<String> clienteIds = recentes.stream().map(Pedido::getClienteId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nomes = new LinkedHashMap<>();
        clienteRepository.findAllById(clienteIds).forEach(c -> nomes.put(c.getId(), c.getNome()));

        LocalDateTime agora = agora();
        List<PedidosRecentesDTO.PedidoResumoDTO> itens = recentes.stream().map(p -> {
            Long minutos = emAndamento(p) ? Duration.between(p.getDataHoraEntrada(), agora).toMinutes() : null;
            return new PedidosRecentesDTO.PedidoResumoDTO(p.getId(),
                    nomes.getOrDefault(p.getClienteId(), "Cliente não encontrado"),
                    p.getTipoAtendimento(), p.getStatus(),
                    p.getStatus() != null ? p.getStatus().getDescricao() : "Sem status",
                    itens(p).stream().mapToInt(ItemPedido::getQuantidade).sum(),
                    dinheiro(valor(p.getValorTotal())), p.getDataHoraEntrada(), minutos,
                    minutos != null && minutos > MINUTOS_ATRASO);
        }).toList();
        return new PedidosRecentesDTO(filtro.periodo(), MINUTOS_ATRASO, itens, agora);
    }

    // =====================================================================
    // RF04 — Produtos mais vendidos
    // =====================================================================

    public RankingProdutosDTO maisVendidos(DashboardFiltro filtro, int limite, CriterioRanking criterio) {
        validarLimite(limite);
        CriterioRanking crit = criterio != null ? criterio : CriterioRanking.QUANTIDADE;
        Map<String, String> categoriaPorProduto = categoriaPorProduto();
        Map<String, Acumulado> porProduto = new LinkedHashMap<>();

        for (Pedido p : validos(buscar(filtro))) {
            Set<String> contados = new TreeSet<>();
            for (ItemPedido item : itens(p)) {
                String categoria = categoria(item, categoriaPorProduto);
                if (filtro.categoria() != null && !filtro.categoria().equals(categoria)) {
                    continue;
                }
                String chave = item.getProdutoId() != null ? item.getProdutoId() : item.getNomeProduto();
                Acumulado acc = porProduto.computeIfAbsent(chave,
                        k -> new Acumulado(item.getProdutoId(), item.getNomeProduto(), categoria));
                acc.quantidade += item.getQuantidade();
                acc.faturamento = acc.faturamento.add(valor(item.getSubtotal()));
                if (contados.add(chave)) {
                    acc.pedidos++;
                }
            }
        }

        long totalItens = porProduto.values().stream().mapToLong(a -> a.quantidade).sum();
        Comparator<Acumulado> ordem = crit == CriterioRanking.FATURAMENTO
                ? Comparator.comparing((Acumulado a) -> a.faturamento).thenComparingLong(a -> a.quantidade)
                : Comparator.comparingLong((Acumulado a) -> a.quantidade).thenComparing(a -> a.faturamento);

        List<Acumulado> ordenados = porProduto.values().stream().sorted(ordem.reversed()).limit(limite).toList();
        List<RankingProdutosDTO.ProdutoRankingDTO> itens = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            Acumulado a = ordenados.get(i);
            itens.add(new RankingProdutosDTO.ProdutoRankingDTO(i + 1, a.produtoId, a.nome, a.categoria,
                    a.quantidade, a.pedidos, dinheiro(a.faturamento),
                    percentual(BigDecimal.valueOf(a.quantidade), BigDecimal.valueOf(totalItens))));
        }
        return new RankingProdutosDTO(filtro.periodo(), crit, totalItens, itens, agora());
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private List<Pedido> buscar(DashboardFiltro filtro) {
        List<String> produtosDaCategoria = List.of();
        if (filtro.categoria() != null) {
            produtosDaCategoria = produtoRepository.findAll().stream()
                    .filter(p -> filtro.categoria().equals(p.getTipo()))
                    .map(Produto::getId)
                    .toList();
        }
        // Descarta registros sem data de entrada: não há como posicioná-los no período
        return dashboardRepository.buscarPedidos(filtro, produtosDaCategoria).stream()
                .filter(p -> p.getDataHoraEntrada() != null)
                .toList();
    }

    private Map<String, String> categoriaPorProduto() {
        return produtoRepository.findAll().stream()
                .filter(p -> p.getId() != null && p.getTipo() != null)
                .collect(Collectors.toMap(Produto::getId, Produto::getTipo, (a, b) -> a));
    }

    // Prioriza a categoria gravada no item; pedidos antigos caem no cadastro atual do produto
    private static String categoria(ItemPedido item, Map<String, String> categoriaPorProduto) {
        if (item.getCategoria() != null && !item.getCategoria().isBlank()) {
            return item.getCategoria();
        }
        return categoriaPorProduto.getOrDefault(item.getProdutoId(), SEM_CATEGORIA);
    }

    private static List<ItemPedido> itens(Pedido pedido) {
        return pedido.getItens() != null ? pedido.getItens() : List.of();
    }

    private static List<Pedido> validos(List<Pedido> pedidos) {
        return pedidos.stream().filter(p -> p.getStatus() != StatusPedido.CANCELADO).toList();
    }

    private static boolean emAndamento(Pedido p) {
        return p.getStatus() != null && p.getStatus() != StatusPedido.FINALIZADO
                && p.getStatus() != StatusPedido.CANCELADO;
    }

    private static String grupo(StatusPedido s) {
        return switch (s) {
            case FINALIZADO -> "CONCLUIDO";
            case CANCELADO -> "CANCELADO";
            default -> "EM_ANDAMENTO";
        };
    }

    private static void validarLimite(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new RegraNegocioException("O limite deve estar entre 1 e " + LIMITE_MAXIMO);
        }
    }

    private static LocalDate chave(LocalDate data, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    private static LocalDate proxima(LocalDate chave, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> chave.plusDays(1);
            case SEMANA -> chave.plusWeeks(1);
            case MES -> chave.plusMonths(1);
        };
    }

    private static String rotulo(LocalDate chave, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> chave.format(DIA_MES);
            case SEMANA -> "Sem. " + chave.format(DIA_MES);
            case MES -> chave.format(MES_ANO).replace(".", "");
        };
    }

    private static BigDecimal somaTotal(List<Pedido> pedidos) {
        return dinheiro(pedidos.stream().map(p -> valor(p.getValorTotal())).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal valor(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal dinheiro(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal media(BigDecimal total, long quantidade) {
        return quantidade == 0 ? dinheiro(BigDecimal.ZERO)
                : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return parte.multiply(CEM).divide(total, 1, RoundingMode.HALF_EVEN);
    }

    /** Variação percentual entre períodos; null quando o período anterior é zero (sem base de comparação). */
    static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, 1, RoundingMode.HALF_EVEN);
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private static final class Acumulado {
        private final String produtoId;
        private final String nome;
        private final String categoria;
        private long quantidade;
        private long pedidos;
        private BigDecimal faturamento = BigDecimal.ZERO;

        private Acumulado(String produtoId, String nome, String categoria) {
            this.produtoId = produtoId;
            this.nome = nome;
            this.categoria = categoria;
        }
    }

    /** Totais de um conjunto de pedidos; base do resumo e de cada ponto da evolução. */
    private record Indicadores(long totalPedidos, long pedidosValidos, BigDecimal faturamento, long itensVendidos,
                               BigDecimal descontos, long cancelados, BigDecimal valorCancelado, long emAndamento,
                               long somaMinutosAtendimento, long pedidosComSaida) {

        static Indicadores de(List<Pedido> pedidos) {
            List<Pedido> validos = validos(pedidos);
            List<Pedido> cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).toList();
            Function<Pedido, BigDecimal> desconto = p -> valor(p.getValorDesconto());

            long somaMinutos = 0;
            long comSaida = 0;
            for (Pedido p : validos) {
                if (p.getDataHoraSaida() != null && !p.getDataHoraSaida().isBefore(p.getDataHoraEntrada())) {
                    somaMinutos += Duration.between(p.getDataHoraEntrada(), p.getDataHoraSaida()).toMinutes();
                    comSaida++;
                }
            }

            return new Indicadores(pedidos.size(), validos.size(), somaTotal(validos),
                    validos.stream().flatMap(p -> itens(p).stream()).mapToLong(ItemPedido::getQuantidade).sum(),
                    dinheiro(validos.stream().map(desconto).reduce(BigDecimal.ZERO, BigDecimal::add)),
                    cancelados.size(), somaTotal(cancelados),
                    pedidos.stream().filter(DashboardService::emAndamento).count(),
                    somaMinutos, comSaida);
        }

        BigDecimal ticketMedio() {
            return media(faturamento, pedidosValidos);
        }

        BigDecimal taxaCancelamento() {
            return percentual(BigDecimal.valueOf(cancelados), BigDecimal.valueOf(totalPedidos));
        }

        BigDecimal tempoMedioAtendimento() {
            return pedidosComSaida == 0 ? null
                    : BigDecimal.valueOf(somaMinutosAtendimento).divide(BigDecimal.valueOf(pedidosComSaida), 1,
                    RoundingMode.HALF_EVEN);
        }
    }
}
