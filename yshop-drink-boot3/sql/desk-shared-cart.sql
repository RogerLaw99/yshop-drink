-- ============================================================
-- 桌台共享购物车（多人同桌、一人支付、合并一单）
-- 适用数据库：MySQL 8
-- 执行：mysql -uroot -p yixiang-drink-open < sql/desk-shared-cart.sql
-- ============================================================

-- 桌台会话：一次开台对应一条记录
CREATE TABLE IF NOT EXISTS `yshop_store_desk_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话编号',
  `shop_id` bigint NOT NULL COMMENT '门店编号',
  `desk_id` bigint NOT NULL COMMENT '桌台编号',
  `desk_number` varchar(32) NOT NULL COMMENT '桌号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0进行中，1已下单，2已取消',
  `payer_uid` bigint DEFAULT NULL COMMENT '支付人用户编号',
  `order_id` varchar(64) DEFAULT NULL COMMENT '生成的订单号',
  `close_time` datetime DEFAULT NULL COMMENT '结束时间',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_desk_status` (`desk_id`, `status`),
  KEY `idx_shop_status` (`shop_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌台会话';

-- 共享购物车明细
CREATE TABLE IF NOT EXISTS `yshop_store_desk_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车项编号',
  `session_id` bigint NOT NULL COMMENT '会话编号',
  `shop_id` bigint NOT NULL COMMENT '门店编号',
  `uid` bigint NOT NULL COMMENT '添加人用户编号',
  `nickname` varchar(64) DEFAULT '' COMMENT '添加人昵称',
  `product_id` bigint NOT NULL COMMENT '商品编号',
  `sku` varchar(255) NOT NULL COMMENT '规格（逗号分隔）',
  `spec_text` varchar(255) DEFAULT '' COMMENT '规格展示文本',
  `name` varchar(128) DEFAULT '' COMMENT '商品名称',
  `image` varchar(255) DEFAULT '' COMMENT '商品图片',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `number` int NOT NULL DEFAULT 1 COMMENT '数量',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_product_sku` (`session_id`, `product_id`, `sku`, `deleted`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌台共享购物车';
