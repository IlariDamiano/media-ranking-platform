
CREATE TABLE app_users (
                           id UUID PRIMARY KEY,

                           username VARCHAR(30) NOT NULL,
                           email VARCHAR(254) NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,

                           role VARCHAR(20) NOT NULL DEFAULT 'USER',

                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                           CONSTRAINT chk_app_users_username
                               CHECK (username ~ '^[A-Za-z0-9_]{3,30}$'),

    CONSTRAINT chk_app_users_email
        CHECK (length(trim(email)) > 0),

    CONSTRAINT chk_app_users_password_hash
        CHECK (length(trim(password_hash)) > 0),

    CONSTRAINT chk_app_users_role
        CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX uq_app_users_username_ci
    ON app_users (LOWER(username));

CREATE UNIQUE INDEX uq_app_users_email_ci
    ON app_users (LOWER(email));
