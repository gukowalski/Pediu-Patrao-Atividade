package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoAtendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

// Só o que o atendente informa. Preços, total, status, responsável e data/hora
// não existem aqui: são definidos pelo servidor e ignorados se vierem no JSON.
public class InsertPedidoDTO {

    @NotBlank(message = "O cliente é obrigatório")
    private String clienteId;
    @NotNull(message = "O tipo de atendimento é obrigatório (ENTREGA ou RETIRADA)")
    private TipoAtendimento tipoAtendimento;
    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    private List<@NotNull(message = "Item inválido") @Valid InsertItemPedidoDTO> itens = new ArrayList<>();
    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    private String observacao;

    public InsertPedidoDTO() {

    }

    public InsertPedidoDTO(String clienteId, TipoAtendimento tipoAtendimento,
                           List<InsertItemPedidoDTO> itens, String observacao) {
        this.clienteId = clienteId;
        this.tipoAtendimento = tipoAtendimento;
        this.itens = itens;
        this.observacao = observacao;
    }

    public String getClienteId() {
        return clienteId;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public List<InsertItemPedidoDTO> getItens() {
        return itens;
    }

    public String getObservacao() {
        return observacao;
    }
}
