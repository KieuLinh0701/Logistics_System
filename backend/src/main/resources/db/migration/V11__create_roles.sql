CREATE TABLE roles (
                       id INTEGER NOT NULL AUTO_INCREMENT,
                       user_owner_id INTEGER NULL,
                       name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                       description VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uq_role_name_per_owner UNIQUE (name, user_owner_id)
) ENGINE=InnoDB;

CREATE INDEX idx_roles_user_owner_id ON roles (user_owner_id);
