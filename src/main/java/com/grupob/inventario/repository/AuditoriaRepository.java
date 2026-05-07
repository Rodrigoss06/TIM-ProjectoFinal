package com.grupob.inventario.repository;

import com.grupob.inventario.domain.model.EventoAuditoria;

import java.util.List;

public interface AuditoriaRepository {

    /** Persiste un evento. Llamar dentro de una transacción activa. */
    void registrar(EventoAuditoria evento);

    /**
     * Consulta eventos con filtros opcionales, ordenados fecha DESC.
     * @param filtro  campos nullable; se combinan con AND
     * @param pagina  basado en 0
     * @param tamanoPagina número máximo de resultados
     */
    List<EventoAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamanoPagina);
}
