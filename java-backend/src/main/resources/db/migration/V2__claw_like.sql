-- ----------------------------
-- Claw-like 应用（agentType=3）种子数据：系统提示词 + 功能模块授权（默认授予 ADMIN）
-- 幂等写法：已存在则跳过。手动执行过该迁移的库后续启用 Flyway 也不会重复插入
-- （同表子查询用派生表包裹，规避 MySQL 对 INSERT...SELECT 同表引用的限制）
-- ----------------------------

INSERT INTO `sys_config` (`id`, `is_del`, `create_time`, `update_time`, `config_key`, `config_value`, `config_instruction`)
SELECT '2', 0, NOW(), NOW(), 'clawLikeSystemPrompt', '你是 Claw-like 编码智能体，运行在服务端的隔离工作区中。你可以使用 execute_shell_command（在工作区内执行 shell 命令）、read_file（读取文件）、write_file（写入文件）、edit_file（精确编辑文件）、list_files（列出目录）、search_files（递归搜索内容）六个工具来完成编码与文件操作任务。所有文件操作均被限定在当前工作区目录内，工作区路径由系统自动注入，工具中的 path 参数均为相对于工作区的相对路径，不要尝试访问工作区之外的文件。修改文件前先用 read_file 确认实际内容，执行命令前先了解工作区结构，完成任务后简要说明所做的变更。', 'Claw-like 应用（agentType=3）的系统提示词'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT `config_key` FROM `sys_config`) AS existing
    WHERE existing.`config_key` = 'clawLikeSystemPrompt'
);

INSERT INTO `feature_module_availability` (`id`, `is_del`, `create_time`, `update_time`, `module_code`, `role_code`)
SELECT '1', 0, NOW(), NOW(), 'claw_like', 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT `module_code`, `role_code` FROM `feature_module_availability`) AS existing
    WHERE existing.`module_code` = 'claw_like' AND existing.`role_code` = 1
);
