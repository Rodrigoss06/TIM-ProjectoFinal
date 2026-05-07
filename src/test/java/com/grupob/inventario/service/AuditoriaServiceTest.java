package com.grupob.inventario.service;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.enums.TipoEvento;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.persistence.TransactionManagerFake;
import com.grupob.inventario.repository.FiltroAuditoria;
import com.grupob.inventario.repository.memory.AuditoriaRepositoryEnMemoria;
import com.grupob.inventario.security.PermisoChecker;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditoriaService — registro, consulta y permisos (RF-INV-009)")
class AuditoriaServiceTest {

    private AuditoriaRepositoryEnMemoria repo;
    private AuditoriaService service;
    private static final Instant T0 = Instant.parse("2026-05-01T10:00:00Z");
    private static final Clock RELOJ = Clock.fixed(T0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repo = new AuditoriaRepositoryEnMemoria();
        service = new AuditoriaService(repo, new PermisoChecker(), RELOJ, 50, new TransactionManagerFake());
    }

    // ── registrar ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar crea evento con los campos correctos")
    void registrar_creaEventoCorrecto() {
        service.registrar("admin", TipoEvento.CREAR_PRODUCTO, "PROD001", "alta inicial");

        List<EventoAuditoria> todos = repo.consultar(new FiltroAuditoria(null, null, null, null), 0, 10);
        assertThat(todos).hasSize(1);
        EventoAuditoria e = todos.get(0);
        assertThat(e.getUsername()).isEqualTo("admin");
        assertThat(e.getTipoEvento()).isEqualTo(TipoEvento.CREAR_PRODUCTO);
        assertThat(e.getEntidadAfectada()).isEqualTo("PROD001");
        assertThat(e.getDetalle()).isEqualTo("alta inicial");
        assertThat(e.getFecha()).isEqualTo(T0);
    }

    @Test
    @DisplayName("registrar con username null se permite (LOGIN_FALLIDO con usuario inexistente)")
    void registrar_usernameNulo_sePermite() {
        service.registrar(null, TipoEvento.LOGIN_FALLIDO, null, "usuario no encontrado");

        List<EventoAuditoria> todos = repo.consultar(new FiltroAuditoria(null, null, null, null), 0, 10);
        assertThat(todos).hasSize(1)
                .extracting(EventoAuditoria::getUsername).containsNull();
    }

    // ── consultar: permisos ───────────────────────────────────────────

    @Test
    @DisplayName("admin puede consultar el log de auditoría")
    void consultar_admin_puede() {
        service.registrar("admin", TipoEvento.LOGIN_EXITOSO, null, null);
        List<EventoAuditoria> result = service.consultar(
                new FiltroAuditoria(null, null, null, null), 0, Rol.ADMINISTRADOR);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("gestor NO puede consultar auditoría → PermisoDenegadoException con SIN_PERMISOS")
    void consultar_gestor_sinPermiso() {
        assertThatThrownBy(() -> service.consultar(
                new FiltroAuditoria(null, null, null, null), 0, Rol.GESTOR_INVENTARIO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    // ── consultar: filtros ────────────────────────────────────────────

    @Test
    @DisplayName("filtro por tipoEvento funciona a nivel servicio")
    void consultar_filtroTipoEvento() {
        service.registrar("admin", TipoEvento.LOGIN_EXITOSO,   null, null);
        service.registrar("admin", TipoEvento.CREAR_PRODUCTO,  "P1", "alta");
        service.registrar("admin", TipoEvento.ACTUALIZAR_STOCK,"P1", "10→20");

        List<EventoAuditoria> result = service.consultar(
                new FiltroAuditoria(null, null, null, TipoEvento.CREAR_PRODUCTO), 0, Rol.ADMINISTRADOR);
        assertThat(result).hasSize(1)
                .extracting(EventoAuditoria::getTipoEvento)
                .containsOnly(TipoEvento.CREAR_PRODUCTO);
    }

    @Test
    @DisplayName("filtro por username funciona a nivel servicio")
    void consultar_filtroUsername() {
        service.registrar("admin",  TipoEvento.LOGIN_EXITOSO, null, null);
        service.registrar("gestor", TipoEvento.CREAR_PRODUCTO, "P1", "alta");

        List<EventoAuditoria> result = service.consultar(
                new FiltroAuditoria(null, null, "admin", null), 0, Rol.ADMINISTRADOR);
        assertThat(result).hasSize(1)
                .extracting(EventoAuditoria::getUsername).containsOnly("admin");
    }

    @Test
    @DisplayName("sin resultados con filtros estrictos → lista vacía, no excepción")
    void consultar_sinResultados_listaVacia() {
        assertThat(service.consultar(
                new FiltroAuditoria(null, null, "nadie", null), 0, Rol.ADMINISTRADOR))
                .isEmpty();
    }
}
