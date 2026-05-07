# Cobertura de Requerimientos — Módulo Inventario Grupo B

Mapeo de cada criterio de aceptación (sección 2) y superficie de ataque (sección 8) de las páginas Notion contra el test que lo cubre.

---

## ETAPA 1 — RF-INV-001 a 007

### RF-INV-001 — Registrar Producto

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Producto válido se registra | `ProductoServiceTest::registrar_productoValido_guardado` |
| No permite código duplicado | `ProductoServiceTest::registrar_codigoDuplicado_excepcion` |
| Campos obligatorios validados | `ProductoValidatorTest::validarRegistro_codigoNull_lanzaExcepcion` etc. |
| Stock inicial ≥ 0 | `ProductoServiceTest::registrar_stockCero_OK` |
| Precio positivo ≤ 2 decimales | `ProductoValidatorTest::validarRegistro_precioCero_lanzaExcepcion` |
| **8.1** Código duplicado case-insensitive | `ProductoServiceTest::registrar_codigoDuplicadoCaseInsensitive_excepcion` |
| **8.1** Precio = 0.00 rechaza | `ProductoServiceTest::registrar_precioCero_excepcion` |
| **8.1** Stock = 0 se permite | `ProductoServiceTest::registrar_stockCero_OK` |
| **8.1** Unicode / strings largos | `ProductoServiceTest::registrar_unicodeNombreCategoria_OK`, `::registrar_stringsLargos_OK` |

### RF-INV-002 — Buscar Producto

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Búsqueda por código exacta | `ProductoServiceTest::buscar_porCodigo_retornaProducto` |
| Coincidencias parciales en nombre | `ProductoServiceTest::buscar_porNombre_parcial` |
| Búsqueda por categoría | `ProductoServiceTest::buscar_porCategoria_retornaCategoria` |
| **8.2** Case-insensitive | `ProductoServiceTest::buscar_porNombre_caseInsensitive` |
| **8.2** Trim de espacios | `ProductoServiceTest::buscar_criterioConEspacios_trim` |
| **8.2** Eliminados no aparecen | `ProductoServiceTest::buscar_productoEliminado_noAparece` |

### RF-INV-003 — Actualizar Stock

| Criterio / Ataque | Test que lo cubre |
|---|---|
| ENTRADA incrementa stock | `InventarioServiceTest::actualizarStock_entrada_incrementaStock` |
| SALIDA decrementa stock | `InventarioServiceTest::actualizarStock_salida_decrementaStock` |
| Stock negativo imposible | `StockValidatorTest::validarMovimiento_salidaStockMasUno_lanzaExcepcion` |
| Movimiento registra metadatos | `InventarioServiceTest::actualizarStock_entrada_movimientoRegistrado` |
| **8.3** Salida = stock (deja en 0) | `InventarioServiceTest::actualizarStock_salidaIgualAlStock_permitido` |
| **8.3** Producto eliminado → mismo mensaje | `InventarioServiceTest::actualizarStock_productoEliminado_mismaMensajeQueNoEncontrado` |

### RF-INV-004 — Eliminar Producto

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Eliminado no aparece en búsquedas | `ProductoServiceTest::buscar_productoEliminado_noAparece` |
| Eliminación es lógica | `ProductoServiceTest::eliminar_admin_exitoso` |
| Historial conservado | `ProductoServiceTest::eliminar_movimientosConservados` |
| **8.4** Gestor intenta eliminar → SIN_PERMISOS | `ProductoServiceTest::eliminar_gestor_sinPermiso`, `FlujoCompletoIT::flujoCompletoRF001A009` |

### RF-INV-005 — Listar Productos

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Filtros combinados | `ProductoServiceTest::listar_filtrosCombinados` |
| stock=4 → BAJO | `ProductoServiceTest::listar_stock4_marcadoBAJO_frontera` |
| stock=5 → NO BAJO | `ProductoServiceTest::listar_stock5_esNORMAL_noBAJO_fronteraEstricta` |
| stock=0 → SIN_STOCK | `ProductoServiceTest::listar_stock0_esSinStock` |
| **8.5** Precio min > max → error | `ProductoServiceTest::listar_precioMinMayorQueMax_excepcion` |

