package com.grupob.inventario.domain.model;

import com.grupob.inventario.domain.enums.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Convertido de record a clase para soportar @Entity de JPA.
 * Los accessores mantienen el estilo de record (sin "get") para compatibilidad
 * con los tests y el código existente de etapa 1.
 * Los movimientos NUNCA se modifican ni eliminan (append-only).
 */
@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "codigo_producto", nullable = false, length = 50)
    private String codigoProducto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private int stockAnterior;

    @Column(name = "stock_nuevo", nullable = false)
    private int stockNuevo;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant fecha;

    /** Constructor requerido por JPA. No usar directamente. */
    protected Movimiento() {}

    public Movimiento(String codigoProducto, TipoMovimiento tipo, int cantidad,
                      int stockAnterior, int stockNuevo, Instant fecha) {
        Objects.requireNonNull(codigoProducto, "codigoProducto no puede ser null");
        Objects.requireNonNull(tipo,           "tipo no puede ser null");
        Objects.requireNonNull(fecha,          "fecha no puede ser null");
        this.id             = UUID.randomUUID();
        this.codigoProducto = codigoProducto;
        this.tipo           = tipo;
        this.cantidad       = cantidad;
        this.stockAnterior  = stockAnterior;
        this.stockNuevo     = stockNuevo;
        this.fecha          = fecha;
    }

    // Accessores estilo record para compatibilidad con código y tests de etapa 1
    public UUID id()               { return id; }
    public String codigoProducto() { return codigoProducto; }
    public TipoMovimiento tipo()   { return tipo; }
    public int cantidad()          { return cantidad; }
    public int stockAnterior()     { return stockAnterior; }
    public int stockNuevo()        { return stockNuevo; }
    public Instant fecha()         { return fecha; }

    // Getters estilo JavaBean para JPA y código nuevo
    public UUID getId()               { return id; }
    public String getCodigoProducto() { return codigoProducto; }
    public TipoMovimiento getTipo()   { return tipo; }
    public int getCantidad()          { return cantidad; }
    public int getStockAnterior()     { return stockAnterior; }
    public int getStockNuevo()        { return stockNuevo; }
    public Instant getFecha()         { return fecha; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movimiento)) return false;
        return Objects.equals(id, ((Movimiento) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Movimiento{id=" + id + ", codigo='" + codigoProducto
                + "', tipo=" + tipo + ", cantidad=" + cantidad + '}';
    }
}
