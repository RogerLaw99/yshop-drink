-- ============================================================
-- 一桌多码：同一桌台可配置多个二维码，每个二维码独立会话/订单
-- 执行：mysql -uroot -p yixiang-drink-open < sql/desk-multi-qr.sql
-- ============================================================

-- 1. 桌台二维码表
CREATE TABLE IF NOT EXISTS `yshop_store_desk_qr` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '二维码编号',
  `shop_id` bigint NOT NULL COMMENT '门店编号',
  `desk_id` bigint NOT NULL COMMENT '桌台编号',
  `qr_name` varchar(64) NOT NULL COMMENT '二维码名称，如 A01-左',
  `qr_token` varchar(64) NOT NULL COMMENT '二维码令牌',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `sort` int DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qr_token` (`qr_token`),
  UNIQUE KEY `uk_desk_qr_name` (`desk_id`, `qr_name`, `deleted`),
  KEY `idx_desk` (`desk_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌台二维码';

-- 2. 把现有桌台的 qr_token 迁移为默认二维码
INSERT INTO `yshop_store_desk_qr`
  (`shop_id`, `desk_id`, `qr_name`, `qr_token`, `status`, `sort`, `creator`, `updater`)
SELECT d.shop_id, d.id, d.desk_number, d.qr_token, 1, 1, 'system', 'system'
FROM `yshop_store_desk` d
WHERE d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `yshop_store_desk_qr` q WHERE q.desk_id = d.id AND q.deleted = 0
  );

-- 3. 会话表增加二维码维度
SET @dbname = DATABASE();
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_desk_session` ADD COLUMN `qr_id` bigint DEFAULT NULL COMMENT ''二维码编号''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_desk_session' AND column_name = 'qr_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_desk_session` ADD COLUMN `qr_name` varchar(64) DEFAULT NULL COMMENT ''二维码名称''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_desk_session' AND column_name = 'qr_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 旧会话没有二维码维度，直接关闭，避免串单
UPDATE `yshop_store_desk_session` SET `status` = 2, `close_time` = NOW() WHERE `status` = 0;

-- 4. 订单表增加二维码维度
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_order` ADD COLUMN `desk_qr_id` bigint DEFAULT NULL COMMENT ''桌台二维码编号''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_order' AND column_name = 'desk_qr_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_order` ADD COLUMN `desk_qr_name` varchar(64) DEFAULT NULL COMMENT ''桌台二维码名称''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_order' AND column_name = 'desk_qr_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 补充索引
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_desk_session` ADD INDEX `idx_qr_status` (`qr_id`, `status`)',
  'SELECT 1') FROM information_schema.statistics
  WHERE table_schema = @dbname AND table_name = 'yshop_store_desk_session' AND index_name = 'idx_qr_status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
