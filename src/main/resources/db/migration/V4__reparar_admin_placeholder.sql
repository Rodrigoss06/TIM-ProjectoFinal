-- La migración V2 inserta admin con hash placeholder de 35 chars.
-- Un hash BCrypt válido tiene exactamente 60 chars.
-- Esta migración elimina el placeholder para que App.java cree el admin real con BCrypt strength 12.
-- Si el admin ya fue creado por la app (hash válido de 60 chars), este DELETE no hace nada.
DELETE FROM usuarios WHERE username = 'admin' AND LENGTH(password_hash) < 60;
