package moe.byn.bynspring21.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.dto.CaptchaDto;
import moe.byn.bynspring21.entity.dto.ForgetPasswordResult;
import moe.byn.bynspring21.entity.dto.UserRegisterResult;
import moe.byn.bynspring21.entity.vo.UserVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.exception.EmailVerificationRequiredException;
import moe.byn.bynspring21.security.AuthService;
import moe.byn.bynspring21.security.CustomUserDetailsService;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.security.entity.TokenResponse;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.service.UserGroupService;
import moe.byn.bynspring21.service.UserSessionService;

import moe.byn.bynspring21.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等接口")
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private UserGroupService userGroupService;

    @Value("${myapp.clientMode.isSsoClient:false}")
    private Boolean isSsoClient;

    @Operation(summary = "用户注册", description = "注册新用户账号，返回 JWT token")
    @PostMapping("register")
    public R<UserRegisterResult> register(@Validated @RequestBody UserVo.RegisterUserVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许单独注册用户");
        }

        return R.ok(userService.registerUser(vo));
    }
    
    @GetMapping("getRegisterCaptcha")
    private R<CaptchaDto> getRegisterCaptcha() {
        return R.ok(userService.getRegisterCaptcha());
    }

    @Operation(
            summary = "用户登录",
            description = "使用用户名和密码登录，返回 JWT token。如果用户邮箱未验证，则返回邮箱验证提示信息。",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "登录成功，返回 JWT tokens",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TokenResponse.class),
                                    examples = @ExampleObject(
                                            name = "成功登录",
                                            value = "{\"code\":200,\"msg\":\"success\",\"data\":{\"accessToken\":\"eyJhbGciOiJIUzM4NCJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzM4NCJ9...\",\"tokenType\":\"Bearer\",\"expiresIn\":3600}}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "200",
                            description = "需要邮箱验证",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "邮箱未验证",
                                            value = "{\"code\":200,\"msg\":\"success\",\"data\":{\"userId\":123,\"email\":\"user@example.com\",\"message\":\"EMAIL_VERIFICATION_REQUIRED\"}}"
                                    )
                            )
                    )
            }
    )
    @PostMapping("login")
    public R<?> login(@Validated @RequestBody UserVo.LoginUserVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许单独登录用户");
        }

        try {
            TokenResponse tokenResponse = authService.login(vo, false);
            return R.ok(tokenResponse);
        } catch (EmailVerificationRequiredException e) {
            // 需要邮箱验证 - 自动发送验证码
            userService.sendEmailCodeAndNotify(e.getUserId(), e.getEmail());

            // 返回特殊响应
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("userId", e.getUserId());
            response.put("email", e.getEmail());
            response.put("message", "EMAIL_VERIFICATION_REQUIRED");
            return R.ok(response);
        }
    }

    @GetMapping("getLoginCaptcha")
    private R<CaptchaDto> getLoginCaptcha() {
        return R.ok(userService.getLoginCaptcha());
    }

    @PostMapping("resendEmailCodeWithoutLogin")
    public R<Void> resendEmailCodeWithoutLogin(@Validated @RequestBody UserVo.LoginUserVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        try {
            authService.login(vo, true);
            return R.error();
        } catch (EmailVerificationRequiredException e) {
            // 需要邮箱验证 - 自动发送验证码
            userService.sendEmailCodeAndNotify(e.getUserId(), e.getEmail());
            return R.ok();
        }
    }

    @Operation(summary = "刷新Token", description = "使用 Refresh Token 获取新的 Access Token 和 Refresh Token")
    @PostMapping("refresh-token")
    public R<TokenResponse> refreshToken(@Validated @RequestBody UserVo.RefreshTokenVo vo) {
        return R.ok(authService.refreshToken(vo.getRefreshToken()));
    }

    @Operation(summary = "用户登出", description = "登出当前用户，将 Access Token 和 Refresh Token 加入黑名单", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("logout")
    public R<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Validated @RequestBody UserVo.LogoutVo vo) {
        // 从请求头提取 Access Token
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        authService.logout(accessToken, vo.getRefreshToken());
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("info")
    public R<User> info() {
        return R.ok(securityUtil.getUser());
    }

    @Operation(summary = "获取当前用户所属用户组", description = "获取当前登录用户所属的用户组列表", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("myGroups")
    public R<List<moe.byn.bynspring21.entity.dto.AdminDto.UserGroupSummary>> myGroups() {
        User user = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(user.getId());
        if (groupIds.isEmpty()) {
            return R.ok(List.of());
        }

        List<moe.byn.bynspring21.entity.UserGroup> userGroups = userGroupService.listByIds(groupIds);
        List<moe.byn.bynspring21.entity.dto.AdminDto.UserGroupSummary> groups = userGroups.stream()
                .map(group -> moe.byn.bynspring21.entity.dto.AdminDto.UserGroupSummary.builder()
                        .groupId(group.getId())
                        .groupName(group.getGroupName())
                        .description(group.getDescription())
                        .build())
                .toList();
        return R.ok(groups);
    }

    @Operation(summary = "验证邮箱验证码", description = "验证用户邮箱的验证码，验证成功后返回 JWT tokens")
    @PostMapping("verifyEmailCode")
    public R<TokenResponse> verifyEmailCode(@Validated @RequestBody UserVo.VerifyEmailCodeVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        return R.ok(userService.verifyEmailCode(vo));
    }

    @Operation(summary = "重新发送邮箱验证码", description = "重新发送邮箱验证码到用户邮箱", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("resendEmailCode")
    public R<Void> resendEmailCode() {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        userService.resendEmailCode();
        return R.ok();
    }

    @Operation(summary = "修改密码", description = "修改当前用户的登录密码", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("changePassword")
    public R<Void> changePassword(@Validated @RequestBody UserVo.ChangePasswordVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        userService.changePassword(vo.getNewPassword());
        return R.ok();
    }

    @Operation(summary = "查看用户日志", description = "分页查询当前用户的操作日志", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("viewUserLog")
    public R<Page<UserLog>> viewUserLog(@Validated @RequestBody UserVo.ViewUserLogVo vo) {
        return R.ok(userService.viewUserLog(vo));
    }

    @Operation(summary = "修改用户信息", description = "修改当前用户的基本信息（如显示名称）", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("editInfo")
    public R<User> editInfo(@Validated @RequestBody UserVo.EditUserInfoVo vo) {
        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        return R.ok(userService.editUserInfo(vo.getDisplayName()));
    }

    @Operation(summary = "上传用户头像", description = "上传头像文件并更新用户头像信息（需要登录）", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("avatar")
    public R<UserVo.UpdateAvatarResponse> uploadAvatar(
            @Parameter(description = "头像文件（支持jpg/png/gif/webp，最大2MB）", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }
        return R.ok(userService.uploadAvatar(file));
    }

    @Operation(summary = "获取当前用户头像", description = "获取当前登录用户的头像图片", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("avatar/me")
    public ResponseEntity<byte[]> getMyAvatar() {
        try {
            // 从Service获取头像流
            InputStream inputStream = userService.getMyAvatarStream();
            byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
            inputStream.close();

            // 获取Content-Type
            String contentType = userService.getMyAvatarContentType();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("max-age=86400"); // 缓存1天

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Failed to read avatar image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (BynBaseException e) {
            log.warn("User avatar not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "修改登录邮箱", description = "修改等待邮箱验证用户的登录邮箱地址，验证通过后会向新邮箱发送验证码")
    @PostMapping("changeLoginEmail")
    public R<Void> changeLoginEmail(@Validated @RequestBody UserVo.ChangeLoginEmailVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }


        String userId;

        // 使用 authService.login 验证用户名和密码
        try {
            authService.login(vo.getLoginInfo(), true);
            throw new BynBaseException("用户状态不正确，只有等待邮箱验证的用户才能修改登录邮箱");
        } catch (EmailVerificationRequiredException e) {
            // 这是预期的异常，获取 userId
            userId = e.getUserId();
        }

        // 获取用户信息
        User user = userService.getById(userId);

        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 检查新邮箱是否已被使用
        User existingUser = userService.lambdaQuery()
                .eq(User::getEmail, vo.getNewEmail())
                .one();
        if (existingUser != null) {
            throw new BynBaseException("该邮箱已被使用");
        }

        // 更新邮箱
        user.setEmail(vo.getNewEmail());
        user.setEmailVerified(false);
        userService.updateById(user);

        // 发送新的验证码到新邮箱
        userService.sendEmailCodeAndNotify(user.getId(), vo.getNewEmail());

        return R.ok();
    }

    @Operation(summary = "忘记密码", description = "发送密码重置验证码到用户邮箱")
    @PostMapping("forgetPassword")
    public R<ForgetPasswordResult> forgetPassword(@Validated @RequestBody UserVo.ForgetPasswordVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        // 通过 UserDetailsService 获取用户信息
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(vo.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new BynBaseException("用户不存在");
        }

        // userDetails.getUsername() 实际上是 userId
        String userId = userDetails.getUsername();
        User user = userService.getById(userId);

        if (user == null) {
            throw new BynBaseException("用户不存在");
        }

        // 检查邮箱是否已验证
        if (!user.getEmailVerified()) {
            throw new BynBaseException("邮箱未验证，无法重置密码");
        }

        // 发送验证码到邮箱
        userService.sendEmailCodeAndNotify(user.getId(), user.getEmail());

        return R.ok(ForgetPasswordResult.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .build());
    }

    @Operation(summary = "重置密码", description = "使用验证码重置密码")
    @PostMapping("resetPassword")
    public R<Void> resetPassword(@Validated @RequestBody UserVo.ResetPasswordVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        // 验证密码长度
        if (vo.getNewPassword().length() < 8) {
            throw new BynBaseException("密码长度不能小于8位");
        }

        // 验证验证码
        userService.resetPassword(vo);

        return R.ok();
    }

    @Operation(summary = "绑定邮箱", description = "为当前登录账户绑定邮箱，发送验证码到邮箱", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("bindEmail")
    public R<Void> bindEmail(@Validated @RequestBody UserVo.BindEmailVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        userService.bindEmail(vo);
        return R.ok();
    }

    @Operation(summary = "验证绑定邮箱", description = "验证绑定邮箱的验证码", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("bindEmailVerify")
    public R<Void> bindEmailVerify(@Validated @RequestBody UserVo.BindEmailVerifyVo vo) {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        userService.bindEmailVerify(vo);
        return R.ok();
    }

    @Operation(summary = "解绑邮箱", description = "解绑当前账户的邮箱（系统要求绑定邮箱时不可用）", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("unbindEmail")
    public R<Void> unbindEmail() {

        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }

        userService.unbindEmail();
        return R.ok();
    }

    @Operation(summary = "获取用户当前会话列表", description = "获取当前登录用户的未失效会话", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("listActiveSessions")
    public R<List<UserSession>> listActiveSessions()
    {
        return R.ok(userSessionService.queryUserActiveSessions(securityUtil.getUserId()));
    }

    @Operation(summary = "注销所有会话", description = "注销当前用户的所有会话（包括当前会话）", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("revokeAllSessions")
    public R<Void> revokeAllSessions() {
        userSessionService.revokeAllSessions(securityUtil.getUserId());
        return R.ok();
    }

    @Operation(summary = "注销指定会话", description = "根据会话ID注销指定的会话", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("revokeSessionById")
    public R<Void> revokeSessionById(@Validated @RequestBody UserVo.RevokeSessionVo vo) {
        userSessionService.revokeSessionById(vo.getSessionId(), securityUtil.getUserId());
        return R.ok();
    }

}
