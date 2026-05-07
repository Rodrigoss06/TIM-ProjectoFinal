-- Seed de demo: 10 productos para que el equipo rival pueda probar sin crear datos a mano.
-- Solo se insertan si la tabla productos está vacía al momento de la migración.
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM productos) = 0 THEN
    INSERT INTO productos (codigo, nombre, descripcion, categoria, precio_unitario, stock, activo) VALUES
      ('PROD001', 'Camisa Blanca',       'Talla M, algodon',     'Ropa',       35.00, 0,  TRUE),
      ('PROD002', 'Camisa Azul',         'Talla L, algodon',     'Ropa',       35.00, 0,  TRUE),
      ('PROD003', 'Pantalon Clasico',    'Talla 32, tela',       'Ropa',       65.00, 3,  TRUE),
      ('PROD004', 'Polo Deportivo',      'Talla S, poliester',   'Ropa',       25.00, 3,  TRUE),
      ('PROD005', 'Jeans Slim',          'Talla 30, denim',      'Ropa',       89.00, 20, TRUE),
      ('PROD006', 'Zapatilla Urbana',    'Talla 42, cuero',      'Calzado',   145.00, 15, TRUE),
      ('PROD007', 'Bota de Trabajo',     'Talla 43, resistente', 'Calzado',   185.00, 30, TRUE),
      ('PROD008', 'Sandalia de Playa',   'Talla 40, EVA',        'Calzado',    45.00, 50, TRUE),
      ('PROD009', 'Cinturon de Cuero',   'Talla unica',          'Accesorios', 55.00, 25, TRUE),
      ('PROD010', 'Cartera Clasica',     'Cuero premium',        'Accesorios',250.00, 18, TRUE);
  END IF;
END $$;
