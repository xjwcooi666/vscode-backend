package com.pigsty.backend.config;

import com.pigsty.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // 告诉 Spring 这是一个组件
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 确保每个请求只执行一次

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService; // 这是 ApplicationConfig 里的那个 Bean

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 从请求头中获取 "Authorization"
        final String authHeader = request.getHeader("Authorization");

        // 2. 检查 Header 是否存在，以及是否以 "Bearer " 开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // 如果不是 JWT，放行给下一个过滤器
            return;
        }

        // 3. 提取 Token (去掉 "Bearer " 前缀)
        final String token = authHeader.substring(7);
        final String username;

        try {
            // 4. 从 Token 中解析出用户名
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            // Token 无效 (过期、伪造等)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        // 5. 检查用户名存在，并且用户“尚未被认证”
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 6. 从数据库中加载用户信息
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 7. 验证 Token 是否有效
            if (jwtService.isTokenValid(token, userDetails)) {
                // 8. Token 有效！创建认证凭证
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // 我们用JWT，不需要密码
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 9. 将凭证设置到 Spring Security 的上下文中
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. 放行
        filterChain.doFilter(request, response);
    }
}
