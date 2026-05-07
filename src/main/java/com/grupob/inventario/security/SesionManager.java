package com.grupob.inventario.security;

import com.grupob.inventario.domain.model.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SesionManager {

    private static final Duration DURACION_SESION = Duration.ofMinutes(30);

    private static record InfoSesion(String username, Instant ultimaActividad) {}

    private final Clock clock;
    private final Map<String, InfoSesion> sesiones = new ConcurrentHashMap<>();

    public SesionManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public String iniciarSesion(Usuario usuario) {
        Objects.requireNonNull(usuario);
        String token = UUID.randomUUID().toString();
        sesiones.put(token, new InfoSesion(usuario.getUsername(), Instant.now(clock)));
        return token;
    }

    public Optional<String> usernameDeSesion(String token) {
        if (token == null) return Optional.empty();
        InfoSesion info = sesiones.get(token);
        if (info == null) return Optional.empty();
        if (estaExpirada(info)) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(info.username());
    }

    public void tocarSesion(String token) {
        if (token == null) return;
        sesiones.computeIfPresent(token, (k, info) ->
                new InfoSesion(info.username(), Instant.now(clock)));
    }

    public void cerrarSesion(String token) {
        if (token == null) return;
        sesiones.remove(token);
    }

    public void invalidarSesionesDe(String username) {
        Objects.requireNonNull(username);
        sesiones.entrySet().removeIf(e -> e.getValue().username().equals(username));
    }

    private boolean estaExpirada(InfoSesion info) {
        return !Instant.now(clock).isBefore(info.ultimaActividad().plus(DURACION_SESION));
    }
}
