package com.grupob.inventario.validation;

import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;

public final class PasswordValidator {

    public void validar(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidacionException(MensajesError.PASSWORD_CORTA);
        }
    }
}
