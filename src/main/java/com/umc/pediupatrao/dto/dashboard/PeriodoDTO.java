package com.umc.pediupatrao.dto.dashboard;

import java.time.LocalDate;

public record PeriodoDTO(LocalDate inicio, LocalDate fim, long dias) {
}
