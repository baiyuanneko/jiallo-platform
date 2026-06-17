package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;
import moe.byn.bynspring21.exception.AntiBruteForceException;
import moe.byn.bynspring21.service.AntiBruteForceService;
import moe.byn.bynspring21.service.UserLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AntiBruteForceServiceImpl implements AntiBruteForceService {

    @Autowired
    private UserLogService userLogService;

    @Override
    public void checkBeforeLogin(String userId) {
        if (userLogService.countBehaviourByCurrentIp(
                UserLogBehaviour.FAILED_LOGIN,
                1,
                TimeUnit.DAYS) > 30) {
            throw new AntiBruteForceException("该IP今天登录错误次数过多，请明天再试");
        }

        if (userLogService.countBehaviourByUserId(
                UserLogBehaviour.FAILED_LOGIN,
                userId,
                4,
                TimeUnit.HOURS) > 10) {
            throw new AntiBruteForceException("该用户短期内登录错误次数过多，请等几小时后再试");
        }
    }

    @Override
    public void checkBeforeRegister() {
        if (userLogService.countBehaviourByCurrentIp(
                UserLogBehaviour.SUCCESSFUL_REGISTER,
                1,
                TimeUnit.DAYS) > 5) {
            throw new AntiBruteForceException("该IP今天注册账号过于频繁，请之后再试");
        }
    }

    @Override
    public void checkBeforeEmailCodeVerification(String userId) {
        if (userLogService.countBehaviourByUserId(
                UserLogBehaviour.EMAIL_CODE_INVALID,
                userId,
                10,
                TimeUnit.MINUTES) > 5) {
            throw new AntiBruteForceException("短期内输入过多错误验证码，请等一会儿后再试");
        }
    }

    @Override
    public void checkBeforeSendEmailCode(String userEmail) {
        if (userLogService.countBehaviourByCurrentIp(
                UserLogBehaviour.SEND_EMAIL_CODE,
                1,
                TimeUnit.DAYS) > 5) {
            throw new AntiBruteForceException("该IP今天请求发送邮箱验证码过于频繁，请之后再试");
        }

        if(userLogService.count(new LambdaQueryWrapper<UserLog>().eq(UserLog::getMessage, userEmail).gt(UserLog::getCreateTime, new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))) > 5) {
            throw new AntiBruteForceException("该邮箱今天请求发送验证码过于频繁，请等明天再试");
        }
    }
}
