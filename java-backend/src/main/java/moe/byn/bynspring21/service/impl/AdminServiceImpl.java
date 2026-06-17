package moe.byn.bynspring21.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.dto.AdminDto;
import moe.byn.bynspring21.entity.vo.AdminVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.JwtTokenUtil;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.security.TokenBlacklistService;
import moe.byn.bynspring21.service.AdminService;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.SsoClientService;
import moe.byn.bynspring21.service.UserLogService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.service.UserSessionService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.service.UserGroupService;
import moe.byn.bynspring21.utils.EmailUtils;
import moe.byn.bynspring21.utils.RedisUtils;
import moe.byn.bynspring21.utils.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserLogService userLogService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private SsoClientService ssoClientService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired(required = false)
    private EmailUtils emailUtils;

    @Autowired
    private SecurityUtil securityUtil;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${myapp.user.emailVerification}")
    private Boolean emailVerification;

    @Override
    public Page<User> queryUsers(AdminVo.QueryUsersVo vo) {
        Page<User> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（用户名、邮箱、显示名称）
        if (StrUtil.isNotBlank(vo.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, vo.getKeyword())
                    .or()
                    .like(User::getEmail, vo.getKeyword())
                    .or()
                    .like(User::getDisplayName, vo.getKeyword())
            );
        }

        // 角色筛选
        if (vo.getRoleType() != null) {
            wrapper.eq(User::getRoleType, vo.getRoleType());
        }

        // 封禁状态筛选
        if (vo.getBanned() != null) {
            wrapper.eq(User::getBanned, vo.getBanned());
        }

        // 邮箱验证状态筛选
        if (vo.getEmailVerified() != null) {
            wrapper.eq(User::getEmailVerified, vo.getEmailVerified());
        }

        // 按创建时间降序排列
        wrapper.orderByDesc(User::getCreateTime);

        return userService.page(page, wrapper);
    }

    @Override
    public AdminDto.UserDetailResponse getUserDetail(String userId) {
        // 获取用户基本信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 获取活跃会话数量
        List<UserSession> sessions = userSessionService.queryUserActiveSessions(userId);
        int activeSessionCount = sessions.size();

        // 获取最近登录信息（从最新的会话记录）
        Date lastLoginTime = null;
        String lastLoginIp = null;
        if (!sessions.isEmpty()) {
            UserSession latestSession = sessions.stream()
                    .max(Comparator.comparing(UserSession::getCreateTime))
                    .orElse(null);
            if (latestSession != null) {
                lastLoginTime = latestSession.getCreateTime();
                lastLoginIp = latestSession.getIp();
            }
        }

        // 获取总日志数量
        long totalLogCount = userLogService.lambdaQuery()
                .eq(UserLog::getUserId, userId)
                .count();

        List<String> userGroupIds = userGroupMemberService.getUserGroupIds(userId);
        List<moe.byn.bynspring21.entity.UserGroup> userGroups = userGroupIds.isEmpty()
                ? List.of()
                : userGroupService.listByIds(userGroupIds);
        List<AdminDto.UserGroupSummary> groups = userGroups.stream()
                .map(group -> AdminDto.UserGroupSummary.builder()
                        .groupId(group.getId())
                        .groupName(group.getGroupName())
                        .description(group.getDescription())
                        .build())
                .toList();

        return AdminDto.UserDetailResponse.builder()
                .user(user)
                .groups(groups)
                .activeSessionCount(activeSessionCount)
                .lastLoginTime(lastLoginTime)
                .lastLoginIp(lastLoginIp)
                .totalLogCount(totalLogCount)
                .build();
    }

    @Override
    public AdminDto.CreateUserResponse createUser(AdminVo.CreateUserVo vo) {
        // 检查用户名是否已存在
        User existingUser = userService.lambdaQuery()
                .eq(User::getUsername, vo.getUsername())
                .one();
        if (existingUser != null) {
            throw new BynBaseException("用户名已存在");
        }

        // 检查邮箱是否已被使用
        if (StrUtil.isNotBlank(vo.getEmail())) {
            User emailUser = userService.lambdaQuery()
                    .eq(User::getEmail, vo.getEmail())
                    .one();
            if (emailUser != null) {
                throw new BynBaseException("该邮箱已被使用");
            }
        }

        // 生成随机密码（12位）
        String randomPassword = SecureRandomUtils.randomString(12);

        // 创建用户
        User newUser = User.builder()
                .username(vo.getUsername())
                .displayName(StrUtil.isNotBlank(vo.getDisplayName()) ? vo.getDisplayName() : vo.getUsername())
                .password(passwordEncoder.encode(randomPassword))
                .passwordUpdatedAt(new Date())
                .email(vo.getEmail())
                .emailVerified(StrUtil.isNotBlank(vo.getEmail())) // 管理员创建默认验证
                .roleType(vo.getRoleType())
                .banned(false)
                .build();

        userService.save(newUser);

        // 发送邮件（如果需要）
        boolean emailSent = false;
        if (Boolean.TRUE.equals(vo.getSendEmail()) && emailVerification && StrUtil.isNotBlank(vo.getEmail())) {
            try {
                emailUtils.sendSimpleEmail(
                        vo.getEmail(),
                        "【" + appName + "】账号创建通知",
                        "您的账号已创建成功！\n用户名：" + vo.getUsername() + "\n初始密码：" + randomPassword + "\n请及时登录并修改密码。"
                );
                emailSent = true;
            } catch (Exception e) {
                log.error("发送邮件失败", e);
            }
        }

        return AdminDto.CreateUserResponse.builder()
                .userId(String.valueOf(newUser.getId()))
                .username(newUser.getUsername())
                .initialPassword(randomPassword)
                .emailSent(emailSent)
                .build();
    }

    @Override
    public User updateUser(AdminVo.UpdateUserVo vo) {
        // 获取用户
        User user = userService.getById(vo.getUserId());
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        boolean needClearCache = false;

        // 更新显示名称
        if (StrUtil.isNotBlank(vo.getDisplayName())) {
            user.setDisplayName(vo.getDisplayName());
            needClearCache = true;
        }

        // 更新邮箱
        if (StrUtil.isNotBlank(vo.getEmail())) {
            // 检查邮箱是否被其他用户使用
            User emailUser = userService.lambdaQuery()
                    .eq(User::getEmail, vo.getEmail())
                    .ne(User::getId, vo.getUserId())
                    .one();
            if (emailUser != null) {
                throw new BynBaseException("该邮箱已被其他用户使用");
            }
            user.setEmail(vo.getEmail());
            needClearCache = true;
        }

        // 更新邮箱验证状态
        if (vo.getEmailVerified() != null) {
            user.setEmailVerified(vo.getEmailVerified());
            needClearCache = true;
        }

        // 更新角色
        if (vo.getRoleType() != null) {
            // 防止管理员修改自己的角色为非 ADMIN
            String currentAdminId = securityUtil.getUserId();
            if (vo.getUserId().equals(currentAdminId) && vo.getRoleType() != RoleType.ADMIN) {
                throw new BynBaseException("不能修改自己的管理员角色");
            }
            user.setRoleType(vo.getRoleType());
            needClearCache = true;
        }

        // 更新数据库
        userService.updateById(user);

        // 清除缓存
        if (needClearCache) {
            redisUtils.tryDel("user:" + vo.getUserId());
        }

        return user;
    }

    @Override
    public void deleteUser(AdminVo.DeleteUserVo vo) {
        // 检查用户是否存在
        User user = userService.getById(vo.getUserId());
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 防止删除自己
        String currentAdminId = securityUtil.getUserId();
        if (vo.getUserId().equals(currentAdminId)) {
            throw new BynBaseException("不能删除自己的账号");
        }

        // 防止删除其他管理员
        if (user.getRoleType() == RoleType.ADMIN) {
            throw new BynBaseException("不能删除管理员账号");
        }

        // 逻辑删除用户（MyBatis Plus 自动处理 is_del=1）
        userService.removeById(vo.getUserId());

        // 失效所有 Token
        revokeAllUserTokens(vo.getUserId());

        // 清除缓存
        redisUtils.tryDel("user:" + vo.getUserId());
    }

    @Override
    public void banUser(AdminVo.BanUserVo vo) {
        // 检查用户是否存在
        User user = userService.getById(vo.getUserId());
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 防止封禁自己
        String currentAdminId = securityUtil.getUserId();
        if (vo.getUserId().equals(currentAdminId)) {
            throw new BynBaseException("不能封禁自己的账号");
        }

        // 防止封禁其他管理员
        if (user.getRoleType() == RoleType.ADMIN) {
            throw new BynBaseException("不能封禁管理员账号");
        }

        // 更新封禁状态
        user.setBanned(vo.getBanned());
        userService.updateById(user);

        // 如果是封禁操作，立即失效所有 Token
        if (Boolean.TRUE.equals(vo.getBanned())) {
            revokeAllUserTokens(vo.getUserId());
        }

        // 清除缓存
        redisUtils.tryDel("user:" + vo.getUserId());
    }

    @Override
    public AdminDto.AdminResetPasswordResponse resetPassword(AdminVo.AdminResetPasswordVo vo) {
        // 检查用户是否存在
        User user = userService.getById(vo.getUserId());
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 生成随机密码（12位）
        String newPassword = SecureRandomUtils.randomString(12);

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(new Date());
        userService.updateById(user);

        // 失效所有 Token（强制重新登录）
        revokeAllUserTokens(vo.getUserId());

        // 清除缓存
        redisUtils.tryDel("user:" + vo.getUserId());

        // 发送邮件（如果需要）
        boolean emailSent = false;
        if (Boolean.TRUE.equals(vo.getSendEmail()) && emailVerification && StrUtil.isNotBlank(user.getEmail())) {
            try {
                emailUtils.sendSimpleEmail(
                        user.getEmail(),
                        "【" + appName + "】密码重置通知",
                        "管理员已重置您的密码。\n新密码：" + newPassword + "\n请及时登录并修改密码。"
                );
                emailSent = true;
            } catch (Exception e) {
                log.error("发送邮件失败", e);
            }
        }

        return AdminDto.AdminResetPasswordResponse.builder()
                .newPassword(newPassword)
                .emailSent(emailSent)
                .build();
    }

    @Override
    public Page<UserLog> getUserLogs(String userId, Integer pageNum, Integer pageSize) {
        Page<UserLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLog::getUserId, userId);
        wrapper.orderByDesc(UserLog::getCreateTime);
        return userLogService.page(page, wrapper);
    }

    @Override
    public List<UserSession> getUserSessions(String userId) {
        return userSessionService.queryUserActiveSessions(userId);
    }

    @Override
    public void revokeAllUserSessions(String userId) {
        revokeAllUserTokens(userId);
    }

    /**
     * 辅助方法：批量失效用户的所有 Token
     *
     * @param userId 用户ID
     */
    private void revokeAllUserTokens(String userId) {
        // 获取用户所有活跃会话
        List<UserSession> sessions = userSessionService.queryUserActiveSessions(userId);

        // 将所有 Token 加入黑名单
        for (UserSession session : sessions) {
            try {
                // Access Token
                if (StrUtil.isNotBlank(session.getJwtAccessToken())) {
                    Date expiration = jwtTokenUtil.getExpirationFromToken(session.getJwtAccessToken());
                    tokenBlacklistService.addToBlacklist(session.getJwtAccessToken(), expiration);
                }
                // Refresh Token
                if (StrUtil.isNotBlank(session.getJwtRefreshToken())) {
                    Date expiration = jwtTokenUtil.getExpirationFromToken(session.getJwtRefreshToken());
                    tokenBlacklistService.addToBlacklist(session.getJwtRefreshToken(), expiration);
                }
            } catch (Exception e) {
                log.error("Failed to blacklist token for session: " + session.getId(), e);
            }
        }

        // 逻辑删除所有会话记录（标记为失效）
        userSessionService.revokeAllSessions(userId);
    }

    // ==================== SSO Client 管理实现 ====================

    @Override
    public Page<SsoClient> querySsoClients(AdminVo.QuerySsoClientsVo vo) {
        Page<SsoClient> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        LambdaQueryWrapper<SsoClient> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（客户端唯一名称、名称、描述）
        if (StrUtil.isNotBlank(vo.getKeyword())) {
            wrapper.and(w -> w
                    .like(SsoClient::getClientUniqueName, vo.getKeyword())
                    .or()
                    .like(SsoClient::getClientName, vo.getKeyword())
                    .or()
                    .like(SsoClient::getClientDesc, vo.getKeyword())
            );
        }

        // 权限类型筛选
        if (vo.getPermissionType() != null) {
            wrapper.eq(SsoClient::getClientPermissionType, vo.getPermissionType());
        }

        // 按创建时间降序排列
        wrapper.orderByDesc(SsoClient::getCreateTime);

        return ssoClientService.page(page, wrapper);
    }

    @Override
    public AdminDto.SsoClientDetailResponse getSsoClientDetail(String clientId) {
        SsoClient client = ssoClientService.getById(clientId);
        if (client == null) {
            throw new BynBaseException("SSO Client 不存在");
        }
        return AdminDto.SsoClientDetailResponse.builder()
                .ssoClient(client)
                .build();
    }

    @Override
    public AdminDto.CreateSsoClientResponse createSsoClient(AdminVo.CreateSsoClientVo vo) {
        // 检查 clientUniqueName 是否已存在
        SsoClient existingClient = ssoClientService.lambdaQuery()
                .eq(SsoClient::getClientUniqueName, vo.getClientUniqueName())
                .one();
        if (existingClient != null) {
            throw new BynBaseException("客户端唯一名称已存在");
        }

        // 生成 clientSecret（32位随机字符串）
        String clientSecret = SecureRandomUtils.randomString(32);

        // 创建 SSO Client
        SsoClient newClient = SsoClient.builder()
                .clientUniqueName(vo.getClientUniqueName())
                .clientName(vo.getClientName())
                .clientDesc(vo.getClientDesc())
                .clientWebsite(vo.getClientWebsite())
                .clientAuthorName(vo.getClientAuthorName())
                .clientSecret(clientSecret)
                .clientRedirectUri(vo.getClientRedirectUri())
                .clientPermissionType(vo.getClientPermissionType())
                .silentRedirect(vo.getSilentRedirect())
                .build();

        ssoClientService.save(newClient);

        return AdminDto.CreateSsoClientResponse.builder()
                .clientId(String.valueOf(newClient.getId()))
                .clientUniqueName(newClient.getClientUniqueName())
                .clientSecret(clientSecret)
                .build();
    }

    @Override
    public SsoClient updateSsoClient(AdminVo.UpdateSsoClientVo vo) {
        // 获取 SSO Client
        SsoClient client = ssoClientService.getById(vo.getClientId());
        if (client == null) {
            throw new BynBaseException("SSO Client 不存在");
        }

        // 更新字段
        if (StrUtil.isNotBlank(vo.getClientName())) {
            client.setClientName(vo.getClientName());
        }

        if (vo.getClientDesc() != null) {
            client.setClientDesc(vo.getClientDesc());
        }

        if (vo.getClientWebsite() != null) {
            client.setClientWebsite(vo.getClientWebsite());
        }

        if (vo.getClientAuthorName() != null) {
            client.setClientAuthorName(vo.getClientAuthorName());
        }

        if (StrUtil.isNotBlank(vo.getClientRedirectUri())) {
            client.setClientRedirectUri(vo.getClientRedirectUri());
        }

        if (vo.getClientPermissionType() != null) {
            client.setClientPermissionType(vo.getClientPermissionType());
        }

        if (vo.getSilentRedirect() != null) {
            client.setSilentRedirect(vo.getSilentRedirect());
        }

        // 更新数据库
        ssoClientService.updateById(client);

        return client;
    }

    @Override
    public void deleteSsoClient(AdminVo.DeleteSsoClientVo vo) {
        // 检查 SSO Client 是否存在
        SsoClient client = ssoClientService.getById(vo.getClientId());
        if (client == null) {
            throw new BynBaseException("SSO Client 不存在");
        }

        // 逻辑删除（MyBatis Plus 自动处理 is_del=1）
        ssoClientService.removeById(vo.getClientId());
    }

    @Override
    public AdminDto.ResetSsoClientSecretResponse resetSsoClientSecret(AdminVo.ResetSsoClientSecretVo vo) {
        // 检查 SSO Client 是否存在
        SsoClient client = ssoClientService.getById(vo.getClientId());
        if (client == null) {
            throw new BynBaseException("SSO Client 不存在");
        }

        // 生成新的 clientSecret（32位随机字符串）
        String newClientSecret = SecureRandomUtils.randomString(32);

        // 更新 clientSecret
        client.setClientSecret(newClientSecret);
        ssoClientService.updateById(client);

        return AdminDto.ResetSsoClientSecretResponse.builder()
                .clientSecret(newClientSecret)
                .build();
    }

    @Override
    public AdminDto.UploadSsoIconResponse uploadSsoClientIcon(String clientId, MultipartFile file) {
        // 验证客户端存在
        SsoClient client = ssoClientService.getById(clientId);
        if (client == null) {
            throw new BynBaseException("SSO客户端不存在");
        }

        // 上传图标到 MinIO
        String objectKey = minioService.uploadSsoIcon(file, clientId);

        // 更新数据库
        ssoClientService.updateClientIcon(clientId, objectKey);

        return AdminDto.UploadSsoIconResponse.builder()
                .clientId(clientId)
                .iconUrl(objectKey)
                .build();
    }
}
