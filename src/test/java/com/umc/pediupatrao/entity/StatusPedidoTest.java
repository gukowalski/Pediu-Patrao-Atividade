package com.umc.pediupatrao.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPedidoTest {

    @Test
    void fluxoCompletoDeEntrega() {
        assertTrue(StatusPedido.RECEBIDO.podeAvancarPara(StatusPedido.EM_PREPARACAO, TipoAtendimento.ENTREGA));
        assertTrue(StatusPedido.EM_PREPARACAO.podeAvancarPara(StatusPedido.PRONTO, TipoAtendimento.ENTREGA));
        assertTrue(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.SAIU_PARA_ENTREGA, TipoAtendimento.ENTREGA));
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.podeAvancarPara(StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA));
    }

    @Test
    void prontoParaRetiradaSoPodeIrParaRetirado() {
        assertTrue(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.RETIRADO, TipoAtendimento.RETIRADA));
        assertFalse(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.SAIU_PARA_ENTREGA, TipoAtendimento.RETIRADA));
    }

    @Test
    void naoPermiteFinalizarPedidoEmPreparacao() {
        assertFalse(StatusPedido.EM_PREPARACAO.podeAvancarPara(StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA));
    }

    @Test
    void cancelamentoNaoFazParteDoFluxoNormal() {
        assertFalse(StatusPedido.RECEBIDO.podeAvancarPara(StatusPedido.CANCELADO, TipoAtendimento.ENTREGA));
    }

    @Test
    void estadosFinaisNaoTemProximoStatus() {
        assertEquals(0, StatusPedido.FINALIZADO.proximosPermitidos(TipoAtendimento.ENTREGA).size());
        assertEquals(0, StatusPedido.CANCELADO.proximosPermitidos(TipoAtendimento.ENTREGA).size());
    }

    @Test
    void cancelamentoEDescontoSomenteAntesDaSaida() {
        assertTrue(StatusPedido.PRONTO.isAntesDaSaida());
        assertFalse(StatusPedido.SAIU_PARA_ENTREGA.isAntesDaSaida());
        assertFalse(StatusPedido.RETIRADO.isAntesDaSaida());
        assertFalse(StatusPedido.CANCELADO.isAntesDaSaida());
    }
}
