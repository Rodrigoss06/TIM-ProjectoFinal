package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.persistence.GestorTransacciones;
import com.grupob.inventario.repository.AuditoriaRepository;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.security.PermisoChecker;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public class AuditoriaService {

    private final AuditoriaRepository repo;
    private final PermisoChecker permisoChecker;
    private final Clock clock;
    private final int paginaTamano;
    private final GestorTransacciones txManager;

    public AuditoriaService(AuditoriaRepository repo, PermisoChecker permisoChecker,
                             Clock clock, int paginaTamano, GestorTransacciones txManager) {
        this.repo           = Objects.requireNonNull(repo);
        this.permisoChecker = Objects.requireNonNull(permisoChecker);
        this.clock          = Objects.requireNonNull(clock);
        this.paginaTamano   = paginaTamano;
        this.txManager      = Objects.requireNonNull(txManager);
    }

    /**
     * Registra un evento. No abre transacción propia — debe invocarse desde dentro
     * de la transacción del servicio que generó el evento (RF-INV-009).
     */
    public void registrar(String username, TipoEvento tipo, String entidad, String detalle) {
        repo.registrar(EventoAuditoria.crear(username, tipo, entidad, detalle, clock));
    }

    /** Solo admin puede consultar el log. Usa soloLectura para JPA. */
    public List<EventoAuditoria> consultar(FiltroAuditoria filtro, int pagina, Rol rolEjecutor) {
        permisoChecker.requierePermiso(rolEjecutor, PermisoChecker.Accion.CONSULTAR_AUDITORIA);
        Objects.requireNonNull(filtro);
        return txManager.soloLectura(() -> repo.consultar(filtro, pagina, paginaTamano));
    }
}