### RF-INV-006 — Autenticación

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Login con credenciales válidas | `AutenticacionServiceTest::login_exitoso_retornaToken` |
| Mismo mensaje inexistente/contraseña mala | `AutenticacionServiceTest::login_mensajeIdentico_usuarioInexistenteYPasswordMal` |
| Bloqueo tras 3 intentos | `AutenticacionServiceTest::login_tresIntentosFallidos_terceroBloquea` |
| Bloqueo 15 minutos | `AutenticacionServiceTest::login_tresIntentosFallidos_terceroBloquea` |
| Sesión expira 30 min | `AutenticacionServiceTest::sesion_validaA29min_expiradaA31min` |
| BCrypt almacenado | `AutenticacionServiceTest::login_hashEnRepoNoEsTextoPlano` |
| **8.6** 4to intento correcto dentro 15 min → bloqueado | `AutenticacionServiceTest::login_4toIntentoCorrecto_dentroDe15min_bloqueado` |
| **8.6** Después 15 min → desbloqueado | `AutenticacionServiceTest::login_exactamente15min_desbloqueado` |

### RF-INV-007 — Gestión de Roles

| Criterio / Ataque | Test que lo cubre |
|---|---|
| Admin crea usuario | `UsuarioServiceTest::crearUsuario_admin_exitoso` |
| Admin cambia rol | `UsuarioServiceTest::cambiarRol_gestorAAdmin_efectoInmediato` |
| No desactivar único admin | `UsuarioServiceTest::desactivar_unicoAdmin_excepcion`, `FlujoCompletoTest::flujoUltimoAdmin` |
| Password ≥ 8 chars | `UsuarioServiceTest::crearUsuario_password7Chars_excepcion` |
| Password hasheada | `UsuarioServiceTest::crearUsuario_passwordHasheada_noTextoPlano` |
| Cambio rol invalida sesión | `UsuarioServiceTest::cambiarRol_invalidaSesionesDelUsuario` |

---

## ETAPA 2 — RF-INV-008 a 010

### RF-INV-008 — Persistencia Transaccional

| Criterio | Test que lo cubre |
|---|---|
| Falla en movimiento revierte stock | `TransaccionalidadIT::falla_movimiento_revierte_stock` |
| Falla en auditoría revierte producto | `TransaccionalidadIT::falla_auditoria_revierte_producto`, `TransaccionalidadEndToEndIT::falla_auditoria_revierte_producto` |
| Ningún dato parcial tras rollback | `TransaccionalidadIT::exito_persiste_todas_las_tablas` |
| Conteo producto = conteo CREAR_PRODUCTO | `TransaccionalidadEndToEndIT::consistencia_productos_vs_auditoria_exitosas` |
| Mezcla éxito+fallo siempre consistente | `TransaccionalidadEndToEndIT::consistencia_con_mezcla_exitos_y_fallos` |

### RF-INV-009 — Auditoría

| Criterio | Test que lo cubre |
|---|---|
| Cada op genera exactamente 1 evento | `FlujoCompletoIT::flujoCompletoRF001A009` (verifica 8+ eventos) |
| Evento dentro de la misma transacción | `TransaccionalidadIT::exito_persiste_todas_las_tablas` |
| Si op falla, no se persiste evento | `TransaccionalidadEndToEndIT::falla_auditoria_revierte_producto` |
| Solo admin consulta | `AutenticacionServiceTest::consultar_gestor_sinPermiso` (vía AuditoriaService) |
| LOGIN_FALLIDO con username=null | `AutenticacionServiceTest::login_usuarioInexistente_credencialesInvalidas`, `FlujoCompletoIT` |
| Filtros se combinan con AND | `AuditoriaRepositoryJpaIT::filtrosCombinados_AND`, `FlujoCompletoIT` |
| Orden cronológico descendente | `FlujoCompletoIT::flujoCompletoRF001A009` |

### RF-INV-010 — Configuración Externa

| Criterio | Test que lo cubre |
|---|---|
| Variables obligatorias con mensaje exacto | `ConfiguracionTest::faltaDbUrl_lanzaConfiguracionException` etc. |
| Env tiene prioridad sobre properties | `ConfiguracionTest::envDbUrl_sobreescribeProperties` |
| Valor inválido rechazado | `ConfiguracionTest::lockoutIntentosMaxCero_lanzaConfiguracionException` |
| `.env.example` existe | Verificable con `ls .env.example` |
| App falla con mensaje claro | `AppArranqueIT::cliArranque_loginAdmin_listar_logout` |
