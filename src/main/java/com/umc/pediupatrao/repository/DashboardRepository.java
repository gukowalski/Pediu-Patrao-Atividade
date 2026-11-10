package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.dto.dashboard.DashboardFiltro;
import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de leitura do dashboard. Os filtros (período, tipo de atendimento, status e categoria)
 * são aplicados no próprio MongoDB, usando o índice de dataHoraEntrada criado no MongoInitConfig.
 */
@Repository
public class DashboardRepository {

    private final MongoTemplate mongoTemplate;

    public DashboardRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * @param produtosDaCategoria ids dos produtos da categoria filtrada; cobre pedidos antigos
     *                            cujos itens não guardaram a categoria
     */
    public List<Pedido> buscarPedidos(DashboardFiltro filtro, Collection<String> produtosDaCategoria) {
        Criteria criteria = Criteria.where("dataHoraEntrada").gte(filtro.inicio()).lt(filtro.fimExclusivo());

        if (filtro.tipoAtendimento() != null) {
            criteria.and("tipoAtendimento").is(filtro.tipoAtendimento().name());
        }
        if (filtro.status() != null) {
            criteria.and("status").is(filtro.status().name());
        }
        if (filtro.categoria() != null) {
            criteria.orOperator(
                    Criteria.where("itens.categoria").is(filtro.categoria()),
                    Criteria.where("itens.produtoId").in(produtosDaCategoria));
        }

        Query query = new Query(criteria).with(Sort.by(Sort.Direction.ASC, "dataHoraEntrada"));
        return mongoTemplate.find(query, Pedido.class);
    }

    public Optional<LocalDateTime> dataPrimeiroPedido() {
        return dataExtrema(Sort.Direction.ASC);
    }

    public Optional<LocalDateTime> dataUltimoPedido() {
        return dataExtrema(Sort.Direction.DESC);
    }

    private Optional<LocalDateTime> dataExtrema(Sort.Direction direcao) {
        Query query = new Query(Criteria.where("dataHoraEntrada").ne(null))
                .with(Sort.by(direcao, "dataHoraEntrada"))
                .limit(1);
        query.fields().include("dataHoraEntrada");
        return Optional.ofNullable(mongoTemplate.findOne(query, Pedido.class)).map(Pedido::getDataHoraEntrada);
    }
}
