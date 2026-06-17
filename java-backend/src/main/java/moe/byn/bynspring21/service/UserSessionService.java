package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.UserSession;

import java.util.Date;
import java.util.List;

public interface UserSessionService extends IService<UserSession> {
    UserSession addSessionRecord(UserSession userSession);

    List<UserSession> queryUserActiveSessions(String userId);

    UserSession updateSessionRecordByRefreshToken(String refreshToken, String newAccessToken, String newRefreshToken, Date expireTime);

    void removeSessionRecordByRefreshToken(String refreshToken);

    void revokeSessionById(String sessionId, String userId);

    void revokeAllSessions(String userId);
}
