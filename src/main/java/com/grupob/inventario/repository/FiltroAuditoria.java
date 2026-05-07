package com.grupob.inventario.repository;

import com.grupob.inventario.domain.enums.TipoEvento;

import java.time.Instant;

/** Filtros opcionales (todos nullable) para consultar el log de auditoría. */
public record FiltroAuditoria(
        Instant desde,
        Instant hasta,
        String username,
        TipoEvento tipoEvento
) {}
