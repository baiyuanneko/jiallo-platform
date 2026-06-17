package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;
import moe.byn.bynspring21.mapper.UserLogMapper;
import moe.byn.bynspring21.service.UserLogService;
import moe.byn.bynspring21.utils.RequestUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserLogServiceImpl extends ServiceImpl<UserLogMapper, UserLog> implements UserLogService {

    @Override
    public UserLog addRecordWithMessage(UserLogBehaviour behaviour, String message){
        UserLog userLog = UserLog.builder()
                        .ip(RequestUtils.getClientIp())
                        .userAgent(RequestUtils.getUserAgent())
                        .behaviour(behaviour)
                        .message(message)
                .build();
        this.save(userLog);
        return userLog;
    }

    @Override
    public UserLog addRecord(UserLogBehaviour behaviour){
        UserLog userLog = UserLog.builder()
                .ip(RequestUtils.getClientIp())
                .userAgent(RequestUtils.getUserAgent())
                .behaviour(behaviour)
                .build();
        this.save(userLog);
        return userLog;
    }

    @Override
    public UserLog addRecordByUserId(UserLogBehaviour behaviour, String userId){
        UserLog userLog = UserLog.builder()
                .ip(RequestUtils.getClientIp())
                .userAgent(RequestUtils.getUserAgent())
                .behaviour(behaviour)
                .userId(userId)
                .build();
        this.save(userLog);
        return userLog;
    }

    @Override
    public Long countBehaviourByCurrentIp(UserLogBehaviour behaviour, Integer duration, TimeUnit timeUnit) {
        return this.count(new LambdaQueryWrapper<UserLog>()
                .eq(UserLog::getBehaviour, behaviour)
                .eq(UserLog::getIp, RequestUtils.getClientIp())
                .gt(UserLog::getCreateTime, new Date(System.currentTimeMillis() - timeUnit.toMillis(duration))));
    }

    @Override
    public Long countBehaviourByUserId(UserLogBehaviour behaviour, String userId, Integer duration, TimeUnit timeUnit) {
        return this.count(new LambdaQueryWrapper<UserLog>()
                .eq(UserLog::getBehaviour, behaviour)
                .eq(UserLog::getUserId, userId)
                .gt(UserLog::getCreateTime, new Date(System.currentTimeMillis() - timeUnit.toMillis(duration))));
    }
}
