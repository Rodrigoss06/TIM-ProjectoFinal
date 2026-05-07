# Checklist de Smoke Manual — Sección 12 de Notion Etapa 2

Ejecutar con: `docker compose up -d && mvn javafx:run`

## 12.1 Persistencia

- [ ] **Producto persiste entre reinicios**: crear producto → apagar app → reiniciar → sigue existiendo en tabla
- [ ] **Rollback en falla**: matar app mid-operación → al reiniciar el producto NO existe (transacción no commiteada)
- [ ] **Apagar Postgres durante uso**: mensaje `Error: No se pudo conectar a la base de datos.` sin stacktrace al usuario
- [ ] **HikariCP recupera**: reiniciar Postgres → la app se reconecta automáticamente sin reiniciarla

## 12.2 Migraciones

- [ ] **BD limpia**: `docker compose down -v && docker compose up -d && mvn javafx:run` → Flyway crea todo desde cero, app arranca con seed demo de 10 productos
- [ ] **BD parcial**: aplicar solo V1 manualmente, arrancar → Flyway aplica V2..V5 y el sistema funciona

## 12.3 Configuración

- [ ] **Sin application.properties**: renombrar el archivo, intentar arrancar → mensaje exacto con la variable que falta
- [ ] **DB_URL inválida**: setear `DB_URL=jdbc:postgresql://noexiste/db` → mensaje de conexión claro, no NPE
- [ ] **lockout.intentos.max=0**: agregar a application.properties → app rechaza al arrancar con `Valor inválido para 'lockout.intentos.max'`

## 12.4 Auditoría

- [ ] **CREAR_PRODUCTO**: registrar producto → consultar auditoría → aparece evento con username del actor
- [ ] **LOGIN_FALLIDO usuario inexistente**: intentar login con usuario que no existe → en auditoría hay evento con `username = null`
- [ ] **Gestor no consulta auditoría**: login gestor → intentar consultar → mensaje `Error: No tiene permisos para esta función.`
- [ ] **No hay edición/borrado de auditoría**: verificar que no hay menú/endpoint para modificar registros de auditoría
- [ ] **Filtros combinados**: filtrar por rango fechas + tipo CREAR_PRODUCTO + usuario admin → solo eventos que cumplen todo
- [ ] **Orden descendente**: los eventos más recientes aparecen primero en la tabla

## 12.5 UI JavaFX

- [ ] **Abrir/cerrar rápido**: abrir y cerrar la app varias veces → no quedan conexiones colgadas
- [ ] **Click rápido x10 en Nuevo**: clicks rápidos en botón Nuevo → no se crean duplicados (botón deshabilitado durante op.)
- [ ] **Cerrar con X durante operación**: cerrar ventana mientras hay tarea en curso → la operación termina o cancela limpiamente
- [ ] **Login gestor**: verificar que NO aparecen los botones Usuarios ni Auditoría en el menú lateral
- [ ] **Resize ventana**: redimensionar → los layouts se adaptan, nada se sale del viewport
- [ ] **Texto largo en campos**: pegar texto de 1000+ chars en campos → no rompe nada
- [ ] **Tildes y ñ**: crear producto "Camiseta Niño de Año" → persiste y se muestra correctamente con tildes

## 12.6 Seguridad

- [ ] **Password hasheado en BD**: `SELECT password_hash FROM usuarios` → debe empezar con `$2a$12$`
- [ ] **No password en texto plano**: buscar en logs de stdout/stderr → no aparece ninguna contraseña
- [ ] **Auditoría sin hashes**: revisar tabla auditoria → ningún campo contiene hashes BCrypt ni contraseñas
- [ ] **No leak en UI**: la pantalla de usuarios no muestra el campo password de ningún usuario
