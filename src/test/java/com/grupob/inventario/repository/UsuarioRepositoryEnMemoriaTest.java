package com.grupob.inventario.repository;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.repository.memory.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsuarioRepositoryEnMemoria — CRUD y contratos de interfaz")
class UsuarioRepositoryEnMemoriaTest {

    private UsuarioRepositoryEnMemoria repo;

    @BeforeEach
    void setUp() {
        repo = new UsuarioRepositoryEnMemoria();
    }

    private Usuario admin(String username) {
        return new Usuario(username, "hashSeguro1", Rol.ADMINISTRADOR);
    }

    private Usuario gestor(String username) {
        return new Usuario(username, "hashSeguro2", Rol.GESTOR_INVENTARIO);
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar y buscarPorUsername retornan el mismo usuario")
    void guardarYBuscar_usuarioGuardado_seEncuentra() {
        Usuario u = admin("admin");
        repo.guardar(u);
        assertThat(repo.buscarPorUsername("admin")).isPresent().contains(u);
    }

    @Test
    @DisplayName("buscarPorUsername con username inexistente devuelve Optional vacío")
    void buscarPorUsername_noExiste_optionalVacio() {
        assertThat(repo.buscarPorUsername("fantasma")).isEmpty();
    }

    @Test
    @DisplayName("guardar actualiza el usuario si el username ya existe (upsert)")
    void guardar_usernameDuplicado_actualiza() {
        repo.guardar(gestor("juan"));
        Usuario actualizado = new Usuario("juan", "nuevoHash", Rol.ADMINISTRADOR);
        repo.guardar(actualizado);
        assertThat(repo.buscarPorUsername("juan")).isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRol()).isEqualTo(Rol.ADMINISTRADOR));
    }

    // ── existeUsername ────────────────────────────────────────────────────────

    @Test
    @DisplayName("existeUsername retorna true cuando el usuario existe")
    void existeUsername_existe_true() {
        repo.guardar(admin("admin"));
        assertThat(repo.existeUsername("admin")).isTrue();
    }

    @Test
    @DisplayName("existeUsername retorna false cuando no existe")
    void existeUsername_noExiste_false() {
        assertThat(repo.existeUsername("nadie")).isFalse();
    }

    @Test
    @DisplayName("existeUsername es case-sensitive: 'Admin' ≠ 'admin'")
    void existeUsername_caseSensitive() {
        repo.guardar(admin("admin"));
        assertThat(repo.existeUsername("Admin")).isFalse();
        assertThat(repo.existeUsername("ADMIN")).isFalse();
    }

    // ── listarAdministradoresActivos ──────────────────────────────────────────

    @Test
    @DisplayName("listarAdministradoresActivos retorna solo administradores activos")
    void listarAdministradoresActivos_soloreturnaAdminsActivos() {
        repo.guardar(admin("admin1"));
        repo.guardar(admin("admin2"));
        repo.guardar(gestor("gestor1"));

        assertThat(repo.listarAdministradoresActivos()).hasSize(2)
                .allSatisfy(u -> assertThat(u.getRol()).isEqualTo(Rol.ADMINISTRADOR));
    }

    @Test
    @DisplayName("listarAdministradoresActivos excluye administradores desactivados")
    void listarAdministradoresActivos_excluyeDesactivados() {
        Usuario adminActivo = admin("admin1");
        Usuario adminInactivo = admin("admin2");
        adminInactivo.setActivo(false);

        repo.guardar(adminActivo);
        repo.guardar(adminInactivo);

        assertThat(repo.listarAdministradoresActivos()).hasSize(1)
                .extracting(Usuario::getUsername).containsExactly("admin1");
    }

    @Test
    @DisplayName("listarAdministradoresActivos con ningún admin retorna lista vacía")
    void listarAdministradoresActivos_sinAdmins_vacio() {
        repo.guardar(gestor("gestor1"));
        assertThat(repo.listarAdministradoresActivos()).isEmpty();
    }

    // ── listarTodos ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos incluye todos los usuarios independiente del rol y estado")
    void listarTodos_incluyeTodos() {
        repo.guardar(admin("admin1"));
        repo.guardar(gestor("gestor1"));
        assertThat(repo.listarTodos()).hasSize(2);
    }

    // ── contratos de inmutabilidad ────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos devuelve lista inmutable")
    void listarTodos_listaInmutable() {
        repo.guardar(admin("admin1"));
        List<Usuario> lista = repo.listarTodos();
        assertThatThrownBy(() -> lista.add(admin("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("listarAdministradoresActivos devuelve lista inmutable")
    void listarAdministradoresActivos_listaInmutable() {
        repo.guardar(admin("admin1"));
        List<Usuario> lista = repo.listarAdministradoresActivos();
        assertThatThrownBy(() -> lista.add(admin("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
