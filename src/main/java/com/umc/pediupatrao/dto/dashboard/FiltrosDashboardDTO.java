package com.umc.pediupatrao.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

/** RF05 — opções disponíveis para montar os filtros do dashboard. */
public record FiltrosDashboardDTO(PeriodoDTO periodoPadrao,
                                  LocalDate primeiroPedido,
                                  LocalDate ultimoPedido,
                                  int intervaloMaximoDias,
                                  List<OpcaoDTO> status,
                                  List<OpcaoDTO> tiposAtendimento,
                                  List<String> categorias,
                                  List<Agrupamento> agrupamentos,
                                  List<CriterioRanking> criteriosRanking) {

    public record OpcaoDTO(String valor, String descricao) {
    }
}
