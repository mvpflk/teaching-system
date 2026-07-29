-- 管理员密码重置脚本
-- 更新时间: 2026-06-14
-- 密码: Lm1227
-- 执行: cat fix_admin_pwd.sql | docker exec -i teaching-mysql mysql -uroot --socket=/var/run/mysqld/mysqld.sock teaching_system
UPDATE users SET password = '$2a$10$UrUWBYgcM3H5DU0Pn8KwG.oDE6eynuVXEtbdP5kBNGN94Rt.t95F6' WHERE username = 'admin';
SELECT id, username, CHAR_LENGTH(password) AS pwd_len FROM users WHERE username = 'admin';
