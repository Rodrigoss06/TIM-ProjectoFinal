package com.grupob.inventario.validation;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsuarioValidator — validación de creación de usuarios")
class UsuarioValidatorTest {

    private UsuarioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UsuarioValidator();
    }

    @Test
    @DisplayName("datos válidos (admin) no lanzan excepción")
    void validarCreacion_datosValidos_sinExcepcion() {
        assertThatCode(() -> validator.validarCreacion("juan", "password1", Rol.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("username null → CAMPOS_OBLIGATORIOS")
    void validarCreacion_usernameNull_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarCreacion(null, "password1", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("username en blanco → CAMPOS_OBLIGATORIOS")
    void validarCreacion_usernameBlancos_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarCreacion("   ", "password1", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }

    @Test
    @DisplayName("password de 7 chars → PASSWORD_CORTA")
    void validarCreacion_passwordCorta_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarCreacion("juan", "1234567", Rol.GESTOR_INVENTARIO))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.PASSWORD_CORTA);
    }

    @Test
    @DisplayName("password de 8 chars → válido")
    void validarCreacion_passwordLimiteInferior_sinExcepcion() {
        assertThatCode(() -> validator.validarCreacion("juan", "12345678", Rol.GESTOR_INVENTARIO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rol null → CAMPOS_OBLIGATORIOS")
    void validarCreacion_rolNull_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validarCreacion("juan", "password1", null))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.CAMPOS_OBLIGATORIOS);
    }
}
