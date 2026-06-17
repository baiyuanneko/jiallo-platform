package moe.byn.bynspring21.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialAdminRunner implements ApplicationRunner {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${myapp.initialAdmin.username:}")
    private String initialAdminUsername;

    @Value("${myapp.initialAdmin.password:}")
    private String initialAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (initialAdminUsername == null || initialAdminUsername.isBlank()) {
            log.info("未配置 myapp.initialAdmin.username，跳过初始管理员创建");
            return;
        }

        User existing = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, initialAdminUsername)
        );
        if (existing != null) {
            log.info("初始管理员账户 [{}] 已存在，跳过创建", initialAdminUsername);
            return;
        }

        User admin = User.builder()
                .username(initialAdminUsername)
                .displayName(initialAdminUsername)
                .password(passwordEncoder.encode(initialAdminPassword))
                .passwordUpdatedAt(new Date())
                .emailVerified(true)
                .roleType(RoleType.ADMIN)
                .banned(false)
                .build();

        userService.save(admin);
        log.info("初始管理员账户 [{}] 创建成功", initialAdminUsername);
    }
}
