-- Run this SQL to add the idUser column to the existing cashback table
ALTER TABLE cashback ADD COLUMN idUser int(11) DEFAULT NULL AFTER idTransaction;
ALTER TABLE cashback ADD KEY fk_cashback_user (idUser);
ALTER TABLE cashback ADD CONSTRAINT fk_cashback_user FOREIGN KEY (idUser) REFERENCES users (idUser) ON DELETE SET NULL;
