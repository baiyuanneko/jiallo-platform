package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.UserSession;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.UserSessionMapper;
import moe.byn.bynspring21.security.JwtTokenUtil;
import moe.byn.bynspring21.security.TokenBlacklistService;
import moe.byn.bynspring21.service.UserSessionService;
import moe.byn.bynspring21.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Override
    public UserSession addSessionRecord(UserSession userSession) {
        this.save(userSession);
        return userSession;
    }

    @Override
    public List<UserSession> queryUserActiveSessions(String userId) {
        List<UserSession> sessions = this.list(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, userId)
                .gt(UserSession::getRefreshTokenExpireTime, new Date()));

        for(UserSession session : sessions) {
            if (session.getJwtAccessToken().equals(RequestUtils.getRequest().getHeader("Authorization").replace("Bearer ", "").trim())){
                session.setIsCurrentSession(true);
            }
        }

        return sessions;
    }

    @Override
    public UserSession updateSessionRecordByRefreshToken(String refreshToken, String newAccessToken, String newRefreshToken, Date newRefreshTokenExpireTime) {
        UserSession userSession = this.getOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getJwtRefreshToken, refreshToken));

        if (userSession != null) {
            userSession.setJwtAccessToken(newAccessToken);
            userSession.setJwtRefreshToken(newRefreshToken);
            userSession.setRefreshTokenExpireTime(newRefreshTokenExpireTime);
            this.updateById(userSession);
        }

        return userSession;
    }

    @Override
    public void removeSessionRecordByRefreshToken(String refreshToken) {
        this.remove(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getJwtRefreshToken, refreshToken));
    }

    @Override
    public void revokeSessionById(String sessionId, String userId) {
        // 查询会话
        UserSession session = this.getById(sessionId);

        if (session == null) {
            throw new BynBaseException("会话不存在");
        }

        // 验证会话属于当前用户
        if (!session.getUserId().equals(userId)) {
            throw new BynBaseException("无权注销此会话");
        }

        // 将 access token 和 refresh token 加入黑名单
        if (session.getJwtAccessToken() != null) {
            tokenBlacklistService.addToBlacklist(
                session.getJwtAccessToken(),
                new Date(System.currentTimeMillis() + accessTokenExpiration) // 1小时后过期
            );
        }

        if (session.getJwtRefreshToken() != null && session.getRefreshTokenExpireTime() != null) {
            tokenBlacklistService.addToBlacklist(
                session.getJwtRefreshToken(),
                session.getRefreshTokenExpireTime()
            );
        }

        // 删除会话记录
        this.removeById(sessionId);

        log.info("User {} revoked session {}", userId, sessionId);
    }

    @Override
    public void revokeAllSessions(String userId) {
        // 查询用户所有活跃会话
        List<UserSession> sessions = this.list(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, userId)
                .gt(UserSession::getRefreshTokenExpireTime, new Date()));

        // 将所有 token 加入黑名单
        for (UserSession session : sessions) {
            if (session.getJwtAccessToken() != null) {
                tokenBlacklistService.addToBlacklist(
                    session.getJwtAccessToken(),
                    new Date(System.currentTimeMillis() + accessTokenExpiration) // 1小时后过期
                );
            }

            if (session.getJwtRefreshToken() != null && session.getRefreshTokenExpireTime() != null) {
                tokenBlacklistService.addToBlacklist(
                    session.getJwtRefreshToken(),
                    session.getRefreshTokenExpireTime()
                );
            }
        }

        // 删除所有会话记录
        this.remove(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, userId));

        log.info("User {} revoked all {} sessions", userId, sessions.size());
    }

}
