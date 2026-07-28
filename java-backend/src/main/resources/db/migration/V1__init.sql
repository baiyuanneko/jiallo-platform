/*
 Navicat Premium Dump SQL

 Source Server         : 本地mysql
 Source Server Type    : MySQL
 Source Server Version : 90700 (9.7.0)
 Source Host           : localhost:3306
 Source Schema         : jiallo

 Target Server Type    : MySQL
 Target Server Version : 90700 (9.7.0)
 File Encoding         : 65001

 Date: 14/06/2026 00:07:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_type_availability
-- ----------------------------
DROP TABLE IF EXISTS `agent_type_availability`;
CREATE TABLE `agent_type_availability` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `agent_type` int DEFAULT NULL,
  `role_code` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_agent_type_availability_agent_type` (`agent_type`,`is_del`),
  KEY `idx_agent_type_availability_role_code` (`role_code`,`is_del`),
  KEY `idx_agent_type_availability_agent_role` (`agent_type`,`role_code`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AgentType availability by role';

-- ----------------------------
-- Table structure for agent_type_group_availability
-- ----------------------------
DROP TABLE IF EXISTS `agent_type_group_availability`;
CREATE TABLE `agent_type_group_availability` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-正常，1-删除',
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `agent_type` int NOT NULL COMMENT '智能体类型',
  `group_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户组ID',
  PRIMARY KEY (`id`),
  KEY `idx_atga_agent_type` (`agent_type`,`is_del`),
  KEY `idx_atga_group_id` (`group_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体-用户组授权';

-- ----------------------------
-- Table structure for agentic_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `agentic_chat_message`;
CREATE TABLE `agentic_chat_message` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `message_type` int DEFAULT NULL,
  `text_content` mediumtext,
  `media_content` mediumtext,
  `prompt_token_count` bigint DEFAULT NULL,
  `completion_token_count` bigint DEFAULT NULL,
  `message_index` int DEFAULT NULL,
  `tool_content` mediumtext,
  `reasoning_content` mediumtext,
  `structured_tool_content` mediumtext,
  `cached_token_count` bigint DEFAULT NULL,
  `reasoning_token_count` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_agentic_chat_message_session_id` (`session_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agentic chat message';

-- ----------------------------
-- Table structure for agentic_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `agentic_chat_session`;
CREATE TABLE `agentic_chat_session` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `session_name` varchar(255) DEFAULT NULL,
  `model_id` varchar(64) DEFAULT NULL,
  `model_type` int DEFAULT NULL,
  `agent_type` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_agentic_chat_session_model_id` (`model_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agentic chat session';

-- ----------------------------
-- Table structure for agentic_chat_shared_message
-- ----------------------------
DROP TABLE IF EXISTS `agentic_chat_shared_message`;
CREATE TABLE `agentic_chat_shared_message` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `message_index` int DEFAULT NULL,
  `message_type` int DEFAULT NULL,
  `text_content` mediumtext,
  `media_content` mediumtext,
  `prompt_token_count` bigint DEFAULT NULL,
  `completion_token_count` bigint DEFAULT NULL,
  `cached_token_count` bigint DEFAULT NULL,
  `reasoning_token_count` bigint DEFAULT NULL,
  `tool_content` mediumtext,
  `reasoning_content` mediumtext,
  `structured_tool_content` mediumtext,
  PRIMARY KEY (`id`),
  KEY `idx_shared_message_session_id` (`session_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agentic chat shared message (分享的会话消息)';

-- ----------------------------
-- Table structure for agentic_chat_shared_session
-- ----------------------------
DROP TABLE IF EXISTS `agentic_chat_shared_session`;
CREATE TABLE `agentic_chat_shared_session` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `session_name` varchar(255) DEFAULT NULL,
  `model_id` varchar(64) DEFAULT NULL,
  `model_type` int DEFAULT NULL,
  `agent_type` int DEFAULT NULL,
  `original_session_id` varchar(64) DEFAULT NULL,
  `share_text_content_only` tinyint(1) DEFAULT NULL COMMENT '是否仅分享文本内容（NULL视为true：过滤reasoningContent/toolContent/structuredToolContent）',
  PRIMARY KEY (`id`),
  KEY `idx_shared_session_original_id` (`original_session_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agentic chat shared session (分享的会话)';

-- ----------------------------
-- Table structure for chatsess_enabled_tool
-- ----------------------------
DROP TABLE IF EXISTS `chatsess_enabled_tool`;
CREATE TABLE `chatsess_enabled_tool` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `tool_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chatsess_enabled_tool_session_id` (`session_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话启用的工具关联表';

-- ----------------------------
-- Table structure for feature_module_availability
-- ----------------------------
DROP TABLE IF EXISTS `feature_module_availability`;
CREATE TABLE `feature_module_availability` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `module_code` varchar(64) NOT NULL COMMENT '功能模块标识，如 rag_knowledge_base',
  `role_code` int DEFAULT NULL COMMENT '角色代码，对应 RoleType 枚举值',
  PRIMARY KEY (`id`),
  KEY `idx_fma_module_code` (`module_code`,`is_del`),
  KEY `idx_fma_role_code` (`role_code`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Feature module availability by role';

-- ----------------------------
-- Table structure for feature_module_group_availability
-- ----------------------------
DROP TABLE IF EXISTS `feature_module_group_availability`;
CREATE TABLE `feature_module_group_availability` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-正常，1-删除',
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `module_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '功能模块标识',
  `group_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户组ID',
  PRIMARY KEY (`id`),
  KEY `idx_fmga_module_code` (`module_code`,`is_del`),
  KEY `idx_fmga_group_id` (`group_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能模块-用户组授权';

-- ----------------------------
-- Table structure for rag_doc_chunk
-- ----------------------------
DROP TABLE IF EXISTS `rag_doc_chunk`;
CREATE TABLE `rag_doc_chunk` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `doc_id` varchar(64) NOT NULL COMMENT '所属文档ID',
  `library_id` varchar(64) DEFAULT NULL COMMENT '所属知识库ID',
  `chunk_index` int NOT NULL COMMENT '分块序号（从0开始）',
  `chunk_content` text COMMENT '分块内容',
  PRIMARY KEY (`id`),
  KEY `idx_rag_doc_chunk_doc_id` (`doc_id`,`is_del`),
  KEY `idx_rag_doc_chunk_library_id` (`library_id`,`is_del`),
  FULLTEXT KEY `ft_rag_doc_chunk_content` (`chunk_content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG 文档分块';

-- ----------------------------
-- Table structure for rag_library
-- ----------------------------
DROP TABLE IF EXISTS `rag_library`;
CREATE TABLE `rag_library` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `name` varchar(255) NOT NULL COMMENT '知识库名称',
  `description` text COMMENT '知识库描述',
  `doc_count` int NOT NULL DEFAULT '0' COMMENT '文档数量',
  `total_file_size` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG 知识库';

-- ----------------------------
-- Table structure for rag_library_doc
-- ----------------------------
DROP TABLE IF EXISTS `rag_library_doc`;
CREATE TABLE `rag_library_doc` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `rag_library_id` varchar(64) NOT NULL COMMENT '所属知识库ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL,
  `chunk_num` bigint NOT NULL DEFAULT '0' COMMENT '分块数量',
  `parsed` tinyint(1) DEFAULT NULL COMMENT '是否已解析',
  PRIMARY KEY (`id`),
  KEY `idx_rag_library_doc_library_id` (`rag_library_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG 知识库文档';

-- ----------------------------
-- Table structure for sso_client
-- ----------------------------
DROP TABLE IF EXISTS `sso_client`;
CREATE TABLE `sso_client` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `client_unique_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_desc` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_website` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_author_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_redirect_uri` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_permission_type` int DEFAULT NULL,
  `silent_redirect` tinyint(1) DEFAULT NULL,
  `client_icon_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sso_client_unique_name` (`client_unique_name`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `config_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci,
  `config_instruction` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sys_config_key` (`config_key`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- ----------------------------
-- Table structure for sys_llm_model
-- ----------------------------
DROP TABLE IF EXISTS `sys_llm_model`;
CREATE TABLE `sys_llm_model` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `provider_id` varchar(64) DEFAULT NULL,
  `model_name` varchar(255) DEFAULT NULL,
  `real_model_name` varchar(255) DEFAULT NULL,
  `model_display_name` varchar(255) DEFAULT NULL,
  `model_icon_url` varchar(255) DEFAULT NULL,
  `is_verified_model` tinyint(1) DEFAULT NULL COMMENT '是否为已验证模型',
  PRIMARY KEY (`id`),
  KEY `idx_sys_llm_model_provider_id` (`provider_id`,`is_del`),
  KEY `idx_sys_llm_model_name` (`model_name`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='LLM model';

-- ----------------------------
-- Table structure for sys_llm_provider
-- ----------------------------
DROP TABLE IF EXISTS `sys_llm_provider`;
CREATE TABLE `sys_llm_provider` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `provider_name` varchar(255) DEFAULT NULL,
  `base_url` varchar(1000) DEFAULT NULL,
  `api_key` varchar(1000) DEFAULT NULL,
  `provider_icon_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sys_llm_provider_name` (`provider_name`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='LLM provider';

-- ----------------------------
-- Table structure for sys_model_availability
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_availability`;
CREATE TABLE `sys_model_availability` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `model_id` varchar(64) DEFAULT NULL,
  `role_code` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sys_model_availability_model_id` (`model_id`,`is_del`),
  KEY `idx_sys_model_availability_role_code` (`role_code`,`is_del`),
  KEY `idx_sys_model_availability_model_role` (`model_id`,`role_code`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Model availability by role';

-- ----------------------------
-- Table structure for sys_model_group_availability
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_group_availability`;
CREATE TABLE `sys_model_group_availability` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-正常，1-删除',
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `model_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型ID',
  `group_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户组ID',
  PRIMARY KEY (`id`),
  KEY `idx_smga_model_id` (`model_id`,`is_del`),
  KEY `idx_smga_group_id` (`group_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型-用户组授权';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) DEFAULT NULL COMMENT '0-正常，1-删除',
  `create_user` varchar(64) DEFAULT NULL COMMENT '用户创建人',
  `update_user` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `username` varchar(50) DEFAULT NULL COMMENT 'username',
  `display_name` varchar(100) DEFAULT NULL COMMENT 'displayName',
  `password` varchar(500) DEFAULT NULL COMMENT 'password',
  `password_updated_at` datetime DEFAULT NULL COMMENT 'passwordUpdatedAt',
  `email` varchar(100) DEFAULT NULL COMMENT 'email',
  `email_verified` tinyint(1) DEFAULT NULL COMMENT 'emailVerified',
  `mobile` varchar(50) DEFAULT NULL COMMENT 'mobile',
  `mobile_verified` tinyint(1) DEFAULT NULL COMMENT 'mobileVerified',
  `role_type` int DEFAULT NULL COMMENT 'roleType',
  `banned` tinyint(1) DEFAULT NULL COMMENT 'banned',
  `avatar_url` varchar(500) DEFAULT NULL,
  `external_user_id` varchar(64) DEFAULT NULL,
  `preferred_model_id` varchar(64) DEFAULT NULL,
  `preferred_model_type` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_username` (`username`,`is_del`),
  KEY `idx_user_email` (`email`,`is_del`),
  KEY `idx_user_email_verified` (`email`,`email_verified`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user';

-- ----------------------------
-- Table structure for user_group
-- ----------------------------
DROP TABLE IF EXISTS `user_group`;
CREATE TABLE `user_group` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-正常，1-删除',
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `group_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户组名称',
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户组描述',
  PRIMARY KEY (`id`),
  KEY `idx_user_group_name` (`group_name`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组';

-- ----------------------------
-- Table structure for user_group_member
-- ----------------------------
DROP TABLE IF EXISTS `user_group_member`;
CREATE TABLE `user_group_member` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-正常，1-删除',
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `group_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户组ID',
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`),
  KEY `idx_ugm_group_id` (`group_id`,`is_del`),
  KEY `idx_ugm_user_id` (`user_id`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组成员';

-- ----------------------------
-- Table structure for user_log
-- ----------------------------
DROP TABLE IF EXISTS `user_log`;
CREATE TABLE `user_log` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `is_del` tinyint(1) DEFAULT NULL COMMENT '0-正常，1-删除',
  `create_user` varchar(64) DEFAULT NULL COMMENT '用户创建人',
  `update_user` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `user_id` varchar(64) DEFAULT NULL COMMENT 'userId',
  `ip` varchar(100) DEFAULT NULL COMMENT 'ip',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'userAgent',
  `behaviour` int DEFAULT NULL COMMENT 'behaviour',
  `message` varchar(255) DEFAULT NULL COMMENT 'message',
  PRIMARY KEY (`id`),
  KEY `idx_user_log_user_id_create_time` (`user_id`,`is_del`,`create_time`),
  KEY `idx_user_log_behaviour_user_id_create_time` (`behaviour`,`user_id`,`is_del`,`create_time`),
  KEY `idx_user_log_behaviour_ip_create_time` (`behaviour`,`ip`,`is_del`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user_log';

-- ----------------------------
-- Table structure for user_memory
-- ----------------------------
DROP TABLE IF EXISTS `user_memory`;
CREATE TABLE `user_memory` (
  `id` varchar(64) NOT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `create_user` varchar(64) DEFAULT NULL,
  `update_user` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `storage_type` int NOT NULL COMMENT '0-默认 1-byn数字分身',
  `content` text COMMENT '记忆内容(最多8192字符)',
  PRIMARY KEY (`id`),
  KEY `idx_user_memory_user_type` (`create_user`,`storage_type`,`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_session
-- ----------------------------
DROP TABLE IF EXISTS `user_session`;
CREATE TABLE `user_session` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `update_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_del` tinyint(1) DEFAULT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `jwt_access_token` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `jwt_refresh_token` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token_expire_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_session_user_id_expire_time` (`user_id`,`is_del`,`refresh_token_expire_time`),
  KEY `idx_user_session_refresh_token` (`jwt_refresh_token`(191),`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` (`id`, `is_del`, `create_time`, `update_time`, `config_key`, `config_value`, `config_instruction`)
VALUES ('1', 0, NOW(), NOW(), 'allowRegister', 'false', '是否允许用户注册（默认关闭，由管理员在后台开启）');

SET FOREIGN_KEY_CHECKS = 1;
