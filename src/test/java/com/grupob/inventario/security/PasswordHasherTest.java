package com.grupob.inventario.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordHasher — BCrypt hash y verificación (6.7 Notion)")
class PasswordHasherTest {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
    }

    @Test
    @DisplayName("el hash generado es distinto al password original (no texto plano)")
    void hashear_hashDistintoAlInput() {
        String hash = hasher.hashear("miPassword1");
        assertThat(hash).isNotEqualTo("miPassword1");
    }

    @Test
    @DisplayName("dos hashes del mismo password son distintos (salt aleatorio funciona)")
    void hashear_mismoPasword_hashesDistintos() {
        String hash1 = hasher.hashear("mismaPassword");
        String hash2 = hasher.hashear("mismaPassword");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("verificar devuelve true para el password correcto")
    void verificar_passwordCorrecto_true() {
        String hash = hasher.hashear("admin123!");
        assertThat(hasher.verificar("admin123!", hash)).isTrue();
    }

    @Test
    @DisplayName("verificar devuelve false para password incorrecto")
    void verificar_passwordIncorrecto_false() {
        String hash = hasher.hashear("admin123!");
        assertThat(hasher.verificar("otraPassword", hash)).isFalse();
    }

    @Test
    @DisplayName("verificar devuelve false si plain es null")
    void verificar_plainNull_false() {
        String hash = hasher.hashear("admin123!");
        assertThat(hasher.verificar(null, hash)).isFalse();
    }

    @Test
    @DisplayName("verificar devuelve false si hash es null")
    void verificar_hashNull_false() {
        assertThat(hasher.verificar("admin123!", null)).isFalse();
    }

    @Test
    @DisplayName("el hash tiene el prefijo BCrypt esperado ($2a$)")
    void hashear_prefijoBCrypt() {
        String hash = hasher.hashear("cualquierPassword1");
        assertThat(hash).startsWith("$2a$");
    }
}
