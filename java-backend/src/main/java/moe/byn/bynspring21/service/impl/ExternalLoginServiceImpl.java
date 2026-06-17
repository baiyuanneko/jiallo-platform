package moe.byn.bynspring21.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;
import moe.byn.bynspring21.entity.dto.ExternalLoginReturnValue;
import moe.byn.bynspring21.entity.vo.ExternalLoginVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.JwtTokenUtil;
import moe.byn.bynspring21.security.entity.TokenResponse;
import moe.byn.bynspring21.service.ExternalLoginService;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.UserLogService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.service.UserSessionService;
import moe.byn.bynspring21.utils.RedisUtils;
import moe.byn.bynspring21.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalLoginServiceImpl implements ExternalLoginService {

    @Value("${myapp.clientMode.isSsoClient}")
    private boolean isSsoClient;

    @Value("${myapp.clientMode.externalSsoAdminUserId}")
    private String externalSsoAdminUserId;

    @Value("${myapp.clientMode.ssoServerInvokeUrl}")
    private String ssoServerInvokeUrl;

    @Value("${myapp.clientMode.ssoClientSecret}")
    private String ssoClientSecret;

    @Value("${myapp.clientMode.ssoClientUniqueName}")
    private String ssoClientUniqueName;

    @Autowired
    private UserService userService;
    @Autowired
    private MinioService minioService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserLogService userLogService;
    @Autowired
    private UserSessionService userSessionService;
    @Autowired
    private RedisUtils redisUtils;

    @Value("${jwt.access-token-expiration-sec}")
    private long accessTokenExpirationSec;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(ExternalLoginVo.LoginVo vo) {
        if (!isSsoClient) {
            throw new BynBaseException("当前后端未声明为SSO Client");
        }

        String ssoToken = vo.getSsoToken();

        Map<String, Object> clientBody = new HashMap<>();

        clientBody.put("token", ssoToken);
        clientBody.put("clientUniqueName", ssoClientUniqueName);
        clientBody.put("clientSecret", ssoClientSecret);

        String bodyJson = JSON.toJSONString(clientBody);

        String jsonReturned = HttpRequest.post(ssoServerInvokeUrl).body(bodyJson).execute().body();

        ExternalLoginReturnValue loginCredential = JSON.parseObject(jsonReturned, new TypeReference<R<ExternalLoginReturnValue>>() {}).getData();

        // try find existing one

        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getExternalUserId, loginCredential.getUid()));

        if (user == null) {
            user = new User();
        }

        user.setUsername(loginCredential.getUsername());
        user.setDisplayName(loginCredential.getDisplayName());
        user.setEmail(loginCredential.getEmail());
        user.setEmailVerified(loginCredential.getEmailVerified());
        try {
            user.setRoleType(RoleType.valueOf(loginCredential.getRoleType()));
        } catch (Exception e) {
            throw new BynBaseException("无效的角色类型");
        }
        user.setBanned(false);
        user.setExternalUserId(loginCredential.getUid());

        userService.saveOrUpdate(user);

        String userId = user.getId();

        // 处理头像：删除旧头像并上传新头像
        String avatarBase64 = loginCredential.getAvatarBase64();
        if (avatarBase64 != null && !avatarBase64.trim().isEmpty()) {
            // 删除用户旧的所有头像
            minioService.deleteUserAvatars(userId);

            // 上传新的 Base64 头像
            String avatarUrl = minioService.uploadAvatarFromBase64(avatarBase64, userId);

            // 更新用户的头像 URL
            user.setAvatarUrl(avatarUrl);
            userService.updateById(user);
        }
        
        // 生成 Token
        String accessToken = jwtTokenUtil.generateAccessToken(userId);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userId);

        // 记录成功登录日志
        userLogService.addRecordByUserId(UserLogBehaviour.SUCCESSFUL_LOGIN, userId);

        // 创建用户会话记录
        userSessionService.addSessionRecord(UserSession.builder()
                .userId(userId)
                .jwtAccessToken(accessToken)
                .jwtRefreshToken(refreshToken)
                .ip(RequestUtils.getClientIp())
                .userAgent(RequestUtils.getUserAgent())
                .refreshTokenExpireTime(jwtTokenUtil.getExpirationFromToken(refreshToken))
                .build());

        // 清除用户缓存
        redisUtils.tryDel("user:" + userId);

        // 返回 Token Response
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationSec)
                .build();
    }
}
