package com.grupob.inventario.validation;

import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordValidator — equivalencia y valores límite (7.2 Notion: 7 chars vs 8 chars)")
class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    // ── partición inválida ────────────────────────────────────────────────────

    @Test
    @DisplayName("password null → PASSWORD_CORTA")
    void validar_null_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validar(null))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.PASSWORD_CORTA);
    }

    @Test
    @DisplayName("password vacía → PASSWORD_CORTA")
    void validar_vacio_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validar(""))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.PASSWORD_CORTA);
    }

    @Test
    @DisplayName("password de 7 chars → PASSWORD_CORTA (límite inferior rechazado)")
    void validar_sieteCars_lanzaExcepcion() {
        assertThatThrownBy(() -> validator.validar("abcdefg"))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(MensajesError.PASSWORD_CORTA);
    }

    // ── partición válida ──────────────────────────────────────────────────────

    @Test
    @DisplayName("password de 8 chars → válido (límite inferior aceptado)")
    void validar_ochoCars_sinExcepcion() {
        assertThatCode(() -> validator.validar("abcdefgh"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("password de 9 chars → válido")
    void validar_nueveCars_sinExcepcion() {
        assertThatCode(() -> validator.validar("abcdefghi"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("password con caracteres especiales y espacios → válido si ≥ 8 chars")
    void validar_caracteresEspeciales_sinExcepcion() {
        assertThatCode(() -> validator.validar("admin123!"))
                .doesNotThrowAnyException();
    }
}
