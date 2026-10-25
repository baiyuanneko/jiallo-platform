package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.byn.bynspring21.entity.base.RoleType;

import java.util.List;

public class BusiAdminVo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询 LLM Provider 请求参数")
    public static class PageSysLlmProvidersVo {
        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "按 Provider 分页查询 LLM Model 请求参数")
    public static class PageSysLlmModelsByProviderIdVo {
        @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "供应商ID不能为空")
        private String providerId;

        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询 LLM Model 请求参数")
    public static class PageSysLlmModelsVo {
        @Schema(description = "供应商ID，单选筛选")
        private String providerId;

        @Schema(description = "搜索关键词（模型名称、真实模型名称、模型展示名称）", example = "gpt")
        private String keyword;

        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "新增 LLM Provider 请求参数")
    public static class AddSysLlmProviderVo {
        @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "供应商名称不能为空")
        private String providerName;

        @Schema(description = "接口基础地址")
        private String baseUrl;

        @Schema(description = "接口密钥")
        private String apiKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除 LLM Provider 请求参数")
    public static class DeleteSysLlmProviderVo {
        @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "供应商ID不能为空")
        private String providerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "修改 LLM Provider 请求参数")
    public static class UpdateSysLlmProviderVo {
        @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ID不能为空")
        private String id;

        @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "供应商名称不能为空")
        private String providerName;

        @Schema(description = "接口基础地址", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "接口基础地址不能为空")
        private String baseUrl;

        @Schema(description = "接口密钥")
        private String apiKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "添加模型到 LLM Provider 请求参数")
    public static class AddModelToSysLlmProviderVo {
        @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "供应商ID不能为空")
        private String providerId;

        @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型名称不能为空")
        private String modelName;

        @Schema(description = "真实模型名称")
        private String realModelName;

        @Schema(description = "模型展示名称")
        private String modelDisplayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除 LLM Model 请求参数")
    public static class DeleteSysLlmModelVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "测试 LLM Model 请求参数")
    public static class TestModelVo {
        @Schema(description = "模型主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ID不能为空")
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询模型可用角色请求参数")
    public static class GetModelAvailableRolesVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "模型授权角色请求参数")
    public static class AuthorizeModelToRolesVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;

        @Schema(description = "要授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "模型取消授权角色请求参数")
    public static class RevokeModelOfRolesVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;

        @Schema(description = "要取消授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "修改 LLM Model 请求参数")
    public static class UpdateSysLlmModelVo {
        @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ID不能为空")
        private String id;

        @Schema(description = "模型名称")
        private String modelName;

        @Schema(description = "真实模型名称")
        private String realModelName;

        @Schema(description = "模型展示名称")
        private String modelDisplayName;

        @Schema(description = "是否为已验证模型")
        private Boolean isVerifiedModel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设置 byn 数字分身系统提示词请求参数")
    public static class SetDigitalBynPromptVo {
        @Schema(description = "系统提示词内容，空字符串表示清除", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "prompt 不能为空")
        private String prompt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设置 Claw-like 系统提示词请求参数")
    public static class SetClawLikePromptVo {
        @Schema(description = "系统提示词内容，空字符串表示清除", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "prompt 不能为空")
        private String prompt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询 agentType 可用角色请求参数")
    public static class GetAgentTypeAvailableRolesVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "agentType 授权角色请求参数")
    public static class AuthorizeAgentTypeToRolesVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;

        @Schema(description = "要授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "agentType 取消授权角色请求参数")
    public static class RevokeAgentTypeOfRolesVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;

        @Schema(description = "要取消授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询功能模块可用角色请求参数")
    public static class GetModuleAvailableRolesVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "授权功能模块到角色请求参数")
    public static class AuthorizeModuleToRolesVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;

        @Schema(description = "要授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "取消功能模块角色授权请求参数")
    public static class RevokeModuleOfRolesVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;

        @Schema(description = "要取消授权的角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色列表不能为空")
        private List<RoleType> roleTypes;
    }

    // ==================== 用户组管理 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询用户组请求参数")
    public static class PageUserGroupsVo {
        @Schema(description = "搜索关键词（用户组名称）")
        private String keyword;

        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "20")
        @Builder.Default
        private Integer pageSize = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "创建用户组请求参数")
    public static class CreateUserGroupVo {
        @Schema(description = "用户组名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户组名称不能为空")
        private String groupName;

        @Schema(description = "用户组描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "修改用户组请求参数")
    public static class UpdateUserGroupVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户组ID不能为空")
        private String groupId;

        @Schema(description = "用户组名称")
        private String groupName;

        @Schema(description = "用户组描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除用户组请求参数")
    public static class DeleteUserGroupVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户组ID不能为空")
        private String groupId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "添加用户组成员请求参数")
    public static class AddMembersToGroupVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户组ID不能为空")
        private String groupId;

        @Schema(description = "要添加的用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户列表不能为空")
        private List<String> userIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "移除用户组成员请求参数")
    public static class RemoveMembersFromGroupVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "用户组ID不能为空")
        private String groupId;

        @Schema(description = "要移除的用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户列表不能为空")
        private List<String> userIds;
    }

    // ==================== 模型-用户组授权 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询模型可用用户组请求参数")
    public static class GetModelAvailableGroupsVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "模型授权用户组请求参数")
    public static class AuthorizeModelToGroupsVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;

        @Schema(description = "要授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "模型取消授权用户组请求参数")
    public static class RevokeModelOfGroupsVo {
        @Schema(description = "模型ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "模型ID不能为空")
        private String modelId;

        @Schema(description = "要取消授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    // ==================== AgentType-用户组授权 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询 agentType 可用用户组请求参数")
    public static class GetAgentTypeAvailableGroupsVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "agentType 授权用户组请求参数")
    public static class AuthorizeAgentTypeToGroupsVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;

        @Schema(description = "要授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "agentType 取消授权用户组请求参数")
    public static class RevokeAgentTypeOfGroupsVo {
        @Schema(description = "agentType", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "agentType 不能为空")
        private Integer agentType;

        @Schema(description = "要取消授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    // ==================== 功能模块-用户组授权 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询功能模块可用用户组请求参数")
    public static class GetModuleAvailableGroupsVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "授权功能模块到用户组请求参数")
    public static class AuthorizeModuleToGroupsVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;

        @Schema(description = "要授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "取消功能模块用户组授权请求参数")
    public static class RevokeModuleOfGroupsVo {
        @Schema(description = "功能模块标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "moduleCode 不能为空")
        private String moduleCode;

        @Schema(description = "要取消授权的用户组ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "用户组列表不能为空")
        private List<String> groupIds;
    }

    // ==================== Claw-like 安全级别 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 角色安全级别项")
    public static class ClawRoleSecurityLevelVo {
        @Schema(description = "角色代码，对应 RoleType 枚举值")
        private Integer roleCode;

        @Schema(description = "安全级别：1-工作区文件，2-完全访问")
        private Integer securityLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设置 Claw-like 角色安全级别请求参数")
    public static class SetClawRoleSecurityLevelVo {
        @Schema(description = "角色代码，对应 RoleType 枚举值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "roleCode 不能为空")
        private Integer roleCode;

        @Schema(description = "安全级别：1-工作区文件，2-完全访问", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "securityLevel 不能为空")
        private Integer securityLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "移除 Claw-like 角色安全级别请求参数")
    public static class RemoveClawRoleSecurityLevelVo {
        @Schema(description = "角色代码，对应 RoleType 枚举值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "roleCode 不能为空")
        private Integer roleCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Claw-like 用户组安全级别项")
    public static class ClawGroupSecurityLevelVo {
        @Schema(description = "用户组ID")
        private String groupId;

        @Schema(description = "安全级别：1-工作区文件，2-完全访问")
        private Integer securityLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设置 Claw-like 用户组安全级别请求参数")
    public static class SetClawGroupSecurityLevelVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "groupId 不能为空")
        private String groupId;

        @Schema(description = "安全级别：1-工作区文件，2-完全访问", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "securityLevel 不能为空")
        private Integer securityLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "移除 Claw-like 用户组安全级别请求参数")
    public static class RemoveClawGroupSecurityLevelVo {
        @Schema(description = "用户组ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "groupId 不能为空")
        private String groupId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Telegram 机器人配置")
    public static class TgBotConfigVo {
        @Schema(description = "是否启用机器人", example = "false")
        private Boolean enabled;

        @Schema(description = "机器人 Token（查询时打码返回；保存时传回打码值表示不修改）")
        private String token;

        @Schema(description = "ChatId 处理模式：whitelist-白名单，blacklist-黑名单", example = "whitelist")
        private String chatIdMode;

        @Schema(description = "白名单 ChatId 列表（逗号分隔）")
        private String whitelistChatIds;

        @Schema(description = "黑名单 ChatId 列表（逗号分隔）")
        private String blacklistChatIds;

        @Schema(description = "功能：消息记录（写入 tg_chat_log）", example = "false")
        private Boolean chatLogEnabled;

        @Schema(description = "功能：网络搜索 /websearch", example = "false")
        private Boolean webSearchEnabled;

        @Schema(description = "功能：智能问答 /chat", example = "false")
        private Boolean chatEnabled;

        @Schema(description = "功能：活跃排行 /rank（依赖消息记录）", example = "false")
        private Boolean rankEnabled;

        @Schema(description = "/chat 使用的模型 ID")
        private String chatModelId;

        @Schema(description = "/chat 自定义系统提示词，为空使用内置提示词")
        private String chatSystemPrompt;

        @Schema(description = "机器人名称（/help、/about 中展示），为空使用默认值", example = "Telegram 智能机器人")
        private String botName;

        @Schema(description = "机器人更新日期（/help、/about 中展示），为空使用默认值", example = "2026-10-24")
        private String botUpdateDate;

        @Schema(description = "ChatID 特定配置：按聊天覆盖全局功能开关")
        @Valid
        private List<TgBotChatFeatureOverrideVo> chatFeatureOverrides;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Telegram 机器人 ChatID 特定功能配置：各功能为 null 表示跟随全局，true/false 表示对该聊天强制开启/关闭")
    public static class TgBotChatFeatureOverrideVo {
        @Schema(description = "ChatID（字符串，避免前端数值精度问题）", example = "-1001234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ChatID 不能为空")
        @Pattern(regexp = "^\\s*-?\\d{1,15}\\s*$", message = "ChatID 必须为整数")
        private String chatId;

        @Schema(description = "备注名")
        private String remark;

        @Schema(description = "消息记录：null-跟随全局，true-开启，false-关闭")
        private Boolean chatLogEnabled;

        @Schema(description = "网络搜索：null-跟随全局，true-开启，false-关闭")
        private Boolean webSearchEnabled;

        @Schema(description = "智能问答：null-跟随全局，true-开启，false-关闭")
        private Boolean chatEnabled;

        @Schema(description = "活跃排行：null-跟随全局，true-开启，false-关闭（依赖该聊天最终生效的消息记录）")
        private Boolean rankEnabled;
    }
}
