package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;

import java.util.concurrent.TimeUnit;

public interface UserLogService extends IService<UserLog> {
    UserLog addRecordWithMessage(UserLogBehaviour behaviour, String message);

    UserLog addRecord(UserLogBehaviour behaviour);

    UserLog addRecordByUserId(UserLogBehaviour behaviour, String userId);

    Long countBehaviourByCurrentIp(UserLogBehaviour behaviour, Integer duration, TimeUnit timeUnit);

    Long countBehaviourByUserId(UserLogBehaviour behaviour, String userId, Integer duration, TimeUnit timeUnit);
}
