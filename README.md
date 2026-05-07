# Módulo Inventario — Grupo B

Sistema de inventario con UI JavaFX + PostgreSQL, desarrollado para el ejercicio *Guerra de Testers* — **Testing, Implantación y Mantenimiento de Sistemas**, UCSM (2026).

## Integrantes

| Código | Nombre |
|--------|--------|
| 2023200842 | Barreda Condori Dayana Lucía |
| 2023221011 | Castro Rosas Diogo Sebastian |
| 2023222151 | Ferro Huanchi Alvaro Jeanpiero |
| 2023225471 | Sucapuca Santamarina Rodrigo Alonso |

---

## Requisitos previos

- **Java 17** (LTS)
- **Maven 3.9+**
- **Docker** (para PostgreSQL)

---

## Setup en 3 comandos

```bash
git clone <repo-url>
cd inventario-grupo-b
docker compose up -d && mvn javafx:run
```

La aplicación:
1. Conecta a Postgres
2. Ejecuta migraciones Flyway (crea tablas + seed demo)
3. Crea usuario `admin` con password `Admin123!` si la BD está vacía
4. Abre la UI JavaFX con pantalla de login

---

## Credenciales por defecto

| Rol | Usuario | Contraseña |
|-----|---------|-----------|
| Administrador | `admin` | `Admin123!` |

El administrador puede crear usuarios Gestor de Inventario desde la pantalla de Usuarios.

---

## Comandos

```bash
# Levantar base de datos
docker compose up -d

# Arrancar UI gráfica (JavaFX)
mvn javafx:run

# Arrancar modo consola (CLI legacy)
mvn javafx:run -Dexec.args="--cli"

# Tests unitarios rápidos (sin Docker)
mvn clean test

# Tests completos con integración (Docker requerido)
mvn clean verify

# Ver cobertura JaCoCo
mvn clean verify
# Reporte en: target/site/jacoco/index.html

# Destruir BD y reiniciar limpio
docker compose down -v && docker compose up -d
```

---

## Requerimientos implementados

### Etapa 1 — CLI + memoria
- [x] **RF-INV-001** — Registrar producto (validación completa)
- [x] **RF-INV-002** — Buscar producto (código, nombre parcial, categoría, case-insensitive)
- [x] **RF-INV-003** — Actualizar stock (entradas/salidas + historial de movimientos)
- [x] **RF-INV-004** — Eliminar producto (eliminación lógica, historial conservado)
- [x] **RF-INV-005** — Listar con filtros (`[BAJO]`/`[SIN STOCK]`, precio, estado)
- [x] **RF-INV-006** — Autenticación (BCrypt, lockout 3 intentos, sesión 30 min)
- [x] **RF-INV-007** — Gestión de roles (admin/gestor, último admin protegido)

### Etapa 2 — JavaFX + PostgreSQL
- [x] **RF-INV-008** — Persistencia transaccional (HikariCP + Hibernate + Flyway)
- [x] **RF-INV-009** — Auditoría end-to-end (append-only, filtros, paginación)
- [x] **RF-INV-010** — Configuración externa (`application.properties` + variables de entorno)

---

## Para testers del grupo rival

### Arranque rápido (< 2 minutos)

```bash
# 1. Levantar Postgres
docker compose up -d

# 2. Arrancar la UI
mvn javafx:run

# 3. Login con admin / Admin123!
```

### Datos seed automáticos

Al primer arranque con BD vacía, el sistema crea automáticamente:
- **Usuario admin**: `admin` / `Admin123!`
- **10 productos demo** en categorías Ropa, Calzado, Accesorios con stocks variados:
  - 2 productos con stock = 0 (marcados `[SIN STOCK]`)
  - 2 productos con stock = 3 (marcados `[BAJO]`)
  - 6 productos con stock normal

### Modo alternativo (CLI)

```bash
mvn javafx:run -Dexec.args="--cli"
# Credenciales: admin / Admin123!
# También: gestor / Gestor123 (crear vía admin → Usuarios primero)
```

### Qué probar

Ver `docs/smoke-manual.md` para el checklist completo de la sección 12 de Notion.

---

## Stack técnico

| Tecnología | Versión |
|---|---|
| Java | 17 LTS |
| JavaFX | 21.0.5 |
| Hibernate | 6.4.4 |
| PostgreSQL | 16 |
| HikariCP | 5.1.0 |
| Flyway | 10.10.0 |
| Testcontainers | 1.20.4 |
| JUnit 5 | 5.10.2 |
| BCrypt | jBCrypt 0.4 |

---

## Documentación

- **Notion Etapa 1 (RF-INV-001..007):** https://app.notion.com/p/351c3fa6d4ce818b9234da65d98a4a8e
- **Notion Etapa 2 (RF-INV-008..010 + JavaFX):** https://www.notion.so/358c3fa6d4ce815392acf3a310a6895c
- **Mapeo criterios ↔ tests:** `docs/cobertura-requerimientos.md`
- **Checklist smoke manual:** `docs/smoke-manual.md`
