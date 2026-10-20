-- ============================================================
-- 多商户多租户改造（共享库 + tenant_id）
-- 执行前请备份数据库！
-- 执行：mysql -uroot -p yixiang-drink-open < sql/multi-tenant.sql
-- ============================================================

DROP PROCEDURE IF EXISTS ensure_tenant_column;
DELIMITER $$
CREATE PROCEDURE ensure_tenant_column(IN tbl VARCHAR(64))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = 'tenant_id'
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT ''租户编号''');
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = 'idx_tenant_id'
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `idx_tenant_id` (`tenant_id`)');
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  SET @s = CONCAT('UPDATE `', tbl, '` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL OR `tenant_id` = 0');
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
END$$
DELIMITER ;

-- 1. 原先缺少 tenant_id 的业务表
CALL ensure_tenant_column('yshop_store_shop');
CALL ensure_tenant_column('yshop_store_desk');
CALL ensure_tenant_column('yshop_store_desk_qr');
CALL ensure_tenant_column('yshop_store_desk_session');
CALL ensure_tenant_column('yshop_store_desk_cart');
CALL ensure_tenant_column('yshop_coupon');
CALL ensure_tenant_column('yshop_coupon_user');
CALL ensure_tenant_column('yshop_order_number');
CALL ensure_tenant_column('yshop_shop_ads');
CALL ensure_tenant_column('yshop_material_group');
CALL ensure_tenant_column('yshop_service');
CALL ensure_tenant_column('yshop_wechat_template');
CALL ensure_tenant_column('yshop_store_product_attr');
CALL ensure_tenant_column('yshop_store_product_attr_result');
CALL ensure_tenant_column('yshop_store_product_attr_value');
CALL ensure_tenant_column('yshop_store_order_cart_info');
CALL ensure_tenant_column('yshop_store_order_status');
CALL ensure_tenant_column('yshop_shipping_templates_free');
CALL ensure_tenant_column('yshop_shipping_templates_region');

-- 2. 已有 tenant_id 的表：规范化 NULL/0 到 1
CALL ensure_tenant_column('yshop_user');
CALL ensure_tenant_column('yshop_user_address');
CALL ensure_tenant_column('yshop_user_bill');
CALL ensure_tenant_column('yshop_store_order');
CALL ensure_tenant_column('yshop_store_product');
CALL ensure_tenant_column('yshop_store_product_brand');
CALL ensure_tenant_column('yshop_store_product_category');
CALL ensure_tenant_column('yshop_store_product_relation');
CALL ensure_tenant_column('yshop_store_product_reply');
CALL ensure_tenant_column('yshop_store_product_rule');
CALL ensure_tenant_column('yshop_material');
CALL ensure_tenant_column('yshop_score_order');
CALL ensure_tenant_column('yshop_score_product');
CALL ensure_tenant_column('yshop_shipping_templates');

-- 3. 系统与基础设施表：规范化到租户 1
CALL ensure_tenant_column('system_users');
CALL ensure_tenant_column('system_role');
CALL ensure_tenant_column('system_role_menu');
CALL ensure_tenant_column('system_user_role');
CALL ensure_tenant_column('system_user_post');
CALL ensure_tenant_column('system_dept');
CALL ensure_tenant_column('system_post');
CALL ensure_tenant_column('system_login_log');
CALL ensure_tenant_column('system_operate_log');
CALL ensure_tenant_column('system_notice');
CALL ensure_tenant_column('system_notify_message');
CALL ensure_tenant_column('system_oauth2_access_token');
CALL ensure_tenant_column('system_oauth2_approve');
CALL ensure_tenant_column('system_oauth2_code');
CALL ensure_tenant_column('system_oauth2_refresh_token');
CALL ensure_tenant_column('system_sms_code');
CALL ensure_tenant_column('system_social_user');
CALL ensure_tenant_column('system_social_user_bind');
CALL ensure_tenant_column('infra_api_access_log');
CALL ensure_tenant_column('infra_api_error_log');
CALL ensure_tenant_column('mp_account');
CALL ensure_tenant_column('mp_auto_reply');
CALL ensure_tenant_column('mp_material');
CALL ensure_tenant_column('mp_menu');
CALL ensure_tenant_column('mp_message');
CALL ensure_tenant_column('mp_tag');
CALL ensure_tenant_column('mp_user');

DROP PROCEDURE IF EXISTS ensure_tenant_column;

-- 4. 子表按父表回填租户
UPDATE `yshop_store_product_attr` a
  JOIN `yshop_store_product` p ON a.product_id = p.id
  SET a.tenant_id = p.tenant_id;

UPDATE `yshop_store_product_attr_result` a
  JOIN `yshop_store_product` p ON a.product_id = p.id
  SET a.tenant_id = p.tenant_id;

UPDATE `yshop_store_product_attr_value` a
  JOIN `yshop_store_product` p ON a.product_id = p.id
  SET a.tenant_id = p.tenant_id;

UPDATE `yshop_store_order_cart_info` c
  JOIN `yshop_store_order` o ON c.oid = o.id
  SET c.tenant_id = o.tenant_id;

UPDATE `yshop_store_order_status` s
  JOIN `yshop_store_order` o ON s.oid = o.id
  SET s.tenant_id = o.tenant_id;

UPDATE `yshop_shipping_templates_free` f
  JOIN `yshop_shipping_templates` t ON f.temp_id = t.id
  SET f.tenant_id = t.tenant_id;

UPDATE `yshop_shipping_templates_region` r
  JOIN `yshop_shipping_templates` t ON r.temp_id = t.id
  SET r.tenant_id = t.tenant_id;
