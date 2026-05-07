package com.grupob.inventario.domain.model;

import com.grupob.inventario.domain.enums.TipoEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoría inmutable (append-only). RF-INV-009 Notion etapa 2.
 * Sin setters — la clase solo se puede crear con el factory estático.
 */
@Entity
@Table(name = "auditoria")
public class EventoAuditoria {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant fecha;

    @Column(length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEvento tipoEvento;

    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    @Column(length = 500)
    private String detalle;

    protected EventoAuditoria() {}

    private EventoAuditoria(UUID id, Instant fecha, String username, TipoEvento tipoEvento,
                             String entidadAfectada, String detalle) {
        this.id              = id;
        this.fecha           = fecha;
        this.username        = username;
        this.tipoEvento      = tipoEvento;
        this.entidadAfectada = entidadAfectada;
        this.detalle         = detalle;
    }

    public static EventoAuditoria crear(String username, TipoEvento tipo,
                                         String entidadAfectada, String detalle, Clock clock) {
        return new EventoAuditoria(
                UUID.randomUUID(),
                Instant.now(clock),
                username,
                tipo,
                entidadAfectada,
                detalle);
    }

    public UUID getId()               { return id; }
    public Instant getFecha()         { return fecha; }
    public String getUsername()       { return username; }
    public TipoEvento getTipoEvento() { return tipoEvento; }
    public String getEntidadAfectada(){ return entidadAfectada; }
    public String getDetalle()        { return detalle; }
}
