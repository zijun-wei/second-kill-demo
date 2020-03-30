CREATE DATABASE `seckill` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;

USE seckill;

CREATE TABLE `user_info` (`id` int NOT NULL AUTO_INCREMENT,
					`name` varchar(64) NOT NULL DEFAULT '',
					`gender` tinyint NOT NULL COMMENT '1代表男性，2代表女性',
					`age` int NOT NULL,
					`telephone` varchar(32) NOT NULL,
					`register_mode` varchar(64) NOT NULL DEFAULT 'byphone' COMMENT 'byphone,bywechat,byalipay',
					`third_party_id` varchar(64)  DEFAULT '' NOT NULL,
					PRIMARY KEY(`id`))ENGINE=InnoDB;

CREATE TABLE `user_password` (`id` int NOT NULL AUTO_INCREMENT,
					`encrpt_password` varchar(128) NOT NULL,
					`user_id` int NOT NULL,
					PRIMARY KEY(`id`))ENGINE=InnoDB;

CREATE TABLE `item`(`id` int NOT NULL  AUTO_INCREMENT,
					`title` varchar(64) NOT NULL DEFAULT '',
					`price` decimal NOT NULL DEFAULT 0,
					`description` varchar(512) NOT NULL DEFAULT '',
					`sales` int NOT NULL DEFAULT 0,
					`img_url` varchar(1024) NOT NULL DEFAULT '',
					PRIMARY KEY(`id`))ENGINE=InnoDB;


CREATE TABLE `item_stock`(`id` int NOT NULL AUTO_INCREMENT,
					`stock` int NOT NULL DEFAULT 0,
					`item_id` int NOT NULL UNIQUE,
					PRIMARY KEY(`id`))ENGINE=InnoDB;



CREATE TABLE `kill_activity`(`id` int NOT NULL AUTO_INCREMENT,
					`kill_name` varchar(64) NOT NULL DEFAULT '',
					`kill_item_price` decimal NOT NULL DEFAULT 0,
					`item_id` int NOT NULL,
					`start_date` timestamp not null default current_timestamp comment '创建时间',
					`end_date` timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
					PRIMARY KEY(`id`))ENGINE=InnoDB;


CREATE TABLE `order_info`(`id` varchar(32) NOT NULL PRIMARY KEY,
					`user_id` int NOT NULL,
					`item_id` int NOT NULL,
					`item_price` decimal NOT NULL DEFAULT 0,
					`amount` int NOT NULL,
					`order_price` decimal NOT NULL DEFAULT 0,
					`kill_id` int NOT NULL)ENGINE=InnoDB;


CREATE TABLE `sequence_info`(`name` varchar(32) NOT NULL PRIMARY KEY,
					`current_value` int NOT NULL,
					`step` int NOT NULL)ENGINE=InnoDB;

CREATE TABLE `stock_log`(`stock_log_id` varchar(64) NOT NULL PRIMARY KEY,
					`item_id` int NOT NULL DEFAULT 0,
					`amount` int NOT NULL DEFAULT 0)ENGINE=InnoDB;