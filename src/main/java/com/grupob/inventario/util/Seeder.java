package com.grupob.inventario.util;

import com.grupob.inventario.domain.enums.Rol;
import com.grupob.inventario.domain.model.Producto;
import com.grupob.inventario.domain.model.Usuario;
import com.grupob.inventario.repository.ProductoRepository;
import com.grupob.inventario.repository.UsuarioRepository;
import com.grupob.inventario.security.PasswordHasher;

import java.math.BigDecimal;

public final class Seeder {

    private Seeder() {}

    public static void sembrar(UsuarioRepository usuarioRepo,
                                ProductoRepository productoRepo,
                                PasswordHasher hasher) {
        // Usuarios de prueba (sección 7.4 Notion)
        usuarioRepo.guardar(new Usuario("admin",  hasher.hashear("Admin123!"),  Rol.ADMINISTRADOR));
        usuarioRepo.guardar(new Usuario("gestor", hasher.hashear("Gestor123"), Rol.GESTOR_INVENTARIO));

        // 10 productos: 2 sin stock, 2 bajos (stock=3), 6 normales
        // Categorías: Ropa, Calzado, Accesorios
        productoRepo.guardar(new Producto("PROD001", "Camisa Blanca",      "Talla M, algodón",      "Ropa",        new BigDecimal("35.00"),  0));
        productoRepo.guardar(new Producto("PROD002", "Camisa Azul",        "Talla L, algodón",      "Ropa",        new BigDecimal("35.00"),  0));
        productoRepo.guardar(new Producto("PROD003", "Pantalón Clásico",   "Talla 32, tela",        "Ropa",        new BigDecimal("65.00"),  3));
        productoRepo.guardar(new Producto("PROD004", "Polo Deportivo",     "Talla S, poliéster",    "Ropa",        new BigDecimal("25.00"),  3));
        productoRepo.guardar(new Producto("PROD005", "Jeans Slim",         "Talla 30, denim",       "Ropa",        new BigDecimal("89.00"), 20));
        productoRepo.guardar(new Producto("PROD006", "Zapatilla Urbana",   "Talla 42, cuero",       "Calzado",     new BigDecimal("145.00"), 15));
        productoRepo.guardar(new Producto("PROD007", "Bota de Trabajo",    "Talla 43, resistente",  "Calzado",     new BigDecimal("185.00"), 30));
        productoRepo.guardar(new Producto("PROD008", "Sandalia de Playa",  "Talla 40, EVA",         "Calzado",     new BigDecimal("45.00"),  50));
        productoRepo.guardar(new Producto("PROD009", "Cinturón de Cuero",  "Talla única",           "Accesorios",  new BigDecimal("55.00"),  25));
        productoRepo.guardar(new Producto("PROD010", "Cartera Clásica",    "Cuero premium",         "Accesorios",  new BigDecimal("250.00"), 18));
    }
}
