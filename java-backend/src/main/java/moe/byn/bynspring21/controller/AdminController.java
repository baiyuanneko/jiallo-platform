package moe.byn.bynspring21.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.SysConfig;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.SysLlmProvider;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserGroup;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.dto.AdminDto;
import moe.byn.bynspring21.entity.vo.AdminVo;
import moe.byn.bynspring21.entity.vo.BusiAdminVo;
import moe.byn.bynspring21.service.AdminService;
import moe.byn.bynspring21.service.BusiAdminService;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "管理员管理", description = "管理员用户管理接口（仅 ADMIN 角色可访问）")
@RestController
@RequestMapping("admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private BusiAdminService busiAdminService;

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Operation(
            summary = "查询用户列表",
            description = "分页查询用户列表，支持关键词搜索和多条件筛选",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/list")
    public R<Page<User>> listUsers(@Validated @RequestBody AdminVo.QueryUsersVo vo) {
        return R.ok(adminService.queryUsers(vo));
    }

    @Operation(
            summary = "查看用户详情",
            description = "获取用户完整信息，包括会话、日志统计等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("users/{userId}")
    public R<AdminDto.UserDetailResponse> getUserDetail(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId) {
        return R.ok(adminService.getUserDetail(userId));
    }

    @Operation(
            summary = "创建用户",
            description = "管理员创建新用户，系统自动生成随机密码",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/create")
    public R<AdminDto.CreateUserResponse> createUser(@Validated @RequestBody AdminVo.CreateUserVo vo) {
        return R.ok(adminService.createUser(vo));
    }

    @Operation(
            summary = "修改用户信息",
            description = "修改用户基本信息、角色、邮箱验证状态等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/update")
    public R<User> updateUser(@Validated @RequestBody AdminVo.UpdateUserVo vo) {
        return R.ok(adminService.updateUser(vo));
    }

    @Operation(
            summary = "删除用户",
            description = "逻辑删除用户（设置 is_del=1），并失效所有 Token",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/delete")
    public R<Void> deleteUser(@Validated @RequestBody AdminVo.DeleteUserVo vo) {
        adminService.deleteUser(vo);
        return R.ok();
    }

    @Operation(
            summary = "封禁/解封用户",
            description = "修改用户封禁状态，封禁时立即失效所有 Token",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/ban")
    public R<Void> banUser(@Validated @RequestBody AdminVo.BanUserVo vo) {
        adminService.banUser(vo);
        return R.ok();
    }

    @Operation(
            summary = "重置用户密码",
            description = "强制重置用户密码为随机密码，并失效所有 Token",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/resetPassword")
    public R<AdminDto.AdminResetPasswordResponse> resetPassword(@Validated @RequestBody AdminVo.AdminResetPasswordVo vo) {
        return R.ok(adminService.resetPassword(vo));
    }

    @Operation(
            summary = "查看用户操作日志",
            description = "分页查询指定用户的操作日志",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("users/{userId}/logs")
    public R<Page<UserLog>> getUserLogs(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(adminService.getUserLogs(userId, pageNum, pageSize));
    }

    @Operation(
            summary = "查看用户活跃会话",
            description = "获取指定用户的所有活跃会话列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("users/{userId}/sessions")
    public R<List<UserSession>> getUserSessions(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId) {
        return R.ok(adminService.getUserSessions(userId));
    }

    @Operation(
            summary = "注销用户所有会话",
            description = "强制失效指定用户的所有会话",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("users/{userId}/revokeSessions")
    public R<Void> revokeUserSessions(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId) {
        adminService.revokeAllUserSessions(userId);
        return R.ok();
    }

    // ==================== SSO Client 管理端点 ====================

    @Operation(
            summary = "查询 SSO Client 列表",
            description = "分页查询 SSO Client 列表，支持关键词搜索和权限类型筛选",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/list")
    public R<Page<SsoClient>> listSsoClients(@Validated @RequestBody AdminVo.QuerySsoClientsVo vo) {
        return R.ok(adminService.querySsoClients(vo));
    }

    @Operation(
            summary = "查看 SSO Client 详情",
            description = "获取 SSO Client 完整信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("sso-clients/{clientId}")
    public R<AdminDto.SsoClientDetailResponse> getSsoClientDetail(
            @Parameter(description = "客户端ID", required = true)
            @PathVariable String clientId) {
        return R.ok(adminService.getSsoClientDetail(clientId));
    }

    @Operation(
            summary = "创建 SSO Client",
            description = "管理员创建新的 SSO Client，系统自动生成 clientSecret",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/create")
    public R<AdminDto.CreateSsoClientResponse> createSsoClient(@Validated @RequestBody AdminVo.CreateSsoClientVo vo) {
        return R.ok(adminService.createSsoClient(vo));
    }

    @Operation(
            summary = "修改 SSO Client 信息",
            description = "修改 SSO Client 的基本信息、权限类型等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/update")
    public R<SsoClient> updateSsoClient(@Validated @RequestBody AdminVo.UpdateSsoClientVo vo) {
        return R.ok(adminService.updateSsoClient(vo));
    }

    @Operation(
            summary = "删除 SSO Client",
            description = "逻辑删除 SSO Client（设置 is_del=1）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/delete")
    public R<Void> deleteSsoClient(@Validated @RequestBody AdminVo.DeleteSsoClientVo vo) {
        adminService.deleteSsoClient(vo);
        return R.ok();
    }

    @Operation(
            summary = "重置 SSO Client 密钥",
            description = "重置 SSO Client 的 clientSecret 为新的随机值",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/resetSecret")
    public R<AdminDto.ResetSsoClientSecretResponse> resetSsoClientSecret(@Validated @RequestBody AdminVo.ResetSsoClientSecretVo vo) {
        return R.ok(adminService.resetSsoClientSecret(vo));
    }

    @Operation(
            summary = "上传 SSO Client 图标",
            description = "上传图标文件并更新SSO客户端图标信息（需要ADMIN权限）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sso-clients/uploadIcon")
    public R<AdminDto.UploadSsoIconResponse> uploadSsoClientIcon(
            @Parameter(description = "客户端ID", required = true)
            @RequestParam("clientId") String clientId,
            @Parameter(description = "图标文件（支持jpg/png/gif/webp，建议PNG，最大2MB）", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return R.ok(adminService.uploadSsoClientIcon(clientId, file));
    }

    // ==================== LLM 管理端点 ====================

    @Operation(
            summary = "查询全部 LLM Provider",
            description = "查询系统中的全部 LLM Provider 列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/all")
    public R<List<SysLlmProvider>> listSysLlmProviders() {
        return R.ok(busiAdminService.listSysLlmProviders());
    }

    @Operation(
            summary = "分页查询 LLM Provider",
            description = "分页查询系统中的 LLM Provider 列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/list")
    public R<Page<SysLlmProvider>> pageSysLlmProviders(@Validated @RequestBody BusiAdminVo.PageSysLlmProvidersVo vo) {
        return R.ok(busiAdminService.pageSysLlmProviders(vo));
    }

    @Operation(
            summary = "分页查询 LLM Model",
            description = "分页查询系统中的 LLM Model 列表，支持 providerId 和 keyword 筛选",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/list")
    public R<Page<SysLlmModel>> pageSysLlmModels(@Validated @RequestBody BusiAdminVo.PageSysLlmModelsVo vo) {
        return R.ok(busiAdminService.pageSysLlmModels(vo));
    }

    @Operation(
            summary = "按 Provider 分页查询 LLM Model",
            description = "分页查询指定 Provider 下的 LLM Model 列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/listByProviderId")
    public R<Page<SysLlmModel>> pageSysLlmModelsByProviderId(@Validated @RequestBody BusiAdminVo.PageSysLlmModelsByProviderIdVo vo) {
        return R.ok(busiAdminService.pageSysLlmModelsByProviderId(vo));
    }

    @Operation(
            summary = "新增 LLM Provider",
            description = "新增一个 LLM Provider，apiKey 将以 AES 加密方式存储",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/add")
    public R<Void> addSysLlmProvider(@Validated @RequestBody BusiAdminVo.AddSysLlmProviderVo vo) {
        busiAdminService.addSysLlmProvider(vo);
        return R.ok();
    }

    @Operation(
            summary = "删除 LLM Provider",
            description = "逻辑删除 Provider，并级联逻辑删除其下所有 Model 与 Availability",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/delete")
    public R<Void> deleteSysLlmProvider(@Validated @RequestBody BusiAdminVo.DeleteSysLlmProviderVo vo) {
        busiAdminService.deleteSysLlmProvider(vo);
        return R.ok();
    }

    @Operation(
            summary = "修改 LLM Provider",
            description = "修改 Provider 基本信息，若传入 apiKey 则重新加密后保存",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/update")
    public R<Void> updateSysLlmProvider(@Validated @RequestBody BusiAdminVo.UpdateSysLlmProviderVo vo) {
        busiAdminService.updateSysLlmProvider(vo);
        return R.ok();
    }

    @Operation(
            summary = "新增 LLM Model",
            description = "向指定 Provider 下新增一个 Model",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/add")
    public R<Void> addModelToSysLlmProvider(@Validated @RequestBody BusiAdminVo.AddModelToSysLlmProviderVo vo) {
        busiAdminService.addModelToSysLlmProvider(vo);
        return R.ok();
    }

    @Operation(
            summary = "删除 LLM Model",
            description = "逻辑删除 Model，并自动逻辑删除其对应的 Availability",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/delete")
    public R<Void> deleteSysLlmModel(@Validated @RequestBody BusiAdminVo.DeleteSysLlmModelVo vo) {
        busiAdminService.deleteSysLlmModel(vo);
        return R.ok();
    }

    @Operation(
            summary = "修改 LLM Model",
            description = "修改 Model 的基础信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/update")
    public R<Void> updateSysLlmModel(@Validated @RequestBody BusiAdminVo.UpdateSysLlmModelVo vo) {
        busiAdminService.updateSysLlmModel(vo);
        return R.ok();
    }

    @Operation(
            summary = "查询 LLM Model 可用角色",
            description = "查询指定模型当前已授权的角色列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/available-roles")
    public R<List<RoleType>> getModelAvailableRoles(@Validated @RequestBody BusiAdminVo.GetModelAvailableRolesVo vo) {
        return R.ok(busiAdminService.getModelAvailableRoles(vo));
    }

    @Operation(
            summary = "授权 LLM Model 到角色",
            description = "将指定模型授权给一组角色；如果角色已经授权则报错",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/authorize-roles")
    public R<Void> authorizeModelToRoles(@Validated @RequestBody BusiAdminVo.AuthorizeModelToRolesVo vo) {
        busiAdminService.authorizeModelToRoles(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消 LLM Model 的角色授权",
            description = "取消指定模型对一组角色的授权；如果角色尚未授权则报错",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/revoke-roles")
    public R<Void> revokeModelOfRoles(@Validated @RequestBody BusiAdminVo.RevokeModelOfRolesVo vo) {
        busiAdminService.revokeModelOfRoles(vo);
        return R.ok();
    }

    @Operation(
            summary = "测试 LLM Model",
            description = "根据模型主键ID测试模型连通性，并返回模型对\u201c你是什么模型\u201d的回答",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/test")
    public R<String> testSysLlmModel(@Validated @RequestBody BusiAdminVo.TestModelVo vo) {
        return R.ok(busiAdminService.testSysLlmModel(vo));
    }

    @Operation(
            summary = "上传 LLM 供应商图标",
            description = "上传图标文件并更新供应商图标信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-providers/uploadIcon")
    public R<AdminDto.UploadProviderIconResponse> uploadProviderIcon(
            @Parameter(description = "供应商ID", required = true)
            @RequestParam("providerId") String providerId,
            @Parameter(description = "图标文件（支持jpg/png/gif/webp，最大2MB）", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return R.ok(busiAdminService.uploadSysProviderIcon(providerId, file));
    }

    @Operation(
            summary = "上传 LLM 模型图标",
            description = "上传图标文件并更新模型图标信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/uploadIcon")
    public R<AdminDto.UploadModelIconResponse> uploadModelIcon(
            @Parameter(description = "模型ID", required = true)
            @RequestParam("modelId") String modelId,
            @Parameter(description = "图标文件（支持jpg/png/gif/webp，最大2MB）", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return R.ok(busiAdminService.uploadSysModelIcon(modelId, file));
    }

    @Operation(
            summary = "获取 LLM 供应商图标",
            description = "降级链：provider 图标 → 系统默认图标 → 404",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("sys-llm-providers/icon/{providerId}")
    public ResponseEntity<byte[]> getProviderIcon(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable String providerId) {
        return busiAdminService.getProviderIcon(providerId);
    }

    // ==================== 系统配置管理端点 ====================

    @Operation(
            summary = "切换注册开关",
            description = "开启或关闭用户注册功能",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("config/allowRegisterSwitch")
    public R<Void> allowRegisterSwitch(
            @Parameter(description = "是否允许注册", required = true)
            @RequestParam("allowRegister") Boolean allowRegister) {
        sysConfigService.setConfigValue("allowRegister", allowRegister.toString());
        return R.ok();
    }

    @Operation(
            summary = "获取所有系统配置",
            description = "返回 sys_config 表的所有配置项",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("config/getAll")
    public R<List<SysConfig>> getAllSysConfig() {
        return R.ok(sysConfigService.list());
    }

    @Operation(
            summary = "设置标题生成模型",
            description = "设置用于自动生成会话标题的模型ID，传入空字符串表示禁用标题生成",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("config/simpleTaskModelSwitch")
    public R<Void> simpleTaskModelSwitch(
            @Parameter(description = "模型ID，空字符串表示禁用标题生成", required = true)
            @RequestParam("modelId") String modelId) {
        sysConfigService.setConfigValue("simpleTaskModelId", modelId);
        return R.ok();
    }

    @Operation(
            summary = "获取 byn 数字分身系统提示词",
            description = "获取 digitalByn (agentType=2) 的系统提示词配置",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("config/digitalBynSystemPrompt")
    public R<String> getDigitalBynSystemPrompt() {
        return R.ok(sysConfigService.getConfigValue("digitalBynSystemPrompt"));
    }

    @Operation(
            summary = "设置 byn 数字分身系统提示词",
            description = "设置 digitalByn (agentType=2) 的系统提示词，传入空字符串表示清除提示词",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("config/digitalBynSystemPrompt")
    public R<Void> setDigitalBynSystemPrompt(
            @Validated @RequestBody BusiAdminVo.SetDigitalBynPromptVo vo) {
        sysConfigService.setConfigValue("digitalBynSystemPrompt", vo.getPrompt());
        return R.ok();
    }

    // ==================== AgentType 可用性管理端点 ====================

    @Operation(
            summary = "查询 agentType 可用角色",
            description = "查询指定 agentType 当前已授权的角色列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/available-roles")
    public R<List<RoleType>> getAgentTypeAvailableRoles(@Validated @RequestBody BusiAdminVo.GetAgentTypeAvailableRolesVo vo) {
        return R.ok(busiAdminService.getAgentTypeAvailableRoles(vo));
    }

    @Operation(
            summary = "授权 agentType 到角色",
            description = "将指定 agentType 授权给一组角色；如果角色已经授权则报错",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/authorize-roles")
    public R<Void> authorizeAgentTypeToRoles(@Validated @RequestBody BusiAdminVo.AuthorizeAgentTypeToRolesVo vo) {
        busiAdminService.authorizeAgentTypeToRoles(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消 agentType 的角色授权",
            description = "取消指定 agentType 对一组角色的授权；如果角色尚未授权则报错",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/revoke-roles")
    public R<Void> revokeAgentTypeOfRoles(@Validated @RequestBody BusiAdminVo.RevokeAgentTypeOfRolesVo vo) {
        busiAdminService.revokeAgentTypeOfRoles(vo);
        return R.ok();
    }

    // ==================== 功能模块授权端点 ====================

    @Operation(
            summary = "查询功能模块可用角色",
            description = "查询指定功能模块当前已授权的角色列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/available-roles")
    public R<List<RoleType>> getModuleAvailableRoles(@Validated @RequestBody BusiAdminVo.GetModuleAvailableRolesVo vo) {
        return R.ok(busiAdminService.getModuleAvailableRoles(vo));
    }

    @Operation(
            summary = "授权功能模块到角色",
            description = "将指定功能模块授权给某些角色",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/authorize-roles")
    public R<Void> authorizeModuleToRoles(@Validated @RequestBody BusiAdminVo.AuthorizeModuleToRolesVo vo) {
        busiAdminService.authorizeModuleToRoles(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消功能模块角色授权",
            description = "取消指定功能模块对某些角色的授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/revoke-roles")
    public R<Void> revokeModuleOfRoles(@Validated @RequestBody BusiAdminVo.RevokeModuleOfRolesVo vo) {
        busiAdminService.revokeModuleOfRoles(vo);
        return R.ok();
    }

    // ==================== 用户组管理端点 ====================

    @Operation(
            summary = "分页查询用户组",
            description = "分页查询系统中所有用户组",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/list")
    public R<Page<UserGroup>> pageUserGroups(@Validated @RequestBody BusiAdminVo.PageUserGroupsVo vo) {
        return R.ok(busiAdminService.pageUserGroups(vo));
    }

    @Operation(
            summary = "查询全部用户组",
            description = "获取系统中全部用户组，供用户/用户组管理界面使用",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("user-groups/all")
    public R<List<UserGroup>> listAllUserGroups() {
        return R.ok(busiAdminService.listAllUserGroups());
    }

    @Operation(
            summary = "创建用户组",
            description = "创建一个新的用户组",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/create")
    public R<UserGroup> createUserGroup(@Validated @RequestBody BusiAdminVo.CreateUserGroupVo vo) {
        return R.ok(busiAdminService.createUserGroup(vo));
    }

    @Operation(
            summary = "修改用户组",
            description = "修改用户组名称和描述",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/update")
    public R<UserGroup> updateUserGroup(@Validated @RequestBody BusiAdminVo.UpdateUserGroupVo vo) {
        return R.ok(busiAdminService.updateUserGroup(vo));
    }

    @Operation(
            summary = "删除用户组",
            description = "删除用户组，级联删除成员和可用性记录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/delete")
    public R<Void> deleteUserGroup(@Validated @RequestBody BusiAdminVo.DeleteUserGroupVo vo) {
        busiAdminService.deleteUserGroup(vo);
        return R.ok();
    }

    @Operation(
            summary = "查询用户组成员",
            description = "获取用户组中所有成员的用户信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/members")
    public R<List<AdminDto.GroupMemberResponse>> getGroupMembers(@RequestParam String groupId) {
        return R.ok(busiAdminService.getGroupMembers(groupId));
    }

    @Operation(
            summary = "添加用户组成员",
            description = "向用户组中添加成员",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/addMembers")
    public R<Void> addMembersToGroup(@Validated @RequestBody BusiAdminVo.AddMembersToGroupVo vo) {
        busiAdminService.addMembersToGroup(vo);
        return R.ok();
    }

    @Operation(
            summary = "移除用户组成员",
            description = "从用户组中移除成员",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("user-groups/removeMembers")
    public R<Void> removeMembersFromGroup(@Validated @RequestBody BusiAdminVo.RemoveMembersFromGroupVo vo) {
        busiAdminService.removeMembersFromGroup(vo);
        return R.ok();
    }

    // ==================== 模型-用户组授权端点 ====================

    @Operation(
            summary = "查询模型可用用户组",
            description = "查询指定模型当前已授权的用户组列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/available-groups")
    public R<List<String>> getModelAvailableGroups(@Validated @RequestBody BusiAdminVo.GetModelAvailableGroupsVo vo) {
        return R.ok(busiAdminService.getModelAvailableGroups(vo));
    }

    @Operation(
            summary = "授权模型到用户组",
            description = "将指定模型授权给一组用户组",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/authorize-groups")
    public R<Void> authorizeModelToGroups(@Validated @RequestBody BusiAdminVo.AuthorizeModelToGroupsVo vo) {
        busiAdminService.authorizeModelToGroups(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消模型用户组授权",
            description = "取消指定模型对一组用户组的授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("sys-llm-models/revoke-groups")
    public R<Void> revokeModelOfGroups(@Validated @RequestBody BusiAdminVo.RevokeModelOfGroupsVo vo) {
        busiAdminService.revokeModelOfGroups(vo);
        return R.ok();
    }

    // ==================== AgentType-用户组授权端点 ====================

    @Operation(
            summary = "查询 agentType 可用用户组",
            description = "查询指定 agentType 当前已授权的用户组列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/available-groups")
    public R<List<String>> getAgentTypeAvailableGroups(@Validated @RequestBody BusiAdminVo.GetAgentTypeAvailableGroupsVo vo) {
        return R.ok(busiAdminService.getAgentTypeAvailableGroups(vo));
    }

    @Operation(
            summary = "授权 agentType 到用户组",
            description = "将指定 agentType 授权给一组用户组",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/authorize-groups")
    public R<Void> authorizeAgentTypeToGroups(@Validated @RequestBody BusiAdminVo.AuthorizeAgentTypeToGroupsVo vo) {
        busiAdminService.authorizeAgentTypeToGroups(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消 agentType 用户组授权",
            description = "取消指定 agentType 对一组用户组的授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("agent-type-availability/revoke-groups")
    public R<Void> revokeAgentTypeOfGroups(@Validated @RequestBody BusiAdminVo.RevokeAgentTypeOfGroupsVo vo) {
        busiAdminService.revokeAgentTypeOfGroups(vo);
        return R.ok();
    }

    // ==================== 功能模块-用户组授权端点 ====================

    @Operation(
            summary = "查询功能模块可用用户组",
            description = "查询指定功能模块当前已授权的用户组列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/available-groups")
    public R<List<String>> getModuleAvailableGroups(@Validated @RequestBody BusiAdminVo.GetModuleAvailableGroupsVo vo) {
        return R.ok(busiAdminService.getModuleAvailableGroups(vo));
    }

    @Operation(
            summary = "授权功能模块到用户组",
            description = "将指定功能模块授权给一组用户组",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/authorize-groups")
    public R<Void> authorizeModuleToGroups(@Validated @RequestBody BusiAdminVo.AuthorizeModuleToGroupsVo vo) {
        busiAdminService.authorizeModuleToGroups(vo);
        return R.ok();
    }

    @Operation(
            summary = "取消功能模块用户组授权",
            description = "取消指定功能模块对一组用户组的授权",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("feature-module-availability/revoke-groups")
    public R<Void> revokeModuleOfGroups(@Validated @RequestBody BusiAdminVo.RevokeModuleOfGroupsVo vo) {
        busiAdminService.revokeModuleOfGroups(vo);
        return R.ok();
    }
}
