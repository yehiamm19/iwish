-- =====================================================================
-- I-Wish Database Full Backup & Schema
-- Compatible with: MySQL 8.0+, MariaDB 10.4+, XAMPP phpMyAdmin
-- Character Set: utf8mb4 (Full Unicode & Emoji support)
-- =====================================================================

-- Step 1: Create and select database
CREATE DATABASE IF NOT EXISTS `iwish` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `iwish`;

-- Step 2: Disable foreign key checks for clean drop and restore
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `contributions`;
DROP TABLE IF EXISTS `wish_items`;
DROP TABLE IF EXISTS `friendships`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- Table Structure: users
-- Passwords are encrypted with standard SHA-256 hash (64 hex characters)
-- =====================================================================
CREATE TABLE `users` (
  `id` INT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `email` VARCHAR(190) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Table Structure: friendships
-- Stores friend relationships and incoming/accepted requests
-- =====================================================================
CREATE TABLE `friendships` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `requester_id` INT NOT NULL,
  `receiver_id` INT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_friend` (`requester_id`, `receiver_id`),
  KEY `idx_receiver_status` (`receiver_id`, `status`),
  CONSTRAINT `fk_friend_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friend_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Table Structure: wish_items
-- Stores user wishes, target price, contributed amount, and funded status
-- =====================================================================
CREATE TABLE `wish_items` (
  `id` INT NOT NULL,
  `owner_id` INT NOT NULL,
  `title` VARCHAR(180) NOT NULL,
  `category` VARCHAR(80) DEFAULT 'General',
  `description` TEXT DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `contributed` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `purchased` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wish_owner` (`owner_id`),
  CONSTRAINT `fk_wish_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Table Structure: contributions
-- Records payments by friends toward a specific wish item
-- =====================================================================
CREATE TABLE `contributions` (
  `id` INT NOT NULL,
  `item_id` INT NOT NULL,
  `buyer_id` INT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contrib_item` (`item_id`),
  KEY `idx_contrib_buyer` (`buyer_id`),
  CONSTRAINT `fk_contrib_item` FOREIGN KEY (`item_id`) REFERENCES `wish_items` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_contrib_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Table Structure: notifications
-- Stores buyer and receiver notifications upon events and gift completion
-- =====================================================================
CREATE TABLE `notifications` (
  `id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `message` VARCHAR(500) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user` (`user_id`),
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Seed Data: Users
-- Demo password for all sample accounts is: 'demo'
-- SHA-256 hash: 2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea
-- =====================================================================
INSERT INTO `users` (`id`, `name`, `email`, `password_hash`) VALUES
(1, 'Demo User', 'demo@iwish.local', '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea'),
(2, 'Mona Ali', 'mona@iwish.local', '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea'),
(3, 'Omar Hassan', 'omar@iwish.local', '2a97516c354b68848cdbd8f54a226a0a55b21ed138e207ad6c5cbb9c00aa5aea');

-- =====================================================================
-- Seed Data: Friendships
-- =====================================================================
INSERT INTO `friendships` (`id`, `requester_id`, `receiver_id`, `status`) VALUES
(1, 1, 2, 'ACCEPTED'),
(2, 1, 3, 'ACCEPTED'),
(3, 2, 3, 'ACCEPTED');

-- =====================================================================
-- Seed Data: Wish Items
-- =====================================================================
INSERT INTO `wish_items` (`id`, `owner_id`, `title`, `category`, `description`, `price`, `contributed`, `purchased`) VALUES
(4, 2, 'Noise Cancelling Headphones', 'Tech', 'For my daily focus time', 129.99, 25.00, 0),
(5, 2, 'Weekend Backpack', 'Travel', 'A compact adventure bag', 79.50, 79.50, 1),
(6, 3, 'Classic Watch', 'Style', 'A timeless everyday watch', 150.00, 0.00, 0);

-- =====================================================================
-- Seed Data: Contributions
-- =====================================================================
INSERT INTO `contributions` (`id`, `item_id`, `buyer_id`, `amount`) VALUES
(1, 4, 1, 25.00),
(2, 5, 1, 79.50);

-- =====================================================================
-- Seed Data: Notifications
-- =====================================================================
INSERT INTO `notifications` (`id`, `user_id`, `message`, `is_read`) VALUES
(1, 1, 'Welcome to I-Wish! Start making wishes or contribute to your friends.', 1),
(2, 2, 'Demo User contributed $79.50 toward your Weekend Backpack.', 1),
(3, 1, 'The gift item \"Weekend Backpack\" you contributed to is now fully funded!', 1),
(4, 2, 'Your wish \"Weekend Backpack\" has been fully funded by your friends!', 1);
