package com.grupob.inventario.domain.model;

import com.grupob.inventario.domain.exception.StockInsuficienteException;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @Column(length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean activo;

    /** Constructor requerido por JPA. No usar directamente. */
    protected Producto() {}

    public Producto(String codigo, String nombre, String descripcion, String categoria,
                    BigDecimal precioUnitario, int stock) {
        if (esBlancoONulo(codigo) || esBlancoONulo(nombre) || esBlancoONulo(categoria)
                || precioUnitario == null) {
            throw new ValidacionException(MensajesError.CAMPOS_OBLIGATORIOS);
        }
        if (precioUnitario.compareTo(BigDecimal.ZERO) <= 0 || stock < 0
                || precioUnitario.scale() > 2) {
            throw new ValidacionException(MensajesError.VALORES_NEGATIVOS);
        }
        // Códigos son case-insensitive (sección 8.1 Notion): se normalizan a mayúsculas
        this.codigo        = codigo.trim().toUpperCase();
        this.nombre        = nombre.trim();
        this.descripcion   = descripcion != null ? descripcion.trim() : "";
        this.categoria     = categoria.trim();
        this.precioUnitario = precioUnitario;
        this.stock         = stock;
        this.activo        = true;
    }

    private static boolean esBlancoONulo(String s) {
        return s == null || s.isBlank();
    }

    public void incrementarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionException(MensajesError.CANTIDAD_NO_POSITIVA);
        }
        this.stock += cantidad;
    }

    public void decrementarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionException(MensajesError.CANTIDAD_NO_POSITIVA);
        }
        if (cantidad > this.stock) {
            throw new StockInsuficienteException(this.stock);
        }
        this.stock -= cantidad;
    }

    public void eliminar() {
        this.activo = false;
    }

    public String getCodigo()          { return codigo; }
    public String getNombre()          { return nombre; }
    public void setNombre(String n)    { this.nombre = n; }
    public String getDescripcion()     { return descripcion; }
    public void setDescripcion(String d){ this.descripcion = d != null ? d : ""; }
    public String getCategoria()       { return categoria; }
    public void setCategoria(String c) { this.categoria = c; }
    public BigDecimal getPrecioUnitario()          { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal p)    { this.precioUnitario = p; }
    public int getStock()              { return stock; }
    public boolean isActivo()          { return activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto)) return false;
        return Objects.equals(codigo, ((Producto) o).codigo);
    }

    @Override
    public int hashCode() { return Objects.hash(codigo); }

    @Override
    public String toString() {
        return "Producto{codigo='" + codigo + "', nombre='" + nombre + "', categoria='" + categoria
                + "', precio=" + precioUnitario + ", stock=" + stock + ", activo=" + activo + '}';
    }
}
