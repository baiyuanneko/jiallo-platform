-- ----------------------------
-- Telegram 机器人聊天记录（设计参考 cyberbynn）：
-- 仅在后台开启「消息记录」功能时，记录通过 ChatId 黑白名单过滤的消息（含机器人自身回复）
-- 消息日志只增不改，不继承 CommonEntity（无 is_del 等通用字段）
-- ----------------------------

-- ----------------------------
-- Table structure for tg_chat_log
-- ----------------------------
CREATE TABLE `tg_chat_log` (
  `id` varchar(64) NOT NULL COMMENT '雪花 ID（字符串存储）',
  `chat_type` varchar(16) NOT NULL COMMENT 'private/group/supergroup 等',
  `chat_id` bigint NOT NULL,
  `sender_id` bigint DEFAULT NULL COMMENT '匿名/频道代发/机器人自身回复时为空',
  `sender_name` varchar(255) DEFAULT NULL,
  `sender_is_bot` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=发送者为机器人（含其他机器人）',
  `send_time` datetime(3) NOT NULL COMMENT '消息发送时间（Asia/Shanghai）',
  `content` text NOT NULL COMMENT '原始消息内容（文本或媒体占位符），特殊行为 [event]/[edited] 标记',
  `is_special_message` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=服务事件或编辑消息',
  `event_content` text DEFAULT NULL COMMENT '特殊消息描述：事件文案或编辑后的新内容',
  `message_id` bigint DEFAULT NULL COMMENT 'Telegram message_id，用于编辑消息关联',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_tcl_chat_time` (`chat_id`,`send_time`),
  KEY `idx_tcl_chat_sender_time` (`chat_id`,`sender_id`,`send_time`),
  KEY `idx_tcl_send_time` (`send_time`),
  KEY `idx_tcl_msg_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Telegram 聊天记录';

-- ----------------------------
-- Table structure for tg_chat_log_chunk
-- 全文检索分块表（ngram 分词，写入即索引；特殊消息的可检索文本为 event_content）
-- ----------------------------
CREATE TABLE `tg_chat_log_chunk` (
  `id` varchar(64) NOT NULL COMMENT '雪花 ID（字符串存储）',
  `log_id` varchar(64) NOT NULL COMMENT '对应 tg_chat_log.id',
  `chunk_index` int NOT NULL,
  `chunk_content` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tclc_log_id` (`log_id`),
  FULLTEXT KEY `ft_tclc_chunk_content` (`chunk_content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Telegram 聊天记录全文检索分块';
