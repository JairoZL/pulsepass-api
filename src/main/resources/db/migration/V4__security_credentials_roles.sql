-- PRD Seguridad §9.1 y §9.2: credenciales y roles en tablas propias.
-- La tabla users no cambia, así ningún DTO de negocio (UserResponse, MapStruct)
-- puede serializar el hash por accidente.

-- 1:1 con users: la PK es el mismo user_id (un usuario tiene como máximo una credencial)
CREATE TABLE user_credentials (
    user_id              BIGINT       NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,   -- BCrypt mide 60 caracteres; 100 deja margen
    password_changed_at  TIMESTAMP,               -- opcional según el PRD

    CONSTRAINT pk_user_credentials      PRIMARY KEY (user_id),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- N:1 con users: un usuario puede tener varios roles, pero nunca el mismo dos veces.
-- La PK (user_id, role) empieza por user_id, así que también sirve de índice
-- para "roles de un usuario": no hace falta un índice adicional.
CREATE TABLE user_roles (
    user_id  BIGINT      NOT NULL,
    role     VARCHAR(20) NOT NULL,

    CONSTRAINT pk_user_roles      PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- Usuarios legados (creados antes de esta fase): reciben solo ROLE_USER y NO reciben
-- credencial. No se inventa contraseña por defecto (§9.2), así que no pueden iniciar
-- sesión hasta una activación segura. Nunca se asigna STAFF ni ADMIN aquí (NFR-SEC-009).
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;
