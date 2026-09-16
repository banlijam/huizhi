package com.huizhipay.user.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component public class CsrfCookieFilter extends OncePerRequestFilter{
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  CsrfToken token=(CsrfToken)request.getAttribute(CsrfToken.class.getName());if(token!=null)token.getToken();chain.doFilter(request,response);
 }
}
