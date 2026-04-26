-- Run this script in phpMyAdmin to create test accounts
-- Passwords for both accounts are: password

INSERT INTO user (email, roles, password, type) 
VALUES 
('admin@esports.com', '["ROLE_ADMIN"]', '$2a$10$oA3nhZ10ep6ACNxoKJfOIOjrU06oD2nVYgO5Kn/NTbk.7QO0KZQm6', 'admin'),
('player@esports.com', '["ROLE_PLAYER"]', '$2a$10$oA3nhZ10ep6ACNxoKJfOIOjrU06oD2nVYgO5Kn/NTbk.7QO0KZQm6', 'player'),
('coach@esports.com', '["ROLE_COACH"]', '$2a$10$oA3nhZ10ep6ACNxoKJfOIOjrU06oD2nVYgO5Kn/NTbk.7QO0KZQm6', 'coach');
