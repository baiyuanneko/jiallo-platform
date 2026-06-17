package moe.byn.bynspring21.security;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.blacklist-prefix}")
    private String blacklistPrefix;

    /**
     * 将 Token 加入黑名单
     */
    public void addToBlacklist(String token, Date expiration) {
        String key = blacklistPrefix + token;
        long timeout = expiration.getTime() - System.currentTimeMillis();

        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, "1", timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = blacklistPrefix + token;
        return redisTemplate.hasKey(key);
    }
}
