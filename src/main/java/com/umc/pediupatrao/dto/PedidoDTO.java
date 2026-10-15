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
    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;
    private String canceladoPor;
    private LocalDateTime dataHoraCancelamento;
    private String justificativaCancelamento;

    public PedidoDTO() {

    }

    public PedidoDTO(String id, String clienteId, TipoAtendimento tipoAtendimento, List<ItemPedidoDTO> itens,
                     String observacao, BigDecimal subtotal, BigDecimal descontoPercentual,
                     BigDecimal valorDesconto, BigDecimal valorTotal, StatusPedido status,
                     String registradoPor, LocalDateTime dataHoraEntrada,
                     String responsavelSaida, LocalDateTime dataHoraSaida, String canceladoPor,
                     LocalDateTime dataHoraCancelamento, String justificativaCancelamento) {
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
        this.responsavelSaida = responsavelSaida;
        this.dataHoraSaida = dataHoraSaida;
        this.canceladoPor = canceladoPor;
        this.dataHoraCancelamento = dataHoraCancelamento;
        this.justificativaCancelamento = justificativaCancelamento;
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

    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public LocalDateTime getDataHoraCancelamento() {
        return dataHoraCancelamento;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }
}
