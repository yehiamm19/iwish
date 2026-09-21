-- I-Wish database schema
-- Compatible with MySQL 8+ and MariaDB 10.6+
CREATE DATABASE IF NOT EXISTS iwish CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE iwish;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS friendships (
  id INT AUTO_INCREMENT PRIMARY KEY,
  requester_id INT NOT NULL,
  receiver_id INT NOT NULL,
  status ENUM('PENDING','ACCEPTED') NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_friend_request (requester_id, receiver_id),
  CONSTRAINT fk_friend_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_friend_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS available_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  category VARCHAR(80) NOT NULL DEFAULT 'General',
  description TEXT,
  default_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS wish_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  owner_id INT NOT NULL,
  title VARCHAR(180) NOT NULL,
  category VARCHAR(80) NOT NULL DEFAULT 'General',
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  contributed DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  purchased BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wish_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_wish_price CHECK (price > 0),
  CONSTRAINT chk_wish_contributed CHECK (contributed >= 0 AND contributed <= price)
);

CREATE TABLE IF NOT EXISTS contributions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  item_id INT NOT NULL,
  buyer_id INT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_contribution_item FOREIGN KEY (item_id) REFERENCES wish_items(id) ON DELETE CASCADE,
  CONSTRAINT fk_contribution_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_contribution_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS notifications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_wish_owner ON wish_items(owner_id);
CREATE INDEX idx_friend_receiver_status ON friendships(receiver_id, status);
CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read);
