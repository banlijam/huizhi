package com.huizhipay.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantApiKeyAuthenticationPort;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class MerchantApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final MerchantApiKeyAuthenticationPort authenticator; private final ObjectMapper objectMapper;
    @Override protected boolean shouldNotFilter(HttpServletRequest r){ return !r.getRequestURI().startsWith("/api/v1/test/"); }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String env="TEST";
        String key=request.getHeader("X-HuizhiPay-Api-Key"); if(key==null) key=request.getHeader("X-HuizhiPay-Test-Key");
        String scope="GET".equals(request.getMethod())?"payments:read":"payments:write";
        try{
            var principal=authenticator.authenticate(key,env,scope);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_MERCHANT_API"))));
            chain.doFilter(request,response);
        }catch(BizException e){ response.setStatus(e.getCode()); response.setContentType(MediaType.APPLICATION_JSON_VALUE); objectMapper.writeValue(response.getWriter(),R.fail(e.getCode(),e.getMessage())); }
    }
}
