package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.SysLlmProvider;
import moe.byn.bynspring21.entity.UserGroup;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.dto.AdminDto;
import moe.byn.bynspring21.entity.vo.BusiAdminVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BusiAdminService {

    List<SysLlmProvider> listSysLlmProviders();

    Page<SysLlmProvider> pageSysLlmProviders(BusiAdminVo.PageSysLlmProvidersVo vo);

    Page<SysLlmModel> pageSysLlmModels(BusiAdminVo.PageSysLlmModelsVo vo);

    Page<SysLlmModel> pageSysLlmModelsByProviderId(BusiAdminVo.PageSysLlmModelsByProviderIdVo vo);

    void addSysLlmProvider(BusiAdminVo.AddSysLlmProviderVo vo);

    void deleteSysLlmProvider(BusiAdminVo.DeleteSysLlmProviderVo vo);

    void updateSysLlmProvider(BusiAdminVo.UpdateSysLlmProviderVo vo);

    void addModelToSysLlmProvider(BusiAdminVo.AddModelToSysLlmProviderVo vo);

    void deleteSysLlmModel(BusiAdminVo.DeleteSysLlmModelVo vo);

    void updateSysLlmModel(BusiAdminVo.UpdateSysLlmModelVo vo);

    List<RoleType> getModelAvailableRoles(BusiAdminVo.GetModelAvailableRolesVo vo);

    void authorizeModelToRoles(BusiAdminVo.AuthorizeModelToRolesVo vo);

    void revokeModelOfRoles(BusiAdminVo.RevokeModelOfRolesVo vo);

    String testSysLlmModel(BusiAdminVo.TestModelVo vo);

    AdminDto.UploadProviderIconResponse uploadSysProviderIcon(String providerId, MultipartFile file);

    AdminDto.UploadModelIconResponse uploadSysModelIcon(String modelId, MultipartFile file);

    ResponseEntity<byte[]> getProviderIcon(String providerId);

    List<RoleType> getAgentTypeAvailableRoles(BusiAdminVo.GetAgentTypeAvailableRolesVo vo);

    void authorizeAgentTypeToRoles(BusiAdminVo.AuthorizeAgentTypeToRolesVo vo);

    void revokeAgentTypeOfRoles(BusiAdminVo.RevokeAgentTypeOfRolesVo vo);

    List<RoleType> getModuleAvailableRoles(BusiAdminVo.GetModuleAvailableRolesVo vo);

    void authorizeModuleToRoles(BusiAdminVo.AuthorizeModuleToRolesVo vo);

    void revokeModuleOfRoles(BusiAdminVo.RevokeModuleOfRolesVo vo);

    // ==================== 用户组管理 ====================

    Page<UserGroup> pageUserGroups(BusiAdminVo.PageUserGroupsVo vo);

    List<UserGroup> listAllUserGroups();

    UserGroup createUserGroup(BusiAdminVo.CreateUserGroupVo vo);

    UserGroup updateUserGroup(BusiAdminVo.UpdateUserGroupVo vo);

    void deleteUserGroup(BusiAdminVo.DeleteUserGroupVo vo);

    List<AdminDto.GroupMemberResponse> getGroupMembers(String groupId);

    void addMembersToGroup(BusiAdminVo.AddMembersToGroupVo vo);

    void removeMembersFromGroup(BusiAdminVo.RemoveMembersFromGroupVo vo);

    // ==================== 模型-用户组授权 ====================

    List<String> getModelAvailableGroups(BusiAdminVo.GetModelAvailableGroupsVo vo);

    void authorizeModelToGroups(BusiAdminVo.AuthorizeModelToGroupsVo vo);

    void revokeModelOfGroups(BusiAdminVo.RevokeModelOfGroupsVo vo);

    // ==================== AgentType-用户组授权 ====================

    List<String> getAgentTypeAvailableGroups(BusiAdminVo.GetAgentTypeAvailableGroupsVo vo);

    void authorizeAgentTypeToGroups(BusiAdminVo.AuthorizeAgentTypeToGroupsVo vo);

    void revokeAgentTypeOfGroups(BusiAdminVo.RevokeAgentTypeOfGroupsVo vo);

    // ==================== 功能模块-用户组授权 ====================

    List<String> getModuleAvailableGroups(BusiAdminVo.GetModuleAvailableGroupsVo vo);

    void authorizeModuleToGroups(BusiAdminVo.AuthorizeModuleToGroupsVo vo);

    void revokeModuleOfGroups(BusiAdminVo.RevokeModuleOfGroupsVo vo);
}
