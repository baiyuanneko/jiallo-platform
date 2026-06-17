package moe.byn.bynspring21.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.SsoClientPermissionType;
import moe.byn.bynspring21.entity.vo.SsoLoginVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.SsoClientService;
import moe.byn.bynspring21.service.SsoLoginService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.utils.RedisUtils;
import moe.byn.bynspring21.utils.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class SsoLoginServiceImpl implements SsoLoginService {

    @Autowired
    private SsoClientService ssoClientService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserService userService;

    @Override
    public String confirmLogin(SsoLoginVo.ConfirmLoginVo vo){
        User currentUser = securityUtil.getUser();
        SsoClient ssoClient = ssoClientService.getOne(new LambdaQueryWrapper<SsoClient>()
                .eq(SsoClient::getClientUniqueName, vo.getClientUniqueName()));
        if (ssoClient == null) {
            throw new BynBaseException("应用不存在");
        }

        JSONObject userInfo = new JSONObject();

        SsoClientPermissionType permissionType = ssoClient.getClientPermissionType();

        switch (permissionType) {
            case UID_ONLY:
                userInfo.put("uid", currentUser.getId());
                break;
            case ROLETYPE_ONLY:
                userInfo.put("roleType", currentUser.getRoleType().getName());
                break;
            case UID_AND_ROLETYPE:
                userInfo.put("uid", currentUser.getId());
                userInfo.put("roleType", currentUser.getRoleType().getName());
                break;
            case BASIC_INFO:
                userInfo.put("uid", currentUser.getId());
                userInfo.put("username", currentUser.getUsername());
                userInfo.put("displayName", currentUser.getDisplayName());
                userInfo.put("avatarBase64", userService.getMyAvatarBase64());
                break;
            case BASIC_INFO_WITH_ROLETYPE:
                userInfo.put("uid", currentUser.getId());
                userInfo.put("username", currentUser.getUsername());
                userInfo.put("displayName", currentUser.getDisplayName());
                userInfo.put("roleType", currentUser.getRoleType().getName());
                userInfo.put("avatarBase64", userService.getMyAvatarBase64());
                break;
            case BASIC_INFO_WITH_EMAIL:
                userInfo.put("uid", currentUser.getId());
                userInfo.put("username", currentUser.getUsername());
                userInfo.put("displayName", currentUser.getDisplayName());
                userInfo.put("avatarBase64", userService.getMyAvatarBase64());
                userInfo.put("email", currentUser.getEmail());
                userInfo.put("emailVerified", currentUser.getEmailVerified());
                break;
            case BASIC_INFO_WITH_ROLETYPE_AND_EMAIL:
                userInfo.put("uid", currentUser.getId());
                userInfo.put("username", currentUser.getUsername());
                userInfo.put("displayName", currentUser.getDisplayName());
                userInfo.put("roleType", currentUser.getRoleType().getName());
                userInfo.put("avatarBase64", userService.getMyAvatarBase64());
                userInfo.put("email", currentUser.getEmail());
                userInfo.put("emailVerified", currentUser.getEmailVerified());
                break;
        }

        String ssoToken = SecureRandomUtils.randomString(32);
        String ssoClientUniqueName = ssoClient.getClientUniqueName();

        redisUtils.set(ssoClientUniqueName + ":" + ssoToken, userInfo.toJSONString(), 60 * 15);

        return ssoToken;
    }
}
