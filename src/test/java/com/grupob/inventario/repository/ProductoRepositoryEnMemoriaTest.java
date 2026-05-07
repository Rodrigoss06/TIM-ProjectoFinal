package com.grupob.inventario.repository;

import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.repository.memory.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductoRepositoryEnMemoria — CRUD, búsquedas y contratos de interfaz")
class ProductoRepositoryEnMemoriaTest {

    private ProductoRepositoryEnMemoria repo;

    @BeforeEach
    void setUp() {
        repo = new ProductoRepositoryEnMemoria();
    }

    private Producto producto(String codigo, String nombre, String categoria, int stock) {
        return new Producto(codigo, nombre, "desc", categoria, new BigDecimal("10.00"), stock);
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar y buscarPorCodigo retornan el mismo producto")
    void guardarYBuscar_productoGuardado_seEncuentra() {
        Producto p = producto("P001", "Arroz", "Alimentos", 100);
        repo.guardar(p);
        Optional<Producto> resultado = repo.buscarPorCodigo("P001");
        assertThat(resultado).isPresent().contains(p);
    }

    @Test
    @DisplayName("buscarPorCodigo con código inexistente devuelve Optional vacío")
    void buscarPorCodigo_noExiste_optionalVacio() {
        assertThat(repo.buscarPorCodigo("NOEXISTE")).isEmpty();
    }

    @Test
    @DisplayName("guardar actualiza el producto si el código ya existe (upsert)")
    void guardar_codigoDuplicado_actualiza() {
        repo.guardar(producto("P001", "Arroz Blanco", "Alimentos", 10));
        Producto actualizado = producto("P001", "Arroz Integral", "Alimentos", 20);
        repo.guardar(actualizado);
        assertThat(repo.buscarPorCodigo("P001")).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getNombre()).isEqualTo("Arroz Integral"));
    }

    // ── case-insensitive de código (sección 8.1 Notion) ──────────────────────

    @Test
    @DisplayName("buscarPorCodigo es case-insensitive: 'p001' encuentra producto guardado como 'P001'")
    void buscarPorCodigo_caseInsensitive_encuentra() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        assertThat(repo.buscarPorCodigo("p001")).isPresent();
        assertThat(repo.buscarPorCodigo("P001")).isPresent();
    }

    @Test
    @DisplayName("existeCodigo es case-insensitive: 'PROD001' y 'prod001' son el mismo código")
    void existeCodigo_caseInsensitive() {
        repo.guardar(producto("PROD001", "Fideos", "Alimentos", 5));
        assertThat(repo.existeCodigo("PROD001")).isTrue();
        assertThat(repo.existeCodigo("prod001")).isTrue();
        assertThat(repo.existeCodigo("Prod001")).isTrue();
    }

    @Test
    @DisplayName("existeCodigo con código inexistente devuelve false")
    void existeCodigo_noExiste_false() {
        assertThat(repo.existeCodigo("NOEXISTE")).isFalse();
    }

    // ── búsqueda por nombre ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorNombre encuentra coincidencias parciales")
    void buscarPorNombre_parcial_encuentraCoincidencias() {
        repo.guardar(producto("P001", "Camisa Blanca", "Ropa", 10));
        repo.guardar(producto("P002", "Camiseta Deportiva", "Ropa", 5));
        repo.guardar(producto("P003", "Pantalon", "Ropa", 8));

        List<Producto> resultado = repo.buscarPorNombre("camis");
        assertThat(resultado).hasSize(2)
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder("Camisa Blanca", "Camiseta Deportiva");
    }

    @Test
    @DisplayName("buscarPorNombre es case-insensitive: 'ARROZ' encuentra 'Arroz'")
    void buscarPorNombre_caseInsensitive() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        assertThat(repo.buscarPorNombre("ARROZ")).hasSize(1);
        assertThat(repo.buscarPorNombre("arroz")).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorNombre con string vacío retorna todos los activos")
    void buscarPorNombre_vacio_retornaTodosActivos() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.guardar(producto("P002", "Fideos", "Alimentos", 5));
        assertThat(repo.buscarPorNombre("")).hasSize(2);
        assertThat(repo.buscarPorNombre(null)).hasSize(2);
    }

    @Test
    @DisplayName("buscarPorNombre ignora productos inactivos (8.2 Notion)")
    void buscarPorNombre_productoEliminado_noAparece() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.eliminar("P001");
        assertThat(repo.buscarPorNombre("Arroz")).isEmpty();
    }

    @Test
    @DisplayName("buscarPorNombre retorna resultados ordenados alfabéticamente")
    void buscarPorNombre_resultadosOrdenados() {
        repo.guardar(producto("P001", "Zanahoria", "Verduras", 10));
        repo.guardar(producto("P002", "Apio", "Verduras", 5));
        repo.guardar(producto("P003", "Brocoli", "Verduras", 8));

        List<Producto> resultado = repo.buscarPorNombre("");
        assertThat(resultado).extracting(Producto::getNombre)
                .containsExactly("Apio", "Brocoli", "Zanahoria");
    }

    // ── búsqueda por categoría ────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorCategoria retorna solo productos de esa categoría")
    void buscarPorCategoria_filtraPorCategoria() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.guardar(producto("P002", "Fideos", "Alimentos", 5));
        repo.guardar(producto("P003", "Camisa", "Ropa", 8));

        assertThat(repo.buscarPorCategoria("Alimentos")).hasSize(2);
        assertThat(repo.buscarPorCategoria("Ropa")).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorCategoria es case-insensitive: 'ALIMENTOS' = 'alimentos'")
    void buscarPorCategoria_caseInsensitive() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        assertThat(repo.buscarPorCategoria("ALIMENTOS")).hasSize(1);
        assertThat(repo.buscarPorCategoria("alimentos")).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorCategoria ignora productos inactivos")
    void buscarPorCategoria_productoEliminado_noAparece() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.eliminar("P001");
        assertThat(repo.buscarPorCategoria("Alimentos")).isEmpty();
    }

    // ── listarActivos / listarTodos ───────────────────────────────────────────

    @Test
    @DisplayName("listarActivos excluye productos con activo = false")
    void listarActivos_excluyeInactivos() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.guardar(producto("P002", "Fideos", "Alimentos", 5));
        repo.eliminar("P001");

        assertThat(repo.listarActivos()).hasSize(1)
                .extracting(Producto::getCodigo).containsExactly("P002");
    }

    @Test
    @DisplayName("listarTodos incluye activos e inactivos")
    void listarTodos_incluyeInactivos() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.guardar(producto("P002", "Fideos", "Alimentos", 5));
        repo.eliminar("P001");

        assertThat(repo.listarTodos()).hasSize(2);
    }

    @Test
    @DisplayName("listarActivos con inventario vacío retorna lista vacía")
    void listarActivos_vacio_listaVacia() {
        assertThat(repo.listarActivos()).isEmpty();
    }

    // ── eliminación lógica ────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar pone activo = false pero el producto sigue en el repo")
    void eliminar_eliminacionLogica_productoSigueBuscable() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.eliminar("P001");

        Optional<Producto> buscado = repo.buscarPorCodigo("P001");
        assertThat(buscado).isPresent()
                .hasValueSatisfying(p -> assertThat(p.isActivo()).isFalse());
    }

    @Test
    @DisplayName("eliminar con código inexistente no lanza excepción")
    void eliminar_codigoInexistente_sinExcepcion() {
        repo.eliminar("NOEXISTE");
    }

    @Test
    @DisplayName("eliminar dos veces no lanza excepción — producto sigue inactivo")
    void eliminar_doble_sigueInactivo() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        repo.eliminar("P001");
        repo.eliminar("P001");
        assertThat(repo.buscarPorCodigo("P001")).isPresent()
                .hasValueSatisfying(p -> assertThat(p.isActivo()).isFalse());
    }

    // ── contratos de inmutabilidad (sección 6.3 Notion) ──────────────────────

    @Test
    @DisplayName("listarActivos devuelve lista inmutable")
    void listarActivos_listaInmutable() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        List<Producto> lista = repo.listarActivos();
        assertThatThrownBy(() -> lista.add(producto("X", "X", "X", 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("listarTodos devuelve lista inmutable")
    void listarTodos_listaInmutable() {
        List<Producto> lista = repo.listarTodos();
        assertThatThrownBy(() -> lista.add(producto("X", "X", "X", 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("buscarPorNombre devuelve lista inmutable")
    void buscarPorNombre_listaInmutable() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        List<Producto> lista = repo.buscarPorNombre("Arroz");
        assertThatThrownBy(() -> lista.add(producto("X", "X", "X", 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("buscarPorCategoria devuelve lista inmutable")
    void buscarPorCategoria_listaInmutable() {
        repo.guardar(producto("P001", "Arroz", "Alimentos", 10));
        List<Producto> lista = repo.buscarPorCategoria("Alimentos");
        assertThatThrownBy(() -> lista.add(producto("X", "X", "X", 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
