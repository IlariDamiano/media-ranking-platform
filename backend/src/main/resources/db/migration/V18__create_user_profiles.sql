
CREATE TABLE user_profiles (
                               user_id UUID PRIMARY KEY,

                               bio VARCHAR(500),
                               avatar_key VARCHAR(80),

                               visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',

                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_user_profiles_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES app_users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_user_profiles_visibility
                                   CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

-- Create profiles for accounts that already exist.
INSERT INTO user_profiles (user_id)
SELECT id
FROM app_users;
