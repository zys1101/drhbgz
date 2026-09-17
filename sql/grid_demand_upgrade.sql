-- ============================================================
-- 增量升级脚本：网格员增员请求表（grid_demand）
-- 适用：已导入过旧版 nep_system.sql 的 MySQL 库
-- 用法：mysql 客户端 / Navicat 中，对现有 nep_system 库直接执行本脚本
--       （幂等：不会删库/清数据，只补建缺失的表）
--
-- 背景：业务规则调整——反馈所在网格区域没有可工作的本地网格员时，
--       不允许异地指派，改为生成“增员请求”，供管理员跟进与决策者判断是否增员。
-- ============================================================
USE `nep_system`;

CREATE TABLE IF NOT EXISTS `grid_demand` (
  `demand_id` INT NOT NULL AUTO_INCREMENT COMMENT '增员请求编号',
  `province_id` INT NOT NULL COMMENT '缺员省编号',
  `city_id` INT NOT NULL COMMENT '缺员市编号',
  `af_id` INT DEFAULT NULL COMMENT '来源反馈编号(可空)',
  `reason` VARCHAR(200) DEFAULT NULL COMMENT '缺员说明',
  `state` INT NOT NULL DEFAULT 0 COMMENT '状态: 0待处理 1已处理 2已忽略',
  `apply_date` VARCHAR(10) DEFAULT NULL COMMENT '申请日期',
  `apply_time` VARCHAR(8) DEFAULT NULL COMMENT '申请时间',
  `handle_date` VARCHAR(10) DEFAULT NULL COMMENT '处理日期',
  `handle_time` VARCHAR(8) DEFAULT NULL COMMENT '处理时间',
  `handle_remark` VARCHAR(200) DEFAULT NULL COMMENT '处理说明',
  PRIMARY KEY (`demand_id`),
  KEY `idx_demand_state` (`state`),
  KEY `idx_demand_city` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格员增员请求';

-- 完成。重新启动后端即可使用 /api/gridDemand/* 接口
