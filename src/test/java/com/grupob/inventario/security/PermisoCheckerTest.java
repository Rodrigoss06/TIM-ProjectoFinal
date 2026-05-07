package com.grupob.inventario.security;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PermisoChecker — matriz completa rol × acción (6.6 Notion)")
class PermisoCheckerTest {

    private PermisoChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PermisoChecker();
    }

    // ── matriz completa: 12 casos (6 acciones × 2 roles) ─────────────────────

    @ParameterizedTest(name = "{0} → {1}: esperado={2}")
    @MethodSource("matrizDePermisos")
    @DisplayName("Matriz completa rol × acción")
    void puede_matrizCompleta(Rol rol, PermisoChecker.Accion accion, boolean esperado) {
        assertThat(checker.puede(rol, accion)).isEqualTo(esperado);
    }

    static Stream<Arguments> matrizDePermisos() {
        return Stream.of(
                // ADMINISTRADOR puede todo (6 acciones)
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.REGISTRAR_PRODUCTO,  true),
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.BUSCAR_PRODUCTO,     true),
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.ACTUALIZAR_STOCK,    true),
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.ELIMINAR_PRODUCTO,   true),
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.LISTAR_PRODUCTO,     true),
                Arguments.of(Rol.ADMINISTRADOR, PermisoChecker.Accion.GESTIONAR_USUARIOS,  true),
                // GESTOR puede 4, NO puede ELIMINAR_PRODUCTO ni GESTIONAR_USUARIOS
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.REGISTRAR_PRODUCTO, true),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.BUSCAR_PRODUCTO,    true),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.ACTUALIZAR_STOCK,   true),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.ELIMINAR_PRODUCTO,  false),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.LISTAR_PRODUCTO,    true),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.GESTIONAR_USUARIOS, false),
                // CONSULTAR_AUDITORIA: solo admin
                Arguments.of(Rol.ADMINISTRADOR,    PermisoChecker.Accion.CONSULTAR_AUDITORIA,  true),
                Arguments.of(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.CONSULTAR_AUDITORIA, false)
        );
    }

    // ── requierePermiso ───────────────────────────────────────────────────────

    @Test
    @DisplayName("requierePermiso no lanza excepción si el rol puede hacer la acción")
    void requierePermiso_permitido_sinExcepcion() {
        checker.requierePermiso(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.REGISTRAR_PRODUCTO);
    }

    @Test
    @DisplayName("requierePermiso lanza PermisoDenegadoException si el rol NO puede (RF-INV-007)")
    void requierePermiso_denegado_lanzaExcepcion() {
        assertThatThrownBy(() ->
                checker.requierePermiso(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.ELIMINAR_PRODUCTO))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    @Test
    @DisplayName("requierePermiso GESTOR intenta GESTIONAR_USUARIOS → PermisoDenegadoException")
    void requierePermiso_gestorGestionaUsuarios_denegado() {
        assertThatThrownBy(() ->
                checker.requierePermiso(Rol.GESTOR_INVENTARIO, PermisoChecker.Accion.GESTIONAR_USUARIOS))
                .isInstanceOf(PermisoDenegadoException.class)
                .hasMessage(MensajesError.SIN_PERMISOS);
    }

    // ── defensiva: null ───────────────────────────────────────────────────────

    @Test
    @DisplayName("puede(null, accion) devuelve false sin lanzar excepción")
    void puede_rolNull_false() {
        assertThat(checker.puede(null, PermisoChecker.Accion.REGISTRAR_PRODUCTO)).isFalse();
    }

    @Test
    @DisplayName("puede(rol, null) devuelve false sin lanzar excepción")
    void puede_accionNull_false() {
        assertThat(checker.puede(Rol.ADMINISTRADOR, null)).isFalse();
    }
}
