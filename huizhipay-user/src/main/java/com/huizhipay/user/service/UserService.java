package com.huizhipay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.user.dto.UserInfoResponse;
import com.huizhipay.user.entity.User;
import com.huizhipay.user.exception.AuthException;
import com.huizhipay.user.mapper.UserMapper;
import com.huizhipay.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserMapper userMapper;

    @Transactional
    public void registerUser(User user) {
        log.info("[User] 注册用户 email={}", user.getEmail());
        // 设置默认值（数据库已有默认值，此处可省略）
        user.setEmailVerified(false);
        user.setTotpEnabled(false);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("[User] 注册完成 userId={}, email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public void updateEmailVerified(Long userId, boolean verified) {
        log.info("[User] 更新邮箱验证 userId={}, verified={}", userId, verified);
        User user = new User();
        user.setId(userId);
        user.setEmailVerified(verified);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updatePassword(Long userId, String encodedPassword) {
        log.info("[User] 更新密码 userId={}", userId);
        User user = new User();
        user.setId(userId);
        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updateStatus(Long userId, Integer status) {
        log.info("[User] 更新状态 userId={}, status={}", userId, status);
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void enableTotp(Long userId, String secret) {
        log.info("[User] 启用TOTP userId={}", userId);
        User user = new User();
        user.setId(userId);
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserInfoResponse getUserInfo(Long userId) {
        log.debug("[User] 查询用户信息 userId={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("[User] 用户不存在 userId={}", userId);
            throw new AuthException("用户不存在");
        }
        return new UserInfoResponse(
                user.getId()
                , user.getEmail()
                , user.getNickname()
                , user.getEmailVerified()
                , user.getTotpEnabled()
                , user.getStatus());
    }

    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        boolean exists = userMapper.exists(wrapper);
        log.debug("[User] existsByEmail email={}, exists={}", email, exists);
        return exists;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("[User] loadUserByUsername email={}", email);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (user == null) {
            log.warn("[User] 登录失败：用户不存在 email={}", email);
            throw new UsernameNotFoundException("用户不存在: " + email);
        }
        return UserPrincipal.create(user);
    }
}
