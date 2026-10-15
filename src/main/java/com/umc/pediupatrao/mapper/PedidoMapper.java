package com.umc.pediupatrao.mapper;

import com.umc.pediupatrao.dto.InsertPedidoDTO;
import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.dto.PedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;

public class PedidoMapper {

    // Copia apenas os dados informados pelo usuário; itens e valores são montados no PedidoService
    public static Pedido toEntity(InsertPedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setTipoAtendimento(dto.getTipoAtendimento());
        pedido.setObservacao(dto.getObservacao());
        return pedido;
    }

    public static PedidoDTO toDto(Pedido pedido) {
        return new PedidoDTO(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getTipoAtendimento(),
                pedido.getItens().stream().map(PedidoMapper::itemToDto).toList(),
                pedido.getObservacao(),
                pedido.getSubtotal(),
                pedido.getDescontoPercentual(),
                pedido.getValorDesconto(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getRegistradoPor(),
                pedido.getDataHoraEntrada(),
                pedido.getResponsavelSaida(),
                pedido.getDataHoraSaida(),
                pedido.getCanceladoPor(),
                pedido.getDataHoraCancelamento(),
                pedido.getJustificativaCancelamento());
    }

    public static ItemPedidoDTO itemToDto(ItemPedido item) {
        return new ItemPedidoDTO(
                item.getProdutoId(),
                item.getNomeProduto(),
                item.getPrecoUnitario(),
                item.getQuantidade(),
                item.getSubtotal());
    }
}
