package moe.byn.bynspring21.security;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    /**
     * 允许通过用户名或邮箱登录
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userService.getById(username);

        if (user == null) {
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        }

        if (user == null) {
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, username)
                    .eq(User::getEmailVerified, true));
        }

        if (user == null) {
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, username));
        }

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 检查用户角色是否为 AWAIT_EMAIL_VERIFICATION
        boolean isAccountNonExpired = user.getRoleType() != RoleType.AWAIT_EMAIL_VERIFICATION;

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                true,
                isAccountNonExpired,
                true,
                !user.getBanned(),
                getAuthorities(user)
        );
    }

    /**
     * 获取用户权限
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRoleType().name());
        return Collections.singletonList(authority);
    }
}
