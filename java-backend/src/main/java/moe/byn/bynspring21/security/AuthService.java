package moe.byn.bynspring21.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;
import moe.byn.bynspring21.entity.vo.UserVo;
import moe.byn.bynspring21.exception.EmailVerificationRequiredException;
import moe.byn.bynspring21.security.entity.TokenResponse;
import moe.byn.bynspring21.service.AntiBruteForceService;
import moe.byn.bynspring21.service.UserLogService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.service.UserSessionService;
import moe.byn.bynspring21.utils.CaptchaUtils;
import moe.byn.bynspring21.utils.RedisUtils;
import moe.byn.bynspring21.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private AntiBruteForceService antiBruteForceService;
    @Autowired
    private UserLogService userLogService;
    @Autowired
    private UserSessionService userSessionService;
    @Autowired
    private RedisUtils redisUtils;

    @Value("${jwt.access-token-expiration-sec}")
    private long accessTokenExpirationSec;
    @Autowired
    private CaptchaUtils captchaUtils;

    /**
     * 用户登录
     */
    public TokenResponse login(UserVo.LoginUserVo request, Boolean skipCaptchaVerification) throws AuthenticationException {

        String userId = null;

        try {
            UserDetails userDetail = userDetailsService.loadUserByUsername(request.getUsername());
            userId = userDetail.getUsername();
        } catch (Exception ignored) {

        }

        antiBruteForceService.checkBeforeLogin(userId);

        if (!skipCaptchaVerification) {
            captchaUtils.verifyCaptcha(request.getVerifyCodeId(), "login", request.getVerifyCode());
        }
        // 进行身份验证
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AccountExpiredException e) {
            // 账户等待邮箱验证 - 获取用户信息
            User user = userService.getById(userId);
            if (user != null) {
                userLogService.addRecordByUserId(UserLogBehaviour.SUCCESSFUL_LOGIN, userId);
                throw new EmailVerificationRequiredException(user.getId(), user.getEmail());
            }
            throw new RuntimeException("用户不存在");
        } catch (AuthenticationException e) {
            userLogService.addRecordByUserId(UserLogBehaviour.FAILED_LOGIN, userId);
            throw e;
        }

        // 获取用户信息
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));

        // 生成 Token
        String accessToken = jwtTokenUtil.generateAccessToken(user.getId());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        userLogService.addRecordByUserId(UserLogBehaviour.SUCCESSFUL_LOGIN, userId);

        userSessionService.addSessionRecord(UserSession.builder()
                        .userId(user.getId())
                        .jwtAccessToken(accessToken)
                        .jwtRefreshToken(refreshToken)
                        .ip(RequestUtils.getClientIp())
                        .userAgent(RequestUtils.getUserAgent())
                        .refreshTokenExpireTime(jwtTokenUtil.getExpirationFromToken(refreshToken))
                .build());

        redisUtils.tryDel("user:" + user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationSec) // 1小时
                .build();
    }

    /**
     * 刷新 Token
     */
    public TokenResponse refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new RuntimeException("无效的 Refresh Token");
        }

        // 检查是否在黑名单中
        if (blacklistService.isBlacklisted(refreshToken)) {
            throw new RuntimeException("Token 已失效");
        }

        // 验证 Token 类型
        String tokenType = jwtTokenUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("Token 类型错误");
        }

        // 获取用户 ID
        String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);

        // 生成新的 Token
        String newAccessToken = jwtTokenUtil.generateAccessToken(userId);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userId);

        userSessionService.updateSessionRecordByRefreshToken(refreshToken, newAccessToken, newRefreshToken, jwtTokenUtil.getExpirationFromToken(newRefreshToken));

        // 将旧的 Refresh Token 加入黑名单
        blacklistService.addToBlacklist(refreshToken, jwtTokenUtil.getExpirationFromToken(refreshToken));

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationSec) // 一小时
                .build();
    }

    /**
     * 登出
     */
    public void logout(String accessToken, String refreshToken) {
        // 将 Access Token 加入黑名单
        if (accessToken != null && jwtTokenUtil.validateToken(accessToken)) {
            blacklistService.addToBlacklist(accessToken, jwtTokenUtil.getExpirationFromToken(accessToken));
        }

        // 将 Refresh Token 加入黑名单
        if (refreshToken != null && jwtTokenUtil.validateToken(refreshToken)) {
            blacklistService.addToBlacklist(refreshToken, jwtTokenUtil.getExpirationFromToken(refreshToken));
        }

        redisUtils.tryDel("user:" + jwtTokenUtil.getUserIdFromToken(accessToken));

        userSessionService.removeSessionRecordByRefreshToken(refreshToken);
    }
}
