package moe.byn.bynspring21.security;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityUtil {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtils redisUtils;

    @Value("${myapp.user.redisCacheTime}")
    private long userCacheTime;

    /**
     * 获取Authentication
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取用户
     *
     * @return
     */
    public User getUser() {
        log.info("进入SecurityUtil.getUser()");
        try {
            var userDetail = (UserDetails) getAuthentication().getPrincipal();

            if (redisUtils.hasKey("user:" + userDetail.getUsername())){
                log.info("从Redis缓存中获取userId:" + userDetail.getUsername());
                return (User) redisUtils.get("user:" + userDetail.getUsername());
            } else {
                log.info("从数据库中获取userId:" + userDetail.getUsername());
                User user = userService.getById(userDetail.getUsername());
                redisUtils.set("user:" + userDetail.getUsername(), user, userCacheTime);
                return user;
            }

        } catch (Exception e) {
            throw new BynBaseException("登录状态过期");
        }
    }

    public String getUserId() {
        try {
            var userDetail = (UserDetails) getAuthentication().getPrincipal();
            return userDetail.getUsername();
        } catch (Exception e) {
            throw new BynBaseException("登录状态过期");
        }
    }

}
