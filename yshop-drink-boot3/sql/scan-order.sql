-- ============================================================
-- 扫码点餐功能数据库变更
-- 适用数据库：MySQL 8
-- 执行方式：
--   mysql -uroot -p yixiang-drink-open < sql/scan-order.sql
-- ============================================================

-- 1. 门店桌台表
CREATE TABLE IF NOT EXISTS `yshop_store_desk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '桌台编号',
  `shop_id` bigint NOT NULL COMMENT '门店编号',
  `desk_number` varchar(32) NOT NULL COMMENT '桌号',
  `area` varchar(64) DEFAULT NULL COMMENT '区域/楼层',
  `capacity` int DEFAULT 4 COMMENT '座位数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `qr_token` varchar(64) NOT NULL COMMENT '二维码令牌（不可猜测）',
  `sort` int DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_desk` (`shop_id`, `desk_number`, `deleted`),
  UNIQUE KEY `uk_qr_token` (`qr_token`),
  KEY `idx_shop_status` (`shop_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店桌台';

-- 2. 订单表补充桌台字段（仅首次执行需要）
SET @dbname = DATABASE();
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_order` ADD COLUMN `desk_id` bigint DEFAULT NULL COMMENT ''桌台编号''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_order' AND column_name = 'desk_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_order` ADD COLUMN `desk_number` varchar(32) DEFAULT NULL COMMENT ''桌号''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_order' AND column_name = 'desk_number');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `yshop_store_order` ADD COLUMN `desk_people` int DEFAULT NULL COMMENT ''就餐人数''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = @dbname AND table_name = 'yshop_store_order' AND column_name = 'desk_people');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 后台菜单：桌台管理（挂在“门店管理”下）
INSERT IGNORE INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `updater`, `deleted`)
VALUES
  (2314, '桌台管理', '', 2, 6, 2264, '/mall/store/desk/index', 'ep:grid',
   'mall/store/desk/index', 'StoreDesk', 0, b'1', b'1', b'1', '1', '1', b'0'),
  (2315, '桌台查询', 'store:desk:query', 3, 1, 2314, '', '', '', NULL,
   0, b'1', b'1', b'1', '1', '1', b'0'),
  (2316, '桌台创建', 'store:desk:create', 3, 2, 2314, '', '', '', NULL,
   0, b'1', b'1', b'1', '1', '1', b'0'),
  (2317, '桌台更新', 'store:desk:update', 3, 3, 2314, '', '', '', NULL,
   0, b'1', b'1', b'1', '1', '1', b'0'),
  (2318, '桌台删除', 'store:desk:delete', 3, 4, 2314, '', '', '', NULL,
   0, b'1', b'1', b'1', '1', '1', b'0'),
  (2319, '桌台批量创建', 'store:desk:create', 3, 5, 2314, '', '', '', NULL,
   0, b'1', b'1', b'1', '1', '1', b'0');

-- 4. 给门店角色（role_id=2）授权桌台菜单
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `updater`, `deleted`, `tenant_id`)
SELECT 2, m.id, '1', '1', b'0', 0
FROM `system_menu` m
WHERE m.id BETWEEN 2314 AND 2319
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm WHERE rm.role_id = 2 AND rm.menu_id = m.id
  );

-- 5. 本地测试桌台（门店 3 = yshop）
INSERT INTO `yshop_store_desk`
  (`shop_id`, `desk_number`, `area`, `capacity`, `status`, `qr_token`, `sort`, `creator`, `updater`)
VALUES
  (3, 'A01', '大厅', 2, 1, REPLACE(UUID(), '-', ''), 1, 'system', 'system'),
  (3, 'A02', '大厅', 4, 1, REPLACE(UUID(), '-', ''), 2, 'system', 'system'),
  (3, 'A03', '大厅', 2, 1, REPLACE(UUID(), '-', ''), 3, 'system', 'system')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;
