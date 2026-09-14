-- ============================================================
-- 人员管理（HR）模块 增量升级脚本
-- 适用：已导入旧版 nep_system.sql 的 MySQL 库（升级前缺 leave 请假表）
-- 用法：mysql 客户端 / Navicat 中，对现有 nep_system 库直接执行本脚本
--       （不会删库/清数据，只补 leave 表结构与示例请假数据）
-- ============================================================
USE `nep_system`;

CREATE TABLE IF NOT EXISTS `leave` (
  `leave_id` INT NOT NULL AUTO_INCREMENT COMMENT '请假编号',
  `emp_id` INT NOT NULL COMMENT '请假网格员编号(employee.emp_id)',
  `reason` VARCHAR(200) NOT NULL COMMENT '请假事由',
  `start_date` VARCHAR(10) NOT NULL COMMENT '开始日期',
  `end_date` VARCHAR(10) NOT NULL COMMENT '结束日期',
  `state` INT NOT NULL DEFAULT 0 COMMENT '状态: 0待审批 1已同意(请假中) 2已驳回 3已销假',
  `apply_date` VARCHAR(10) DEFAULT NULL COMMENT '申请日期',
  `apply_time` VARCHAR(8) DEFAULT NULL COMMENT '申请时间',
  `approve_date` VARCHAR(10) DEFAULT NULL COMMENT '审批/销假日期',
  `approve_time` VARCHAR(8) DEFAULT NULL COMMENT '审批/销假时间',
  PRIMARY KEY (`leave_id`),
  KEY `idx_leave_emp` (`emp_id`),
  KEY `idx_leave_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格员请假表';

-- 示例请假数据（仅在表为空时插入，避免覆盖已有记录）
INSERT INTO `leave` (`leave_id`,`emp_id`,`reason`,`start_date`,`end_date`,`state`,`apply_date`,`apply_time`,`approve_date`,`approve_time`)
SELECT * FROM (SELECT 1,2,'老家有事，需回家处理','2026-09-15','2026-09-17',0,'2026-09-11','09:12:00',NULL,NULL) AS t
WHERE NOT EXISTS (SELECT 1 FROM `leave` LIMIT 1);
INSERT INTO `leave` (`leave_id`,`emp_id`,`reason`,`start_date`,`end_date`,`state`,`apply_date`,`apply_time`,`approve_date`,`approve_time`)
SELECT * FROM (SELECT 2,4,'身体不适，医院就诊','2026-09-18','2026-09-19',0,'2026-09-11','14:30:00',NULL,NULL) AS t
WHERE (SELECT COUNT(*) FROM `leave`) = 1;
INSERT INTO `leave` (`leave_id`,`emp_id`,`reason`,`start_date`,`end_date`,`state`,`apply_date`,`apply_time`,`approve_date`,`approve_time`)
SELECT * FROM (SELECT 3,5,'病假休养（已同意，未销假，处于请假状态）','2026-09-08','2026-09-14',1,'2026-09-05','10:20:00','2026-09-06','09:00:00') AS t
WHERE (SELECT COUNT(*) FROM `leave`) = 2;
INSERT INTO `leave` (`leave_id`,`emp_id`,`reason`,`start_date`,`end_date`,`state`,`apply_date`,`apply_time`,`approve_date`,`approve_time`)
SELECT * FROM (SELECT 4,7,'婚假（已销假）','2026-07-01','2026-07-05',3,'2026-06-28','08:40:00','2026-07-06','08:30:00') AS t
WHERE (SELECT COUNT(*) FROM `leave`) = 3;
INSERT INTO `leave` (`leave_id`,`emp_id`,`reason`,`start_date`,`end_date`,`state`,`apply_date`,`apply_time`,`approve_date`,`approve_time`)
SELECT * FROM (SELECT 5,8,'个人事务请假（已驳回）','2026-06-10','2026-06-11',2,'2026-06-08','16:05:00','2026-06-09','10:30:00') AS t
WHERE (SELECT COUNT(*) FROM `leave`) = 4;
-- 让自增主键继续
SET @m = (SELECT IFNULL(MAX(leave_id), 0) FROM `leave`);
ALTER TABLE `leave` AUTO_INCREMENT = @m + 1;

-- 完成。重新启动后端即可在“人员管理-网格员管理”正常加载
