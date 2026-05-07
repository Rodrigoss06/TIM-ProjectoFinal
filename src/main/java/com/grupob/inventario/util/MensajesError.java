package com.grupob.inventario.util;

public final class MensajesError {
    public static final String CODIGO_DUPLICADO = "Error: El código de producto ya está registrado.";
    public static final String CAMPOS_OBLIGATORIOS = "Error: Complete todos los campos requeridos.";
    public static final String VALORES_NEGATIVOS = "Error: Los valores numéricos deben ser positivos.";
    public static final String PRODUCTO_NO_ENCONTRADO = "Error: Producto no encontrado.";
    public static final String CANTIDAD_NO_POSITIVA = "Error: La cantidad debe ser un número positivo.";
    public static final String STOCK_INSUFICIENTE_FMT = "Error: Stock insuficiente. Disponible: %d unidades.";
    public static final String CREDENCIALES_INVALIDAS = "Error: Credenciales inválidas.";
    public static final String CUENTA_BLOQUEADA_FMT = "Error: Cuenta bloqueada temporalmente. Intente en %d minutos.";
    public static final String SESION_EXPIRADA = "Sesión expirada. Inicie sesión nuevamente.";
    public static final String SIN_PERMISOS = "Error: No tiene permisos para esta función.";
    public static final String USUARIO_DUPLICADO = "Error: El nombre de usuario ya está en uso.";
    public static final String ULTIMO_ADMIN = "Error: Debe existir al menos un administrador activo.";
    public static final String PASSWORD_CORTA = "Error: La contraseña debe tener al menos 8 caracteres.";
    public static final String SIN_PRODUCTOS = "No hay productos en el inventario.";
    public static final String SIN_RESULTADOS_BUSQUEDA = "No se encontraron productos con el criterio ingresado.";
    public static final String SIN_RESULTADOS_FILTRO = "No se encontraron productos con los filtros aplicados.";
    public static final String MOVIMIENTOS_RECIENTES_ADVERTENCIA = "Advertencia: El producto tiene movimientos recientes. ¿Desea continuar?";
    // ── Etapa 2 — sección 9 de Notion ──────────────────────────────
    public static final String ERROR_CONEXION_BD = "Error: No se pudo conectar a la base de datos. Contacte al administrador.";
    public static final String ERROR_TRANSACCION = "Error: No se pudo completar la operación. Los cambios fueron revertidos.";
    public static final String SIN_REGISTROS_AUDITORIA = "No hay registros de auditoría con los filtros aplicados.";
    public static final String CONFIG_FALTANTE_FMT = "Error: Falta configuración requerida '%s'. Verifique application.properties o variables de entorno.";
    public static final String CONFIG_INVALIDA_FMT = "Error: Valor inválido para '%s': debe ser un entero positivo.";
    public static final String ERROR_INESPERADO = "Error inesperado. Si persiste, contacte al administrador.";
    public static final String INPUT_NUMERICO_INVALIDO = "Error: El valor ingresado no es un número válido.";
    public static final String FILTRO_RANGO_INVALIDO = "Error: El precio mínimo no puede ser mayor que el máximo.";
    private MensajesError() {}
}
