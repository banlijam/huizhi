package com.huizhipay.user.config;

import com.huizhipay.user.security.JwtAuthenticationFilter;
import com.huizhipay.user.security.MerchantApiKeyAuthenticationFilter;
import com.huizhipay.user.security.CsrfCookieFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MerchantApiKeyAuthenticationFilter merchantApiKeyAuthenticationFilter;
    private final CsrfCookieFilter csrfCookieFilter;

    @Value("${huizhipay.dummy.checkout-result-enabled:false}")
    private boolean dummyCheckoutResultEnabled;

    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite(cookieSecure ? "Strict" : "Lax")
                .secure(cookieSecure).path("/"));
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/v1/test/**", "/webhook/**",
                                "/api/v1/dummy/orders/*/result",
                                "/api/v1/dummy/orders/*/payment-methods/*"))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/webhook/transfi").permitAll();
                    auth.requestMatchers("/api/v1/test/payments/**").hasRole("MERCHANT_API");
                    // 买家只可凭随机 checkoutToken 查询单笔订单；后台建单和列表必须登录。
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/dummy/orders/*").permitAll();
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/v1/dummy/orders/*/payment-methods/*").permitAll();
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(merchantApiKeyAuthenticationFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class);

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
