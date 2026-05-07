package com.grupob.inventario.security;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.exception.PermisoDenegadoException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PermisoChecker {

    public enum Accion {
        REGISTRAR_PRODUCTO,
        BUSCAR_PRODUCTO,
        ACTUALIZAR_STOCK,
        ELIMINAR_PRODUCTO,
        LISTAR_PRODUCTO,
        GESTIONAR_USUARIOS,
        CONSULTAR_AUDITORIA
    }

    private static final Map<Rol, Set<Accion>> PERMISOS = new EnumMap<>(Rol.class);

    static {
        PERMISOS.put(Rol.ADMINISTRADOR, EnumSet.allOf(Accion.class));
        PERMISOS.put(Rol.GESTOR_INVENTARIO, EnumSet.of(
                Accion.REGISTRAR_PRODUCTO,
                Accion.BUSCAR_PRODUCTO,
                Accion.ACTUALIZAR_STOCK,
                Accion.LISTAR_PRODUCTO
        ));
    }

    public boolean puede(Rol rol, Accion accion) {
        if (rol == null || accion == null) return false;
        return PERMISOS.getOrDefault(rol, EnumSet.noneOf(Accion.class)).contains(accion);
    }

    public void requierePermiso(Rol rol, Accion accion) {
        if (!puede(rol, accion)) {
            throw new PermisoDenegadoException();
        }
    }
}
