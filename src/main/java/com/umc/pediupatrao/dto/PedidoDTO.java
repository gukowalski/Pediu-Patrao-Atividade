package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PedidoDTO {
    private String id;
    private String clienteId;
    private TipoAtendimento tipoAtendimento;
    private List<ItemPedidoDTO> itens;
    private String observacao;
    private BigDecimal subtotal;
    private BigDecimal descontoPercentual;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;
    private StatusPedido status;
    private String registradoPor;
    private LocalDateTime dataHoraEntrada;

    public PedidoDTO() {

    }

    public PedidoDTO(String id, String clienteId, TipoAtendimento tipoAtendimento, List<ItemPedidoDTO> itens,
                     String observacao, BigDecimal subtotal, BigDecimal descontoPercentual,
                     BigDecimal valorDesconto, BigDecimal valorTotal, StatusPedido status,
                     String registradoPor, LocalDateTime dataHoraEntrada) {
        this.id = id;
        this.clienteId = clienteId;
        this.tipoAtendimento = tipoAtendimento;
        this.itens = itens;
        this.observacao = observacao;
        this.subtotal = subtotal;
        this.descontoPercentual = descontoPercentual;
        this.valorDesconto = valorDesconto;
        this.valorTotal = valorTotal;
        this.status = status;
        this.registradoPor = registradoPor;
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public List<ItemPedidoDTO> getItens() {
        return itens;
    }

    public String getObservacao() {
        return observacao;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescontoPercentual() {
        return descontoPercentual;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }
}
