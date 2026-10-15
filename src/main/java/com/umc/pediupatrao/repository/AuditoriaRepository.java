package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditoriaRepository extends Repository<Auditoria, String> {

    Auditoria save(Auditoria auditoria);

    List<Auditoria> findAllByOrderByDataHoraDesc();
}
