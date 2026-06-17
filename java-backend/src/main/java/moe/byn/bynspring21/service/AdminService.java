package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.dto.AdminDto;
import moe.byn.bynspring21.entity.vo.AdminVo;

import java.util.List;

/**
 * 管理员服务接口
 * 提供管理员用户管理功能
 */
public interface AdminService {

    /**
     * 查询用户列表（分页 + 搜索 + 筛选）
     *
     * @param vo 查询条件
     * @return 分页用户列表
     */
    Page<User> queryUsers(AdminVo.QueryUsersVo vo);

    /**
     * 获取用户详细信息
     *
     * @param userId 用户ID
     * @return 用户详细信息（包含统计数据）
     */
    AdminDto.UserDetailResponse getUserDetail(String userId);

    /**
     * 创建用户
     *
     * @param vo 创建用户请求
     * @return 创建结果（包含初始密码）
     */
    AdminDto.CreateUserResponse createUser(AdminVo.CreateUserVo vo);

    /**
     * 修改用户信息
     *
     * @param vo 修改用户请求
     * @return 更新后的用户信息
     */
    User updateUser(AdminVo.UpdateUserVo vo);

    /**
     * 删除用户（逻辑删除）
     *
     * @param vo 删除用户请求
     */
    void deleteUser(AdminVo.DeleteUserVo vo);

    /**
     * 封禁/解封用户
     *
     * @param vo 封禁用户请求
     */
    void banUser(AdminVo.BanUserVo vo);

    /**
     * 重置用户密码
     *
     * @param vo 重置密码请求
     * @return 重置结果（包含新密码）
     */
    AdminDto.AdminResetPasswordResponse resetPassword(AdminVo.AdminResetPasswordVo vo);

    /**
     * 获取用户操作日志
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页日志列表
     */
    Page<UserLog> getUserLogs(String userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户活跃会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<UserSession> getUserSessions(String userId);

    /**
     * 注销用户所有会话
     *
     * @param userId 用户ID
     */
    void revokeAllUserSessions(String userId);

    // ==================== SSO Client 管理 ====================

    /**
     * 查询 SSO Client 列表（分页 + 搜索 + 筛选）
     *
     * @param vo 查询条件
     * @return 分页 SSO Client 列表
     */
    Page<SsoClient> querySsoClients(AdminVo.QuerySsoClientsVo vo);

    /**
     * 获取 SSO Client 详细信息
     *
     * @param clientId 客户端ID
     * @return SSO Client 信息
     */
    AdminDto.SsoClientDetailResponse getSsoClientDetail(String clientId);

    /**
     * 创建 SSO Client
     *
     * @param vo 创建请求
     * @return 创建结果（包含 clientSecret）
     */
    AdminDto.CreateSsoClientResponse createSsoClient(AdminVo.CreateSsoClientVo vo);

    /**
     * 修改 SSO Client 信息
     *
     * @param vo 修改请求
     * @return 更新后的 SSO Client 信息
     */
    SsoClient updateSsoClient(AdminVo.UpdateSsoClientVo vo);

    /**
     * 删除 SSO Client
     *
     * @param vo 删除请求
     */
    void deleteSsoClient(AdminVo.DeleteSsoClientVo vo);

    /**
     * 重置 SSO Client 密钥
     *
     * @param vo 重置请求
     * @return 重置结果（包含新 clientSecret）
     */
    AdminDto.ResetSsoClientSecretResponse resetSsoClientSecret(AdminVo.ResetSsoClientSecretVo vo);

    /**
     * 上传 SSO 客户端图标
     *
     * @param clientId 客户端ID
     * @param file     图标文件
     * @return 上传结果（包含图标路径）
     */
    AdminDto.UploadSsoIconResponse uploadSsoClientIcon(String clientId, org.springframework.web.multipart.MultipartFile file);
}
