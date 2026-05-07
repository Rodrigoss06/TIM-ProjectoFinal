package com.grupob.inventario.security;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public final class PasswordHasher {

    public String hashear(String plain) {
        Objects.requireNonNull(plain, "password no puede ser null");
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    public boolean verificar(String plain, String hash) {
        if (plain == null || hash == null) return false;
        return BCrypt.checkpw(plain, hash);
    }
}
