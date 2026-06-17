package moe.byn.bynspring21.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;
import moe.byn.bynspring21.entity.dto.CaptchaDto;
import moe.byn.bynspring21.entity.dto.UserRegisterResult;
import moe.byn.bynspring21.entity.dto.UserRegisterResultEnum;
import moe.byn.bynspring21.entity.vo.UserVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.exception.EmailDomainUntrustedException;
import moe.byn.bynspring21.mapper.UserMapper;
import moe.byn.bynspring21.security.CustomUserDetailsService;
import moe.byn.bynspring21.security.JwtTokenUtil;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.security.entity.TokenResponse;
import moe.byn.bynspring21.service.*;
import moe.byn.bynspring21.utils.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${myapp.user.emailVerification}")
    private Boolean emailVerification;

    @Value("${myapp.user.allowEmailBinding}")
    private Boolean allowEmailBinding;

    @Autowired(required = false)
    private EmailUtils emailUtils;

    @Autowired
    private UserLogService userLogService;

    @Autowired
    private AntiBruteForceService antiBruteForceService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    @Lazy
    private SecurityUtil securityUtil;

    @Autowired
    private MinioService minioService;

    @Autowired
    @Lazy
    private CustomUserDetailsService customUserDetailsService;

    @Value("${jwt.access-token-expiration-sec}")
    private Long accessTokenExpirationSec;

    @Value("${myapp.name}")
    private String appName;
    @Autowired
    private UserSessionService userSessionService;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private CaptchaUtils captchaUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterResult registerUser(UserVo.RegisterUserVo vo) {

        if ("false".equals(sysConfigService.getConfigValue("allowRegister"))) {
            throw new BynBaseException("管理员已关闭注册通道");
        }

        antiBruteForceService.checkBeforeRegister();


        if (emailVerification && StrUtil.isBlank(vo.getEmail())) {
            throw new BynBaseException("EMAIL_VERIFICATION_REQUIRED");
        }

        captchaUtils.verifyCaptcha(vo.getVerifyCodeId(), "register", vo.getVerifyCode());

        vo.setUsername(vo.getUsername().trim());

        if (vo.getUsername().contains("@") || vo.getUsername().contains("+")) {
            // 避免用户名与邮箱或（未来的）手机号冲突
            throw new BynBaseException("用户名中包含了不允许的特殊字符");
        }

        if (vo.getPassword().length() < 8) {
            throw new BynBaseException("密码长度不能小于8位");
        }

        try {
            customUserDetailsService.loadUserByUsername(vo.getUsername());
            throw new BynBaseException("用户已存在");

        } catch (UsernameNotFoundException e) {
            // 用户不存在，允许继续注册
        }

        // 检查邮箱是否已被使用
        if (StrUtil.isNotBlank(vo.getEmail())) {
            try {
                customUserDetailsService.loadUserByUsername(vo.getEmail());
                throw new BynBaseException("该邮箱已被使用");
            } catch (UsernameNotFoundException e) {
                // 邮箱未被使用，允许继续注册
            }
        }

        User newUser = User.builder()
                .username(vo.getUsername())
                .displayName(vo.getUsername())
                .password(passwordEncoder.encode(vo.getPassword()))
                .passwordUpdatedAt(new Date())
                .email(vo.getEmail())
                .emailVerified(false)
                .roleType(emailVerification ? RoleType.AWAIT_EMAIL_VERIFICATION : RoleType.USER)
                .banned(false)
                .build();

        this.save(newUser);

        if (emailVerification) {

            log.info("需要邮箱验证，进入邮箱验证流程");

            this.sendEmailCodeAndNotify(newUser.getId(), newUser.getEmail());

            userLogService.addRecordWithMessage(UserLogBehaviour.SUCCESSFUL_REGISTER, "Email verification required");

            return UserRegisterResult.builder()
                    .user(newUser)
                    .registerResult(UserRegisterResultEnum.REQUIRE_EMAIL_VERIFICATION.getRegisterResult())
                    .tokenResponse(null) // 不返回 token
                    .build();
        }

        // 若不需要邮箱验证，直接生成 token 用于登录

        String accessToken = jwtTokenUtil.generateAccessToken(newUser.getId());
        String refreshToken = jwtTokenUtil.generateRefreshToken(newUser.getId());

        userLogService.addRecord(UserLogBehaviour.SUCCESSFUL_REGISTER);

        userSessionService.addSessionRecord(UserSession.builder()
                        .userId(newUser.getId())
                        .jwtAccessToken(accessToken)
                        .jwtRefreshToken(refreshToken)
                        .ip(RequestUtils.getClientIp())
                        .userAgent(RequestUtils.getUserAgent())
                .refreshTokenExpireTime(jwtTokenUtil.getExpirationFromToken(refreshToken))
                .build()
        );

        return UserRegisterResult.builder()
                .user(newUser)
                .registerResult(UserRegisterResultEnum.SUCCESS.getRegisterResult())
                .tokenResponse(
                        TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .expiresIn(accessTokenExpirationSec)
                                .build()
                )
                .build();
    }

    @Override
    public CaptchaDto getRegisterCaptcha() {
        return captchaUtils.getCaptcha("register");
    }

    @Override
    public CaptchaDto getLoginCaptcha() {
        return captchaUtils.getCaptcha("login");
    }

    @Override
    public TokenResponse verifyEmailCode(UserVo.VerifyEmailCodeVo vo) {

        antiBruteForceService.checkBeforeEmailCodeVerification(String.valueOf(vo.getUserId()));

        String correctCode = (String) redisUtils.get("emailVerification:" + vo.getUserId());

        if (!vo.getCode().equals(correctCode)) {
            userLogService.addRecordByUserId(UserLogBehaviour.EMAIL_CODE_INVALID, String.valueOf(vo.getUserId()));
            throw new BynBaseException("验证码错误");
        }

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getId, vo.getUserId()));

        // 只有角色为 AWAIT_EMAIL_VERIFICATION 才能更新为 USER
        if (user.getRoleType() != RoleType.AWAIT_EMAIL_VERIFICATION) {
            throw new BynBaseException("用户状态不正确，无法进行邮箱验证");
        }

        user.setEmailVerified(true);
        user.setRoleType(RoleType.USER);
        this.updateById(user);

        // 清除验证码
        redisUtils.tryDel("emailVerification:" + vo.getUserId());

        // 生成 Token
        String accessToken = jwtTokenUtil.generateAccessToken(user.getId());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        userSessionService.addSessionRecord(UserSession.builder()
                        .userId(user.getId())
                        .jwtAccessToken(accessToken)
                        .jwtRefreshToken(refreshToken)
                        .ip(RequestUtils.getClientIp())
                        .userAgent(RequestUtils.getUserAgent())
                .refreshTokenExpireTime(jwtTokenUtil.getExpirationFromToken(refreshToken))
                .build()
        );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationSec)
                .build();
    }


    /**
     * 通用的用于发送邮箱验证码的函数
     * @param userId
     * @param userEmail
     */
    public void sendEmailCodeAndNotify(String userId, String userEmail) {

        if (emailUtils == null) {
            throw new BynBaseException("邮件服务未配置，请设置 EMAIL_VERIFICATION=false 或配置邮件服务");
        }

        antiBruteForceService.checkBeforeSendEmailCode(userEmail);

        redisUtils.tryDel("emailVerification:" + userId);

        String verifyCode = String.valueOf(SecureRandomUtils.randomInt(100000, 999999));

        redisUtils.set("emailVerification:" + userId, verifyCode, 60 * 5);

        try {
            emailUtils.sendSimpleEmail(userEmail, "【" + appName + "】" + "用户注册验证码", "您的【" + appName + "】用户注册验证码为：" + verifyCode);
        } catch (EmailDomainUntrustedException e) {
            throw new BynBaseException(e.getMessage());
        }

        userLogService.addRecordWithMessage(UserLogBehaviour.SEND_EMAIL_CODE, userEmail);
    }

    @Override
    public void resendEmailCode() {
        User currentUser = securityUtil.getUser();
        this.sendEmailCodeAndNotify(currentUser.getId(), currentUser.getEmail());
    }

    @Override
    public void changePassword(String newPassword) {
        if (newPassword.length() < 8) {
            throw new BynBaseException("密码长度不能小于8位");
        }

        // 重要接口，不从缓存中获取用户信息
        String userId = securityUtil.getUserId();
        User user = this.getById(userId);

        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(new Date());

        this.updateById(user);

        // 清除 Redis 缓存
        redisUtils.tryDel("user:" + userId);

        // 记录日志
        userLogService.addRecordByUserId(UserLogBehaviour.PASSWORD_CHANGE, userId);
    }

    @Override
    public Page<UserLog> viewUserLog(UserVo.ViewUserLogVo vo) {
        String userId = securityUtil.getUserId();

        Page<UserLog> page = new Page<>(vo.getPageNum(), vo.getPageSize());

        LambdaQueryWrapper<UserLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLog::getUserId, userId)
               .orderByDesc(UserLog::getCreateTime);

        return userLogService.page(page, wrapper);
    }

    @Override
    public User editUserInfo(String displayName) {
        String userId = securityUtil.getUserId();

        // 重要接口，不从缓存中获取用户信息
        User user = this.getById(userId);

        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        if (StrUtil.isNotBlank(displayName)) {
            user.setDisplayName(displayName);
            this.updateById(user);
        }

        // 清除 Redis 缓存
        redisUtils.tryDel("user:" + userId);

        return user;
    }

    @Override
    public void updateAvatar(String userId, String avatarUrl) {
        User user = this.getById(userId);

        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        user.setAvatarUrl(avatarUrl);
        this.updateById(user);

        // 清除 Redis 缓存
        redisUtils.tryDel("user:" + userId);
    }

    @Override
    public UserVo.UpdateAvatarResponse uploadAvatar(MultipartFile file) {
        // 获取当前登录用户ID
        String userId = securityUtil.getUserId();
        String objectKey = minioService.uploadAvatar(file, userId);

        // 更新用户头像字段
        this.updateAvatar(userId, objectKey);

        // 返回结果
        return UserVo.UpdateAvatarResponse.builder()
                .avatarUrl(objectKey)
                .build();
    }

    @Override
    public InputStream getMyAvatarStream() {
        // 获取当前用户信息
        User user = securityUtil.getUser();
        String objectKey = user.getAvatarUrl();
        if (objectKey == null || objectKey.isEmpty()) {
            throw new BynBaseException("用户未设置头像");
        }

        // 从MinIO获取对象流
        return minioService.getObject(objectKey);
    }

    @Override
    public String getMyAvatarContentType() {
        // 获取当前用户信息
        User user = securityUtil.getUser();
        String objectKey = user.getAvatarUrl();

        if (objectKey == null || objectKey.isEmpty()) {
            throw new BynBaseException("用户未设置头像");
        }

        return minioService.getObjectContentType(objectKey);
    }

    @Override
    public String getMyAvatarBase64() {
        // 获取当前用户信息
        User user = securityUtil.getUser();
        String objectKey = user.getAvatarUrl();

        if (objectKey == null || objectKey.isEmpty()) {
            throw new BynBaseException("用户未设置头像");
        }

        try (InputStream inputStream = minioService.getObject(objectKey)) {
            // 读取所有字节
            byte[] imageBytes = inputStream.readAllBytes();

            // 转换为 Base64
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            // 获取 Content Type
            String contentType = minioService.getObjectContentType(objectKey);

            // 返回 Data URL 格式
            return "data:" + contentType + ";base64," + base64String;

        } catch (IOException e) {
            log.error("读取头像文件失败", e);
            throw new BynBaseException("读取头像失败");
        }
    }

    @Override
    public void resetPassword(UserVo.ResetPasswordVo vo) {
        // 验证验证码
        String correctCode = (String) redisUtils.get("emailVerification:" + vo.getUserId());

        if (!vo.getCode().equals(correctCode)) {
            throw new BynBaseException("验证码错误或已过期");
        }

        // 获取用户
        User user = this.getById(vo.getUserId());
        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(vo.getNewPassword()));
        user.setPasswordUpdatedAt(new Date());
        this.updateById(user);

        // 清除验证码
        redisUtils.tryDel("emailVerification:" + vo.getUserId());

        // 清除用户缓存
        redisUtils.tryDel("user:" + vo.getUserId());

        // 记录日志
        userLogService.addRecordByUserId(UserLogBehaviour.PASSWORD_CHANGE, String.valueOf(vo.getUserId()));
    }

    @Override
    public void bindEmail(UserVo.BindEmailVo vo) {
        // 检查是否允许绑定邮箱
        if (!allowEmailBinding) {
            throw new BynBaseException("当前不允许绑定邮箱");
        }

        User currentUser = securityUtil.getUser();

        // 检查用户是否已经绑定并验证了邮箱
        if (currentUser.getEmail() != null && currentUser.getEmailVerified()) {
            throw new BynBaseException("已绑定邮箱，请先解绑");
        }

        // 检查邮箱是否已被其他用户使用
        User existingUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, vo.getEmail()));
        if (existingUser != null && !existingUser.getId().equals(currentUser.getId())) {
            throw new BynBaseException("该邮箱已被使用");
        }

        // 更新邮箱
        currentUser.setEmail(vo.getEmail());
        currentUser.setEmailVerified(false);
        this.updateById(currentUser);

        // 清除用户缓存
        redisUtils.tryDel("user:" + currentUser.getId());

        // 发送验证码
        this.sendEmailCodeAndNotify(currentUser.getId(), vo.getEmail());
    }

    @Override
    public void bindEmailVerify(UserVo.BindEmailVerifyVo vo) {
        User currentUser = securityUtil.getUser();

        // 验证验证码
        String correctCode = (String) redisUtils.get("emailVerification:" + currentUser.getId());

        if (correctCode == null || !vo.getEmailCode().equals(correctCode)) {
            userLogService.addRecord(UserLogBehaviour.EMAIL_CODE_INVALID);
            throw new BynBaseException("验证码错误或已过期");
        }

        // 设置邮箱已验证
        currentUser.setEmailVerified(true);
        this.updateById(currentUser);

        // 清除验证码
        redisUtils.tryDel("emailVerification:" + currentUser.getId());

        // 清除用户缓存
        redisUtils.tryDel("user:" + currentUser.getId());
    }

    @Override
    public void unbindEmail() {
        // emailVerification 开启时不允许解绑
        if (emailVerification) {
            throw new BynBaseException("当前系统要求绑定邮箱，无法解绑");
        }

        String userId = securityUtil.getUserId();

        // 清除邮箱
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getEmail, null)
                .set(User::getEmailVerified, false);

        this.update(null, updateWrapper);

        // 清除用户缓存
        redisUtils.tryDel("user:" + userId);
    }
}
