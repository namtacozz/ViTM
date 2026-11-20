CREATE DATABASE IF NOT EXISTS atm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE atm_db;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS external_accounts;
DROP TABLE IF EXISTS atm_vault;
DROP TABLE IF EXISTS atm_config;

CREATE TABLE accounts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  so_tai_khoan VARCHAR(20) UNIQUE NOT NULL,
  ten_chu_tk VARCHAR(100) NOT NULL,
  so_du BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE external_accounts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  ma_ngan_hang VARCHAR(20) NOT NULL,
  ten_ngan_hang VARCHAR(100) NOT NULL,
  so_tai_khoan VARCHAR(20) NOT NULL,
  ten_chu_tk VARCHAR(100) NOT NULL,
  UNIQUE KEY uq_bank_stk (ma_ngan_hang, so_tai_khoan)
);

CREATE TABLE cards (
  so_the VARCHAR(16) PRIMARY KEY,
  pin VARCHAR(64) NOT NULL,
  account_id INT NOT NULL,
  so_lan_sai INT DEFAULT 0,
  trang_thai VARCHAR(15) DEFAULT 'ACTIVE',
  FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  ma_tham_chieu VARCHAR(32) UNIQUE NOT NULL,
  account_id INT NOT NULL,
  loai VARCHAR(20) NOT NULL,
  so_tien BIGINT DEFAULT 0,
  phi BIGINT DEFAULT 0,
  so_du_sau BIGINT,
  tk_doi_tac VARCHAR(30),
  ngan_hang_nhan VARCHAR(50),
  noi_dung VARCHAR(255),
  dia_diem_atm VARCHAR(150),
  thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE atm_vault (
  menh_gia INT PRIMARY KEY,
  so_to INT NOT NULL DEFAULT 0
);

CREATE TABLE atm_config (
  config_key VARCHAR(50) PRIMARY KEY,
  config_value VARCHAR(200) NOT NULL
);

CREATE TABLE audit_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
  atm_id VARCHAR(30) NOT NULL,
  so_the VARCHAR(20),
  account_id INT,
  hanh_dong VARCHAR(50) NOT NULL,
  chi_tiet VARCHAR(255),
  ket_qua VARCHAR(30) NOT NULL
);
