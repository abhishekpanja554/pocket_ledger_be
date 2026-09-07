ALTER TABLE transactions DROP CONSTRAINT transactions_user_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE tags DROP CONSTRAINT tags_user_id_fkey;
ALTER TABLE tags ADD CONSTRAINT tags_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE rules DROP CONSTRAINT rules_user_id_fkey;
ALTER TABLE rules ADD CONSTRAINT rules_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE settings DROP CONSTRAINT settings_user_id_fkey;
ALTER TABLE settings ADD CONSTRAINT settings_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE documents DROP CONSTRAINT documents_user_id_fkey;
ALTER TABLE documents ADD CONSTRAINT documents_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;