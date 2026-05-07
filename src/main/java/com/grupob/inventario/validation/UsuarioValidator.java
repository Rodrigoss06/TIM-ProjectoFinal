package com.grupob.inventario.validation;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.ValidacionException;
import com.grupob.inventario.util.MensajesError;

public final class UsuarioValidator {

    private final PasswordValidator passwordValidator = new PasswordValidator();

    public void validarCreacion(String username, String password, Rol rol) {
        if (username == null || username.isBlank()) {
            throw new ValidacionException(MensajesError.CAMPOS_OBLIGATORIOS);
        }
        passwordValidator.validar(password);
        if (rol == null) {
            throw new ValidacionException(MensajesError.CAMPOS_OBLIGATORIOS);
        }
    }
}
