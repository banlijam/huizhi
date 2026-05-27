package com.huizhipay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.user.dto.UserInfoResponse;
import com.huizhipay.user.entity.User;
import com.huizhipay.user.exception.AuthException;
import com.huizhipay.user.mapper.UserMapper;
import com.huizhipay.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserMapper userMapper;

    @Transactional
    public void registerUser(User user) {
        // 设置默认值（数据库已有默认值，此处可省略）
        user.setEmailVerified(false);
        user.setTotpEnabled(false);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Transactional
    public void updateEmailVerified(Long userId, boolean verified) {
        User user = new User();
        user.setId(userId);
        user.setEmailVerified(verified);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updatePassword(Long userId, String encodedPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updateStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void enableTotp(Long userId, String secret) {
        User user = new User();
        user.setId(userId);
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
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
        return userMapper.exists(wrapper);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + email);
        }
        return UserPrincipal.create(user);
    }
}