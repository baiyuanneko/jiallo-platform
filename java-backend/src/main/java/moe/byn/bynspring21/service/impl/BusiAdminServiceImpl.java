package moe.byn.bynspring21.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.SysLlmProvider;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserGroup;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.dto.AdminDto;
import moe.byn.bynspring21.entity.vo.BusiAdminVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.service.BusiAdminService;
import moe.byn.bynspring21.service.AgentTypeAvailabilityService;
import moe.byn.bynspring21.service.AgentTypeGroupAvailabilityService;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.FeatureModuleGroupAvailabilityService;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.SysConfigService;
import moe.byn.bynspring21.service.SysLlmModelService;
import moe.byn.bynspring21.service.SysLlmProviderService;
import moe.byn.bynspring21.service.SysModelAvailabilityService;
import moe.byn.bynspring21.service.SysModelGroupAvailabilityService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.service.UserGroupService;
import moe.byn.bynspring21.utils.AesUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BusiAdminServiceImpl implements BusiAdminService {

    @Autowired
    private SysLlmModelService sysLlmModelService;

    @Autowired
    private SysLlmProviderService sysLlmProviderService;

    @Autowired
    private SysModelAvailabilityService sysModelAvailabilityService;

    @Autowired
    private AgentTypeAvailabilityService agentTypeAvailabilityService;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private MinioService minioService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private UserService userService;

    @Autowired
    private SysModelGroupAvailabilityService sysModelGroupAvailabilityService;

    @Autowired
    private AgentTypeGroupAvailabilityService agentTypeGroupAvailabilityService;

    @Autowired
    private FeatureModuleGroupAvailabilityService featureModuleGroupAvailabilityService;

    @Override
    public List<SysLlmProvider> listSysLlmProviders() {
        return sysLlmProviderService.list(new LambdaQueryWrapper<SysLlmProvider>()
                .orderByDesc(SysLlmProvider::getCreateTime));
    }

    @Override
    public Page<SysLlmProvider> pageSysLlmProviders(BusiAdminVo.PageSysLlmProvidersVo vo) {
        Page<SysLlmProvider> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        return sysLlmProviderService.page(page, new LambdaQueryWrapper<SysLlmProvider>()
                .orderByDesc(SysLlmProvider::getCreateTime));
    }

    @Override
    public Page<SysLlmModel> pageSysLlmModels(BusiAdminVo.PageSysLlmModelsVo vo) {
        Page<SysLlmModel> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        LambdaQueryWrapper<SysLlmModel> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(vo.getProviderId())) {
            wrapper.eq(SysLlmModel::getProviderId, vo.getProviderId());
        }

        if (StrUtil.isNotBlank(vo.getKeyword())) {
            wrapper.and(w -> w
                    .like(SysLlmModel::getModelName, vo.getKeyword())
                    .or()
                    .like(SysLlmModel::getRealModelName, vo.getKeyword())
                    .or()
                    .like(SysLlmModel::getModelDisplayName, vo.getKeyword())
            );
        }

        wrapper.orderByDesc(SysLlmModel::getCreateTime);
        return sysLlmModelService.page(page, wrapper);
    }

    @Override
    public Page<SysLlmModel> pageSysLlmModelsByProviderId(BusiAdminVo.PageSysLlmModelsByProviderIdVo vo) {
        Page<SysLlmModel> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        return sysLlmModelService.page(page, new LambdaQueryWrapper<SysLlmModel>()
                .eq(SysLlmModel::getProviderId, vo.getProviderId())
                .orderByDesc(SysLlmModel::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSysLlmProvider(BusiAdminVo.AddSysLlmProviderVo vo) {
        SysLlmProvider provider = SysLlmProvider.builder()
                .providerName(vo.getProviderName())
                .baseUrl(vo.getBaseUrl())
                .apiKey(vo.getApiKey())
                .build();

        if (StrUtil.isNotBlank(provider.getApiKey())) {
            provider.setApiKey(aesUtils.encrypt(provider.getApiKey()));
        }
        sysLlmProviderService.save(provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSysLlmProvider(BusiAdminVo.DeleteSysLlmProviderVo vo) {
        SysLlmProvider provider = sysLlmProviderService.getById(vo.getProviderId());
        if (provider == null) {
            throw new BynBaseException("供应商不存在");
        }

        List<SysLlmModel> models = sysLlmModelService.list(new LambdaQueryWrapper<SysLlmModel>()
                .eq(SysLlmModel::getProviderId, vo.getProviderId()));
        for (SysLlmModel model : models) {
            sysLlmModelService.removeModelWithAvailability(model.getId());
        }

        sysLlmProviderService.removeById(vo.getProviderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSysLlmProvider(BusiAdminVo.UpdateSysLlmProviderVo vo) {
        SysLlmProvider existedProvider = sysLlmProviderService.getById(vo.getId());
        if (existedProvider == null) {
            throw new BynBaseException("供应商不存在");
        }

        SysLlmProvider provider = new SysLlmProvider();
        provider.setId(existedProvider.getId());
        provider.setProviderName(vo.getProviderName());
        provider.setBaseUrl(vo.getBaseUrl());
        if (StrUtil.isNotBlank(vo.getApiKey())) {
            provider.setApiKey(aesUtils.encrypt(vo.getApiKey()));
        }

        sysLlmProviderService.updateById(provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addModelToSysLlmProvider(BusiAdminVo.AddModelToSysLlmProviderVo vo) {
        if (sysLlmProviderService.getById(vo.getProviderId()) == null) {
            throw new BynBaseException("供应商不存在");
        }

        SysLlmModel model = SysLlmModel.builder()
                .providerId(vo.getProviderId())
                .modelName(vo.getModelName())
                .realModelName(vo.getRealModelName())
                .modelDisplayName(vo.getModelDisplayName())
                .build();
        sysLlmModelService.save(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSysLlmModel(BusiAdminVo.DeleteSysLlmModelVo vo) {
        sysLlmModelService.removeModelWithAvailability(vo.getModelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSysLlmModel(BusiAdminVo.UpdateSysLlmModelVo vo) {
        SysLlmModel existedModel = sysLlmModelService.getById(vo.getId());
        if (existedModel == null) {
            throw new BynBaseException("模型不存在");
        }

        SysLlmModel model = new SysLlmModel();
        model.setId(existedModel.getId());
        model.setModelName(vo.getModelName());
        model.setRealModelName(vo.getRealModelName());
        model.setModelDisplayName(vo.getModelDisplayName());
        model.setIsVerifiedModel(vo.getIsVerifiedModel());
        sysLlmModelService.updateById(model);
    }

    @Override
    public List<RoleType> getModelAvailableRoles(BusiAdminVo.GetModelAvailableRolesVo vo) {
        return sysModelAvailabilityService.getModelAvailableRoles(vo.getModelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModelToRoles(BusiAdminVo.AuthorizeModelToRolesVo vo) {
        sysModelAvailabilityService.authorizeModelToRoles(vo.getModelId(), vo.getRoleTypes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModelOfRoles(BusiAdminVo.RevokeModelOfRolesVo vo) {
        sysModelAvailabilityService.revokeModelOfRoles(vo.getModelId(), vo.getRoleTypes());
    }

    @Override
    public String testSysLlmModel(BusiAdminVo.TestModelVo vo) {
        ChatClient chatClient = ChatClient.create(sysLlmModelService.createChatModelByModelId(vo.getId()));
        String content = chatClient.prompt("你是什么模型").call().content();
        if (StrUtil.isBlank(content)) {
            throw new BynBaseException("模型返回为空");
        }
        return content;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDto.UploadProviderIconResponse uploadSysProviderIcon(String providerId, MultipartFile file) {
        SysLlmProvider provider = sysLlmProviderService.getById(providerId);
        if (provider == null) {
            throw new BynBaseException("供应商不存在");
        }

        String iconUrl = minioService.uploadSysProviderIcon(file, providerId);
        provider.setProviderIconUrl(iconUrl);
        sysLlmProviderService.updateById(provider);

        return AdminDto.UploadProviderIconResponse.builder()
                .providerId(providerId)
                .providerIconUrl(iconUrl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDto.UploadModelIconResponse uploadSysModelIcon(String modelId, MultipartFile file) {
        SysLlmModel model = sysLlmModelService.getById(modelId);
        if (model == null) {
            throw new BynBaseException("模型不存在");
        }

        String iconUrl = minioService.uploadSysModelIcon(file, modelId);
        model.setModelIconUrl(iconUrl);
        sysLlmModelService.updateById(model);

        return AdminDto.UploadModelIconResponse.builder()
                .modelId(modelId)
                .modelIconUrl(iconUrl)
                .build();
    }

    @Override
    public ResponseEntity<byte[]> getProviderIcon(String providerId) {
        SysLlmProvider provider = sysLlmProviderService.getById(providerId);
        if (provider == null) {
            return ResponseEntity.notFound().build();
        }

        String iconUrl = null;

        // 降级链 1：provider.iconUrl
        if (StringUtils.hasText(provider.getProviderIconUrl())) {
            iconUrl = provider.getProviderIconUrl();
        }

        // 降级链 2：sysConfig defaultLlmIcon
        if (iconUrl == null) {
            String defaultIcon = sysConfigService.getConfigValue("defaultLlmIcon");
            if (StringUtils.hasText(defaultIcon)) {
                iconUrl = defaultIcon;
            }
        }

        // 降级链 3：全部为空 → 404
        if (iconUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = minioService.getObjectContentType(iconUrl);
        byte[] bytes = minioService.getMediaBytes(iconUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(bytes);
    }

    @Override
    public List<RoleType> getAgentTypeAvailableRoles(BusiAdminVo.GetAgentTypeAvailableRolesVo vo) {
        return agentTypeAvailabilityService.getAgentTypeAvailableRoles(vo.getAgentType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeAgentTypeToRoles(BusiAdminVo.AuthorizeAgentTypeToRolesVo vo) {
        agentTypeAvailabilityService.authorizeAgentTypeToRoles(vo.getAgentType(), vo.getRoleTypes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAgentTypeOfRoles(BusiAdminVo.RevokeAgentTypeOfRolesVo vo) {
        agentTypeAvailabilityService.revokeAgentTypeOfRoles(vo.getAgentType(), vo.getRoleTypes());
    }

    @Override
    public List<RoleType> getModuleAvailableRoles(BusiAdminVo.GetModuleAvailableRolesVo vo) {
        return featureModuleAvailabilityService.getModuleAvailableRoles(vo.getModuleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModuleToRoles(BusiAdminVo.AuthorizeModuleToRolesVo vo) {
        featureModuleAvailabilityService.authorizeModuleToRoles(vo.getModuleCode(), vo.getRoleTypes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModuleOfRoles(BusiAdminVo.RevokeModuleOfRolesVo vo) {
        featureModuleAvailabilityService.revokeModuleOfRoles(vo.getModuleCode(), vo.getRoleTypes());
    }

    // ==================== 用户组管理 ====================

    @Override
    public Page<UserGroup> pageUserGroups(BusiAdminVo.PageUserGroupsVo vo) {
        return userGroupService.pageUserGroups(vo.getKeyword(), vo.getPageNum(), vo.getPageSize());
    }

    @Override
    public List<UserGroup> listAllUserGroups() {
        return userGroupService.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserGroup createUserGroup(BusiAdminVo.CreateUserGroupVo vo) {
        return userGroupService.createUserGroup(vo.getGroupName(), vo.getDescription());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserGroup updateUserGroup(BusiAdminVo.UpdateUserGroupVo vo) {
        return userGroupService.updateUserGroup(vo.getGroupId(), vo.getGroupName(), vo.getDescription());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserGroup(BusiAdminVo.DeleteUserGroupVo vo) {
        userGroupService.deleteUserGroup(vo.getGroupId());
    }

    @Override
    public List<AdminDto.GroupMemberResponse> getGroupMembers(String groupId) {
        List<String> userIds = userGroupMemberService.getGroupMemberUserIds(groupId);
        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<String, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));

        return userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(user -> AdminDto.GroupMemberResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembersToGroup(BusiAdminVo.AddMembersToGroupVo vo) {
        userGroupMemberService.addMembersToGroup(vo.getGroupId(), vo.getUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMembersFromGroup(BusiAdminVo.RemoveMembersFromGroupVo vo) {
        userGroupMemberService.removeMembersFromGroup(vo.getGroupId(), vo.getUserIds());
    }

    // ==================== 模型-用户组授权 ====================

    @Override
    public List<String> getModelAvailableGroups(BusiAdminVo.GetModelAvailableGroupsVo vo) {
        return sysModelGroupAvailabilityService.getModelAvailableGroups(vo.getModelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModelToGroups(BusiAdminVo.AuthorizeModelToGroupsVo vo) {
        sysModelGroupAvailabilityService.authorizeModelToGroups(vo.getModelId(), vo.getGroupIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModelOfGroups(BusiAdminVo.RevokeModelOfGroupsVo vo) {
        sysModelGroupAvailabilityService.revokeModelOfGroups(vo.getModelId(), vo.getGroupIds());
    }

    // ==================== AgentType-用户组授权 ====================

    @Override
    public List<String> getAgentTypeAvailableGroups(BusiAdminVo.GetAgentTypeAvailableGroupsVo vo) {
        return agentTypeGroupAvailabilityService.getAgentTypeAvailableGroups(vo.getAgentType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeAgentTypeToGroups(BusiAdminVo.AuthorizeAgentTypeToGroupsVo vo) {
        agentTypeGroupAvailabilityService.authorizeAgentTypeToGroups(vo.getAgentType(), vo.getGroupIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAgentTypeOfGroups(BusiAdminVo.RevokeAgentTypeOfGroupsVo vo) {
        agentTypeGroupAvailabilityService.revokeAgentTypeOfGroups(vo.getAgentType(), vo.getGroupIds());
    }

    // ==================== 功能模块-用户组授权 ====================

    @Override
    public List<String> getModuleAvailableGroups(BusiAdminVo.GetModuleAvailableGroupsVo vo) {
        return featureModuleGroupAvailabilityService.getModuleAvailableGroups(vo.getModuleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModuleToGroups(BusiAdminVo.AuthorizeModuleToGroupsVo vo) {
        featureModuleGroupAvailabilityService.authorizeModuleToGroups(vo.getModuleCode(), vo.getGroupIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModuleOfGroups(BusiAdminVo.RevokeModuleOfGroupsVo vo) {
        featureModuleGroupAvailabilityService.revokeModuleOfGroups(vo.getModuleCode(), vo.getGroupIds());
    }
}
