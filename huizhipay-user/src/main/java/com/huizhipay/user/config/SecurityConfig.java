package com.huizhipay.user.config;

import com.huizhipay.user.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${huizhipay.dummy.checkout-result-enabled:false}")
    private boolean dummyCheckoutResultEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)   // 禁用CSRF（使用JWT）
                .authorizeHttpRequests(auth -> {
                    // 买家只可凭随机 checkoutToken 查询单笔订单；后台建单和列表必须登录。
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/dummy/orders/*").permitAll();
                    if (dummyCheckoutResultEnabled) {
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/dummy/orders/*/result").permitAll();
                    } else {
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/dummy/orders/*/result").denyAll();
                    }
                    auth.requestMatchers("/api/v1/auth/**", "/actuator/health",
                                    "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                            .permitAll();
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
