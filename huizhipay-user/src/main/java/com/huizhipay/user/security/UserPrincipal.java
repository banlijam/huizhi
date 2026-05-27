package com.huizhipay.user.security;

import com.huizhipay.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private Long id;
    private String email;
    private String password;
    private boolean emailVerified;
    private boolean totpEnabled;
    private Integer status;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 从 User 实体构造 UserPrincipal
     */
    public static UserPrincipal create(User user) {
        // 这里可扩展角色/权限，目前给一个默认 ROLE_USER
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getEmailVerified(),
                user.getTotpEnabled(),
                user.getStatus(),
                authorities
        );
    }

    // ------- UserDetails 接口方法 -------

    @Override
    public String getUsername() {
        return email; // 使用 email 作为用户名
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1; // 1 表示启用
    }

    @Override
    public boolean isEnabled() {
        return emailVerified; // 邮箱验证后方可登录
    }
}