-- ----------------------------
-- Claw-like 应用（agentType=3）安全级别：角色/用户组级别授权表 + 种子数据（默认授予 ADMIN 完全访问）
-- 安全级别：0-无权限（纯聊天，无工具），1-工作区文件（仅文件工具且限定工作区），2-完全访问（全部工具且不限工作区）
-- 生效级别取角色授权与所有用户组授权中的最高值，无授权记录视为 0
-- ----------------------------

-- ----------------------------
-- Table structure for claw_like_security_level
-- ----------------------------
CREATE TABLE `claw_like_security_level` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `role_code` int NOT NULL COMMENT '角色代码，对应 RoleType 枚举值',
  `security_level` int NOT NULL COMMENT '安全级别：1-工作区文件，2-完全访问',
  PRIMARY KEY (`id`),
  KEY `idx_clsl_role_code` (`role_code`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Claw-like 安全级别-角色授权';

-- ----------------------------
-- Table structure for claw_like_security_level_group
-- ----------------------------
CREATE TABLE `claw_like_security_level_group` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `group_id` varchar(64) NOT NULL COMMENT '用户组ID',
  `security_level` int NOT NULL COMMENT '安全级别：1-工作区文件，2-完全访问',
  PRIMARY KEY (`id`),
  KEY `idx_clslg_group_id` (`group_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Claw-like 安全级别-用户组授权';

-- ----------------------------
-- 种子数据：ADMIN 角色默认授予完全访问（FULL）
-- 幂等写法：已存在则跳过（同表子查询用派生表包裹，规避 MySQL 对 INSERT...SELECT 同表引用的限制）
-- ----------------------------
INSERT INTO `claw_like_security_level` (`id`, `is_del`, `create_time`, `update_time`, `role_code`, `security_level`)
SELECT '1', 0, NOW(), NOW(), 1, 2
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT `role_code` FROM `claw_like_security_level`) AS existing
    WHERE existing.`role_code` = 1
);
