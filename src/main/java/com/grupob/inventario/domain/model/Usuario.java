package com.grupob.inventario.domain.model;

import com.grupob.inventario.domain.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta", columnDefinition = "TIMESTAMPTZ")
    private Instant bloqueadoHasta;

    /** Constructor requerido por JPA. No usar directamente. */
    protected Usuario() {}

    public Usuario(String username, String passwordHash, Rol rol) {
        Objects.requireNonNull(username,     "username no puede ser null");
        Objects.requireNonNull(passwordHash, "passwordHash no puede ser null");
        Objects.requireNonNull(rol,          "rol no puede ser null");
        this.username         = username;
        this.passwordHash     = passwordHash;
        this.rol              = rol;
        this.activo           = true;
        this.intentosFallidos = 0;
        this.bloqueadoHasta   = null;
    }

    public void registrarIntentoFallido(Clock clock) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 3) {
            bloquearPor(Duration.ofMinutes(15), clock);
        }
    }

    public void resetIntentos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta   = null;
    }

    public void bloquearPor(Duration duracion, Clock clock) {
        this.bloqueadoHasta = Instant.now(clock).plus(duracion);
    }

    public boolean estaBloqueado(Clock clock) {
        return bloqueadoHasta != null && Instant.now(clock).isBefore(bloqueadoHasta);
    }

    public String getUsername()           { return username; }
    public String getPasswordHash()       { return passwordHash; }
    public Rol getRol()                   { return rol; }
    public void setRol(Rol rol)           { this.rol = Objects.requireNonNull(rol); }
    public boolean isActivo()             { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getIntentosFallidos()      { return intentosFallidos; }
    public Instant getBloqueadoHasta()    { return bloqueadoHasta; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        return Objects.equals(username, ((Usuario) o).username);
    }

    @Override
    public int hashCode() { return Objects.hash(username); }

    @Override
    public String toString() {
        return "Usuario{username='" + username + "', rol=" + rol
                + ", activo=" + activo + ", intentosFallidos=" + intentosFallidos + '}';
    }
}
