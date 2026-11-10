/*
 * Dashboard administrativo — consome /api/dashboard/** (RF06).
 * Cada bloco carrega de forma independente: se um endpoint falhar, só aquele bloco mostra o erro (RF07).
 */
(function () {
    'use strict';

    const API = '/api/dashboard';
    const INTERVALO_ATUALIZACAO_MS = 60000;

    // Paleta validada (categórica em ordem fixa) + tinta e grade recessivas
    const COR = {
        serie1: '#2a78d6',
        serie2: '#eb6834',
        texto: '#0b0b0b',
        textoSecundario: '#52514e',
        mudo: '#898781',
        grade: '#e1e0d9',
        eixo: '#c3c2b7',
        bom: '#006300',
        critico: '#d03b3b'
    };

    const brl = new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'});
    const brlCompacto = new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1});
    const inteiro = new Intl.NumberFormat('pt-BR');
    const decimal1 = new Intl.NumberFormat('pt-BR', {minimumFractionDigits: 1, maximumFractionDigits: 1});

    const estado = {
        agrupamento: 'DIA',
        criterio: 'QUANTIDADE',
        graficos: {},
        tabelas: {},
        timer: null,
        carregando: false
    };

    const $ = (sel, raiz) => (raiz || document).querySelector(sel);
    const $$ = (sel, raiz) => Array.from((raiz || document).querySelectorAll(sel));

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    function esc(valor) {
        return String(valor ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    function num(v) {
        const n = Number(v);
        return Number.isFinite(n) ? n : 0;
    }

    function dataISO(d) {
        const p = n => String(n).padStart(2, '0');
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
    }

    function dataBR(iso) {
        if (!iso) return '—';
        const [a, m, d] = String(iso).substring(0, 10).split('-');
        return d + '/' + m + '/' + a;
    }

    function dataHoraBR(iso) {
        if (!iso) return '—';
        return dataBR(iso).substring(0, 5) + ' ' + String(iso).substring(11, 16);
    }

    function filtrosAtuais() {
        return {
            dataInicio: $('#f-inicio').value,
            dataFim: $('#f-fim').value,
            tipoAtendimento: $('#f-tipo').value,
            status: $('#f-status').value,
            categoria: $('#f-categoria').value
        };
    }

    function query(extra) {
        const params = new URLSearchParams();
        Object.entries(Object.assign(filtrosAtuais(), extra || {})).forEach(([k, v]) => {
            if (v !== '' && v != null) params.append(k, v);
        });
        const s = params.toString();
        return s ? '?' + s : '';
    }

    // Chamada à API com tratamento padronizado de erro (StandardError do backend)
    async function api(caminho) {
        let resposta;
        try {
            resposta = await fetch(API + caminho, {
                headers: {'Accept': 'application/json'},
                credentials: 'same-origin'
            });
        } catch (e) {
            throw new Error('Sem conexão com o servidor.');
        }
        if (resposta.status === 401) {
            window.location.href = '/login';
            throw new Error('Sessão expirada. Faça login novamente.');
        }
        if (!resposta.ok) {
            let mensagem = 'Erro ' + resposta.status + ' ao consultar a API.';
            try {
                const corpo = await resposta.json();
                if (corpo && corpo.message) mensagem = corpo.message;
            } catch (e) { /* corpo não é JSON */ }
            if (resposta.status === 403) mensagem = 'Seu perfil não tem acesso ao dashboard.';
            throw new Error(mensagem);
        }
        return resposta.json();
    }

    function vazio(mensagem) {
        return '<div class="estado-vazio"><i class="bi bi-inbox"></i>' + esc(mensagem || 'Nenhum pedido no período selecionado.') + '</div>';
    }

    function erroBloco(e) {
        return '<div class="estado-vazio text-danger"><i class="bi bi-exclamation-triangle"></i>' + esc(e.message) + '</div>';
    }

    function bloco(nome) {
        return $('[data-bloco="' + nome + '"]');
    }

    // Executa o carregamento de um bloco marcando-o como "carregando" e isolando falhas
    async function carregarBloco(nomes, fn) {
        const blocos = [].concat(nomes).map(bloco).filter(Boolean);
        blocos.forEach(b => b.classList.add('carregando'));
        try {
            await fn();
            return true;
        } catch (e) {
            console.error('[dashboard]', e);
            blocos.forEach(b => {
                const corpo = $('.card-body', b);
                const grafico = $('.grafico', corpo);
                if (grafico) {
                    grafico.classList.add('d-none');
                    let aviso = $('.aviso-erro', corpo);
                    if (!aviso) {
                        aviso = document.createElement('div');
                        aviso.className = 'aviso-erro';
                        corpo.prepend(aviso);
                    }
                    aviso.innerHTML = erroBloco(e);
                } else {
                    corpo.innerHTML = erroBloco(e);
                }
            });
            return false;
        } finally {
            blocos.forEach(b => b.classList.remove('carregando'));
        }
    }

    function limparErroGrafico(nome) {
        const b = bloco(nome);
        if (!b) return;
        const aviso = $('.aviso-erro', b);
        if (aviso) aviso.remove();
        const grafico = $('.grafico', b);
        const tabela = $('.tabela-dados', b);
        if (grafico && (!tabela || tabela.classList.contains('d-none'))) grafico.classList.remove('d-none');
    }

    // ------------------------------------------------------------------
    // Filtros (RF05)
    // ------------------------------------------------------------------

    function aplicarPreset(valor) {
        const hoje = new Date();
        let inicio = new Date(hoje);
        if (valor === 'custom') return;
        if (valor === 'mes') {
            inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        } else {
            const dias = Number(valor);
            inicio.setDate(hoje.getDate() - Math.max(dias - 1, 0));
        }
        $('#f-inicio').value = dataISO(inicio);
        $('#f-fim').value = dataISO(hoje);
    }

    function validarFiltros() {
        const erro = $('#db-filtro-erro');
        const {dataInicio, dataFim} = filtrosAtuais();
        let mensagem = '';
        if (dataInicio && dataFim && dataInicio > dataFim) {
            mensagem = 'A data inicial não pode ser posterior à data final.';
        }
        erro.textContent = mensagem;
        erro.classList.toggle('d-none', !mensagem);
        return !mensagem;
    }

    async function carregarOpcoes() {
        const opcoes = await api('/filtros');
        const preencher = (sel, itens, valor, texto) => {
            const el = $(sel);
            itens.forEach(i => {
                const o = document.createElement('option');
                o.value = valor(i);
                o.textContent = texto(i);
                el.appendChild(o);
            });
        };
        preencher('#f-tipo', opcoes.tiposAtendimento, i => i.valor, i => i.descricao);
        preencher('#f-status', opcoes.status, i => i.valor, i => i.descricao);
        preencher('#f-categoria', opcoes.categorias, i => i, i => i);
        if (opcoes.primeiroPedido) $('#f-inicio').min = opcoes.primeiroPedido;
    }

    // ------------------------------------------------------------------
    // RF01 — Indicadores
    // ------------------------------------------------------------------

    function delta(variacao, opcoes) {
        const o = Object.assign({inverter: false, sufixo: '%', rotulo: 'vs. período anterior'}, opcoes);
        if (variacao === null || variacao === undefined) {
            return '<span class="text-muted">Sem base de comparação</span>';
        }
        const v = num(variacao);
        if (v === 0) {
            return '<span class="text-muted"><i class="bi bi-dash"></i> estável ' + esc(o.rotulo) + '</span>';
        }
        const subiu = v > 0;
        const bom = o.inverter ? !subiu : subiu;
        const icone = subiu ? 'bi-arrow-up-right' : 'bi-arrow-down-right';
        const texto = (subiu ? '+' : '') + decimal1.format(v) + o.sufixo;
        return '<span style="color:' + (bom ? COR.bom : COR.critico) + '"><i class="bi ' + icone + '"></i> ' +
            esc(texto) + '</span> <span class="text-muted">' + esc(o.rotulo) + '</span>';
    }

    function kpi(nome, rotulo, valor, rodape) {
        const card = $('[data-kpi="' + nome + '"]');
        $('.kpi-rotulo', card).textContent = rotulo;
        $('.kpi-valor', card).innerHTML = esc(valor);
        $('.kpi-delta', card).innerHTML = rodape;
    }

    async function carregarResumo() {
        const r = await api('/resumo' + query());
        const c = r.comparativo || {};
        $('#db-periodo-texto').textContent = 'Período: ' + dataBR(r.periodo.inicio) + ' a ' + dataBR(r.periodo.fim) +
            ' (' + r.periodo.dias + (r.periodo.dias === 1 ? ' dia' : ' dias') + ') · comparado a ' +
            dataBR(c.periodoAnterior?.inicio) + ' a ' + dataBR(c.periodoAnterior?.fim);

        // Sem histórico suficiente no período anterior, a API não calcula variações (evita "+6.000%")
        const semBase = '<span class="text-muted" title="O período anterior começa antes do primeiro pedido registrado">' +
            '<i class="bi bi-info-circle"></i> Histórico insuficiente para comparar</span>';
        const comparar = (v, opcoes) => c.baseCompleta ? delta(v, opcoes) : semBase;

        kpi('faturamento', 'Faturamento', brl.format(num(r.faturamento)), comparar(c.variacaoFaturamento));
        kpi('pedidos', 'Pedidos', inteiro.format(r.pedidosValidos), comparar(c.variacaoPedidos));
        kpi('ticket', 'Ticket médio', brl.format(num(r.ticketMedio)), comparar(c.variacaoTicketMedio));
        kpi('itens', 'Itens vendidos', inteiro.format(r.itensVendidos),
            '<span class="text-muted">Descontos: ' + esc(brl.format(num(r.totalDescontos))) + '</span>');

        const difTaxa = c.taxaCancelamentoAnterior == null ? null : num(r.taxaCancelamento) - num(c.taxaCancelamentoAnterior);
        kpi('cancelamento', 'Cancelamentos', decimal1.format(num(r.taxaCancelamento)) + '%',
            '<span class="text-muted">' + esc(inteiro.format(r.pedidosCancelados) + ' pedidos · ' + brl.format(num(r.valorCancelado))) +
            '</span><br>' + comparar(r.totalPedidos && c.taxaCancelamentoAnterior != null ? difTaxa : null,
                {inverter: true, sufixo: ' p.p.'}));

        kpi('tempo', 'Tempo médio até a saída',
            r.tempoMedioAtendimentoMinutos == null ? '—' : decimal1.format(num(r.tempoMedioAtendimentoMinutos)) + ' min',
            '<span class="text-muted">' + esc(inteiro.format(r.pedidosEmAndamento)) + ' pedido(s) em andamento</span>');

        $('#db-atualizado-em').textContent = dataHoraBR(r.geradoEm) + ':' + String(r.geradoEm).substring(17, 19);
    }

    // ------------------------------------------------------------------
    // RF02 — Gráficos
    // ------------------------------------------------------------------

    function configurarChartJs() {
        if (!window.Chart) return false;
        Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", sans-serif';
        Chart.defaults.font.size = 12;
        Chart.defaults.color = COR.mudo;
        Chart.defaults.animation.duration = 300;
        Chart.defaults.plugins.tooltip.backgroundColor = '#0b0b0b';
        Chart.defaults.plugins.tooltip.padding = 10;
        Chart.defaults.plugins.tooltip.cornerRadius = 6;
        Chart.defaults.plugins.legend.labels.boxWidth = 10;
        Chart.defaults.plugins.legend.labels.boxHeight = 10;
        Chart.defaults.plugins.legend.labels.color = COR.textoSecundario;
        return true;
    }

    function eixos(formatarY) {
        return {
            x: {grid: {display: false}, border: {color: COR.eixo}, ticks: {maxRotation: 0, autoSkipPadding: 12}},
            y: {
                beginAtZero: true,
                grid: {color: COR.grade},
                border: {display: false},
                ticks: {callback: formatarY, maxTicksLimit: 6}
            }
        };
    }

    function desenhar(id, config) {
        if (!window.Chart) {
            throw new Error('Biblioteca de gráficos indisponível (verifique a conexão com a internet).');
        }
        if (estado.graficos[id]) {
            estado.graficos[id].destroy();
        }
        estado.graficos[id] = new Chart(document.getElementById(id), config);
    }

    function registrarTabela(alvo, cabecalho, linhas) {
        estado.tabelas[alvo] = {cabecalho, linhas};
        const el = $('[data-tabela="' + alvo + '"]');
        el.innerHTML = '<div class="table-responsive"><table class="table table-sm mb-0"><thead><tr>' +
            cabecalho.map((c, i) => '<th' + (i ? ' class="text-end"' : '') + '>' + esc(c) + '</th>').join('') +
            '</tr></thead><tbody>' +
            linhas.map(l => '<tr>' + l.map((c, i) => '<td' + (i ? ' class="text-end"' : '') + '>' + esc(c) + '</td>').join('') + '</tr>').join('') +
            '</tbody></table></div>';
    }

    async function carregarEvolucao() {
        const r = await api('/vendas/evolucao' + query({agrupamento: estado.agrupamento}));
        const pontos = r.pontos || [];
        const rotulos = pontos.map(p => p.rotulo);
        limparErroGrafico('evolucao');
        limparErroGrafico('pedidos');

        desenhar('ch-faturamento', {
            type: 'line',
            data: {
                labels: rotulos,
                datasets: [{
                    label: 'Faturamento',
                    data: pontos.map(p => num(p.faturamento)),
                    borderColor: COR.serie1,
                    backgroundColor: 'rgba(42,120,214,0.08)',
                    fill: true,
                    borderWidth: 2,
                    tension: 0.3,
                    cubicInterpolationMode: 'monotone',
                    pointRadius: pontos.length > 45 ? 0 : 2,
                    pointHoverRadius: 5,
                    pointHitRadius: 12,
                    // Semana/mês incompleto no período: trecho tracejado para não parecer uma queda real
                    segment: {
                        borderDash: ctx => (pontos[ctx.p0DataIndex]?.parcial || pontos[ctx.p1DataIndex]?.parcial) ? [5, 4] : undefined
                    }
                }]
            },
            options: {
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            title: items => items[0].label + (pontos[items[0].dataIndex].parcial ? ' (período parcial)' : ''),
                            label: ctx => ' Faturamento: ' + brl.format(ctx.parsed.y),
                            afterLabel: ctx => {
                                const p = pontos[ctx.dataIndex];
                                return ' Pedidos: ' + inteiro.format(p.pedidos) + '\n Ticket médio: ' + brl.format(num(p.ticketMedio));
                            }
                        }
                    }
                },
                scales: eixos(v => brlCompacto.format(v))
            }
        });

        desenhar('ch-pedidos', {
            type: 'bar',
            data: {
                labels: rotulos,
                datasets: [
                    {label: 'Válidos', data: pontos.map(p => p.pedidos), backgroundColor: COR.serie1, borderRadius: 3, maxBarThickness: 28},
                    {label: 'Cancelados', data: pontos.map(p => p.cancelados), backgroundColor: COR.serie2, borderRadius: 3, maxBarThickness: 28}
                ]
            },
            options: {
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                plugins: {legend: {position: 'top', align: 'end'}},
                scales: Object.assign(eixos(v => Number.isInteger(v) ? inteiro.format(v) : ''), {
                    x: {stacked: true, grid: {display: false}, border: {color: COR.eixo}, ticks: {maxRotation: 0, autoSkipPadding: 12}},
                    y: {stacked: true, beginAtZero: true, grid: {color: COR.grade}, border: {display: false},
                        ticks: {precision: 0, maxTicksLimit: 6}}
                })
            }
        });

        const titulo = {DIA: 'Dia', SEMANA: 'Semana (início)', MES: 'Mês'}[r.agrupamento];
        registrarTabela('faturamento', [titulo, 'Faturamento', 'Pedidos', 'Ticket médio'],
            pontos.map(p => [p.rotulo + (p.parcial ? ' (parcial)' : ''), brl.format(num(p.faturamento)), inteiro.format(p.pedidos), brl.format(num(p.ticketMedio))]));
        registrarTabela('pedidos', [titulo, 'Válidos', 'Cancelados'],
            pontos.map(p => [p.rotulo, inteiro.format(p.pedidos), inteiro.format(p.cancelados)]));
    }

    async function carregarDistribuicao() {
        const r = await api('/vendas/distribuicao' + query());
        limparErroGrafico('hora');
        limparErroGrafico('semana');

        // Mostra só a faixa de horas com movimento (ex.: 11h às 23h)
        let horas = r.porHora || [];
        const comVenda = horas.filter(h => h.pedidos > 0).map(h => h.hora);
        if (comVenda.length) {
            const min = Math.min(...comVenda), max = Math.max(...comVenda);
            horas = horas.filter(h => h.hora >= min && h.hora <= max);
        }
        const maxHora = Math.max(0, ...horas.map(h => h.pedidos));

        desenhar('ch-hora', {
            type: 'bar',
            data: {
                labels: horas.map(h => h.rotulo),
                datasets: [{
                    label: 'Pedidos',
                    data: horas.map(h => h.pedidos),
                    backgroundColor: horas.map(h => h.pedidos === maxHora && maxHora > 0 ? COR.serie1 : 'rgba(42,120,214,0.45)'),
                    borderRadius: 4,
                    maxBarThickness: 32
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: ctx => ' Pedidos: ' + inteiro.format(ctx.parsed.y),
                            afterLabel: ctx => ' Faturamento: ' + brl.format(num(horas[ctx.dataIndex].faturamento))
                        }
                    }
                },
                scales: Object.assign(eixos(), {
                    y: {beginAtZero: true, grid: {color: COR.grade}, border: {display: false}, ticks: {precision: 0, maxTicksLimit: 6}}
                })
            }
        });

        const dias = r.porDiaSemana || [];
        desenhar('ch-semana', {
            type: 'bar',
            data: {
                labels: dias.map(d => d.rotulo),
                datasets: [{
                    label: 'Faturamento',
                    data: dias.map(d => num(d.faturamento)),
                    backgroundColor: COR.serie1,
                    borderRadius: 4,
                    maxBarThickness: 40
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            label: ctx => ' Faturamento: ' + brl.format(ctx.parsed.y),
                            afterLabel: ctx => {
                                const d = dias[ctx.dataIndex];
                                return ' Pedidos: ' + inteiro.format(d.pedidos) + '\n Ticket médio: ' + brl.format(num(d.ticketMedio));
                            }
                        }
                    }
                },
                scales: eixos(v => brlCompacto.format(v))
            }
        });

        registrarTabela('hora', ['Hora', 'Pedidos', 'Faturamento'],
            horas.map(h => [h.rotulo, inteiro.format(h.pedidos), brl.format(num(h.faturamento))]));
        registrarTabela('semana', ['Dia', 'Pedidos', 'Faturamento', 'Ticket médio'],
            dias.map(d => [d.rotulo, inteiro.format(d.pedidos), brl.format(num(d.faturamento)), brl.format(num(d.ticketMedio))]));
    }

    async function carregarTipoAtendimento() {
        const r = await api('/vendas/por-tipo-atendimento' + query());
        const el = $('#db-tipo');
        const itens = r.itens || [];
        const total = itens.reduce((s, i) => s + i.pedidos, 0);
        if (!total) {
            el.innerHTML = vazio();
            return;
        }
        const cores = [COR.serie1, COR.serie2];
        el.innerHTML =
            '<div class="barra-dividida mb-3" role="img" aria-label="' + esc(itens.map(i => i.descricao + ' ' + decimal1.format(num(i.participacao)) + '%').join(', ')) + '">' +
            itens.filter(i => i.pedidos > 0).map(i =>
                '<span style="width:' + num(i.participacao) + '%;background:' + cores[itens.indexOf(i)] + '" title="' +
                esc(i.descricao + ': ' + decimal1.format(num(i.participacao)) + '%') + '"></span>').join('') +
            '</div>' +
            itens.map((i, idx) =>
                '<div class="d-flex justify-content-between align-items-start py-2' + (idx ? ' border-top' : '') + '">' +
                '<div><span class="amostra" style="background:' + cores[idx] + '"></span><strong>' + esc(i.descricao) + '</strong>' +
                '<div class="small text-muted ms-3">' + esc(inteiro.format(i.pedidos)) + ' pedidos · ticket ' + esc(brl.format(num(i.ticketMedio))) + '</div></div>' +
                '<div class="text-end"><div class="fw-semibold">' + esc(decimal1.format(num(i.participacao))) + '%</div>' +
                '<div class="small text-muted">' + esc(brl.format(num(i.faturamento))) + '</div></div></div>'
            ).join('');
    }

    async function carregarCategorias() {
        const r = await api('/vendas/por-categoria' + query());
        const el = $('#db-categoria');
        const itens = r.itens || [];
        if (!itens.length) {
            el.innerHTML = vazio();
            return;
        }
        const max = Math.max(...itens.map(i => num(i.faturamento)));
        el.innerHTML = itens.map(i =>
            '<div class="linha-barra">' +
            '<div class="d-flex justify-content-between small"><span class="fw-semibold">' + esc(i.categoria) + '</span>' +
            '<span>' + esc(brl.format(num(i.faturamento))) + ' <span class="text-muted">(' + esc(decimal1.format(num(i.participacao))) + '%)</span></span></div>' +
            '<div class="trilho"><span style="width:' + (max ? num(i.faturamento) / max * 100 : 0) + '%"></span></div>' +
            '<div class="small text-muted">' + esc(inteiro.format(i.quantidade)) + ' itens</div></div>'
        ).join('') +
            '<div class="small text-muted border-top pt-2 mt-2">Total em itens: ' + esc(brl.format(num(r.faturamentoItens))) + '</div>';
    }

    // ------------------------------------------------------------------
    // RF03 — Situação dos pedidos
    // ------------------------------------------------------------------

    const ICONE_GRUPO = {
        EM_ANDAMENTO: '<i class="bi bi-hourglass-split text-primary" aria-hidden="true"></i>',
        CONCLUIDO: '<i class="bi bi-check-circle-fill" style="color:#0ca30c" aria-hidden="true"></i>',
        CANCELADO: '<i class="bi bi-x-circle-fill" style="color:#d03b3b" aria-hidden="true"></i>'
    };

    async function carregarStatus() {
        const r = await api('/pedidos/status' + query());
        const el = $('#db-status');
        $('#db-status-resumo').textContent = inteiro.format(r.total) + ' pedidos · ' + inteiro.format(r.emAndamento) +
            ' em andamento · ' + inteiro.format(r.finalizados) + ' finalizados · ' + inteiro.format(r.cancelados) + ' cancelados';
        if (!r.total) {
            el.innerHTML = vazio();
            return;
        }
        const max = Math.max(...r.itens.map(i => i.quantidade));
        el.innerHTML = r.itens.map(i =>
            '<div class="linha-barra' + (i.quantidade ? '' : ' opacity-50') + '">' +
            '<div class="d-flex justify-content-between small">' +
            '<span>' + ICONE_GRUPO[i.grupo] + ' ' + esc(i.descricao) + '</span>' +
            '<span><strong>' + esc(inteiro.format(i.quantidade)) + '</strong> <span class="text-muted">(' + esc(decimal1.format(num(i.percentual))) + '%)</span></span></div>' +
            '<div class="trilho"><span style="width:' + (max ? i.quantidade / max * 100 : 0) + '%"></span></div></div>'
        ).join('');
    }

    async function carregarRecentes() {
        const r = await api('/pedidos/recentes' + query({limite: 10}));
        const el = $('#db-recentes');
        $('#db-recentes-legenda').textContent = 'Últimos ' + (r.itens || []).length + ' pedidos do período · em andamento há mais de ' +
            r.limiteAtrasoMinutos + ' min são marcados como atrasados';
        if (!r.itens || !r.itens.length) {
            el.innerHTML = vazio();
            return;
        }
        el.innerHTML = '<div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead class="table-light"><tr>' +
            '<th class="ps-3">Entrada</th><th>Cliente</th><th>Atendimento</th><th>Status</th><th class="text-end">Itens</th>' +
            '<th class="text-end">Valor</th><th class="text-end pe-3">Em aberto</th></tr></thead><tbody>' +
            r.itens.map(p =>
                '<tr><td class="ps-3 text-nowrap">' + esc(dataHoraBR(p.dataHoraEntrada)) + '</td>' +
                '<td>' + esc(p.cliente) + '</td>' +
                '<td>' + esc(p.tipoAtendimento === 'RETIRADA' ? 'Retirada' : 'Entrega') + '</td>' +
                '<td><span class="badge rounded-pill text-bg-light border">' + esc(p.statusDescricao) + '</span></td>' +
                '<td class="text-end">' + esc(p.quantidadeItens) + '</td>' +
                '<td class="text-end text-nowrap">' + esc(brl.format(num(p.valorTotal))) + '</td>' +
                '<td class="text-end pe-3 text-nowrap">' + (p.minutosEmAberto == null ? '<span class="text-muted">—</span>' :
                    (p.atrasado
                        ? '<span class="badge text-bg-danger"><i class="bi bi-exclamation-triangle-fill"></i> Atrasado · ' + esc(p.minutosEmAberto) + ' min</span>'
                        : esc(p.minutosEmAberto) + ' min')) + '</td></tr>'
            ).join('') + '</tbody></table></div>';
    }

    // ------------------------------------------------------------------
    // RF04 — Ranking de produtos
    // ------------------------------------------------------------------

    async function carregarRanking() {
        const r = await api('/produtos/mais-vendidos' + query({limite: 10, criterio: estado.criterio}));
        const el = $('#db-ranking');
        if (!r.itens || !r.itens.length) {
            el.innerHTML = vazio();
            return;
        }
        const porFaturamento = r.criterio === 'FATURAMENTO';
        const valorBarra = p => porFaturamento ? num(p.faturamento) : p.quantidade;
        const max = Math.max(...r.itens.map(valorBarra));
        el.innerHTML = '<div class="table-responsive"><table class="table align-middle mb-0 ranking"><thead class="table-light"><tr>' +
            '<th class="ps-3">#</th><th>Produto</th><th class="text-end">Qtd.</th><th class="text-end">Pedidos</th>' +
            '<th class="text-end">Faturamento</th><th class="text-end pe-3">Part.</th></tr></thead><tbody>' +
            r.itens.map(p =>
                '<tr><td class="ps-3 text-muted">' + p.posicao + '</td>' +
                '<td><div class="fw-semibold">' + esc(p.nome) + '</div><div class="small text-muted">' + esc(p.categoria) + '</div>' +
                '<div class="trilho fino"><span style="width:' + (max ? valorBarra(p) / max * 100 : 0) + '%"></span></div></td>' +
                '<td class="text-end' + (porFaturamento ? '' : ' fw-semibold') + '">' + esc(inteiro.format(p.quantidade)) + '</td>' +
                '<td class="text-end">' + esc(inteiro.format(p.pedidos)) + '</td>' +
                '<td class="text-end text-nowrap' + (porFaturamento ? ' fw-semibold' : '') + '">' + esc(brl.format(num(p.faturamento))) + '</td>' +
                '<td class="text-end pe-3">' + esc(decimal1.format(num(p.participacao))) + '%</td></tr>'
            ).join('') + '</tbody></table></div>' +
            '<div class="small text-muted px-3 py-2 border-top">Participação sobre ' + esc(inteiro.format(r.totalItensVendidos)) + ' itens vendidos no período</div>';
    }

    // ------------------------------------------------------------------
    // Orquestração e atualização (RF06 / RF07)
    // ------------------------------------------------------------------

    async function carregarTudo() {
        if (estado.carregando || !validarFiltros()) return;
        estado.carregando = true;
        const botao = $('#db-atualizar');
        botao.disabled = true;
        $('#db-erro').classList.add('d-none');
        try {
            const resultados = await Promise.all([
                carregarBloco([], carregarResumo).catch(() => false),
                carregarBloco(['evolucao', 'pedidos'], carregarEvolucao),
                carregarBloco('tipo', carregarTipoAtendimento),
                carregarBloco('status', carregarStatus),
                carregarBloco(['hora', 'semana'], carregarDistribuicao),
                carregarBloco('ranking', carregarRanking),
                carregarBloco('categoria', carregarCategorias),
                carregarBloco('recentes', carregarRecentes)
            ]);
            if (!resultados[0]) {
                mostrarErroGeral('Não foi possível carregar os indicadores gerais.');
            }
        } finally {
            estado.carregando = false;
            botao.disabled = false;
        }
    }

    function mostrarErroGeral(mensagem) {
        const el = $('#db-erro');
        el.textContent = mensagem;
        el.classList.remove('d-none');
    }

    function configurarAutoAtualizacao() {
        clearInterval(estado.timer);
        if ($('#db-auto').checked) {
            estado.timer = setInterval(() => {
                // Não consulta a API com a aba em segundo plano
                if (document.visibilityState === 'visible') carregarTudo();
            }, INTERVALO_ATUALIZACAO_MS);
        }
    }

    function registrarEventos() {
        $('#db-filtros').addEventListener('submit', e => {
            e.preventDefault();
            carregarTudo();
        });
        $('#f-preset').addEventListener('change', e => {
            aplicarPreset(e.target.value);
            if (e.target.value !== 'custom') carregarTudo();
        });
        ['#f-inicio', '#f-fim'].forEach(sel => $(sel).addEventListener('change', () => {
            $('#f-preset').value = 'custom';
            validarFiltros();
        }));
        ['#f-tipo', '#f-status', '#f-categoria'].forEach(sel => $(sel).addEventListener('change', carregarTudo));
        $('#db-limpar').addEventListener('click', () => {
            $('#db-filtros').reset();
            $('#f-preset').value = '30';
            aplicarPreset('30');
            carregarTudo();
        });
        $('#db-atualizar').addEventListener('click', carregarTudo);
        $('#db-auto').addEventListener('change', configurarAutoAtualizacao);
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible' && $('#db-auto').checked) carregarTudo();
        });

        $$('input[name="agrupamento"]').forEach(r => r.addEventListener('change', e => {
            estado.agrupamento = e.target.value;
            carregarBloco(['evolucao', 'pedidos'], carregarEvolucao);
        }));
        $$('input[name="criterio"]').forEach(r => r.addEventListener('change', e => {
            estado.criterio = e.target.value;
            carregarBloco('ranking', carregarRanking);
        }));

        // Alterna gráfico <-> tabela (acessibilidade e conferência dos números)
        $$('.db-ver-tabela').forEach(b => b.addEventListener('click', () => {
            const card = b.closest('.card');
            const tabela = $('[data-tabela="' + b.dataset.alvo + '"]', card);
            const grafico = $('.grafico', card);
            const mostrarTabela = tabela.classList.contains('d-none');
            tabela.classList.toggle('d-none', !mostrarTabela);
            grafico.classList.toggle('d-none', mostrarTabela);
            b.textContent = mostrarTabela ? 'Ver gráfico' : 'Ver dados';
        }));
    }

    async function iniciar() {
        configurarChartJs();
        aplicarPreset('30');
        registrarEventos();
        try {
            await carregarOpcoes();
        } catch (e) {
            mostrarErroGeral('Não foi possível carregar as opções de filtro: ' + e.message);
        }
        await carregarTudo();
        configurarAutoAtualizacao();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
