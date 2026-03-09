package com.pigsty.backend.config;

import com.pigsty.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
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

/**
 * JWT 认证过滤器
 * 
 * 该过滤器负责在每个请求到达控制器之前进行 JWT Token 认证。
 * 继承自 OncePerRequestFilter，确保每个请求只执行一次认证逻辑。
 * 
 * 主要功能：
 * - 从请求头中提取和验证 JWT Token
 * - 解析 Token 获取用户名
 * - 验证 Token 的有效性和过期时间
 * - 将认证信息设置到 Spring Security 上下文中
 * 
 * @author 系统架构
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 判断是否应该跳过当前请求的过滤
     * 
     * 以下情况跳过 JWT 认证：
     * - 认证相关接口（/api/auth/）：注册和登录不需要认证
     * - OPTIONS 请求：预检请求不需要认证
     * 
     * @param request HTTP 请求对象
     * @return 如果应该跳过过滤返回true，否则返回false
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    /**
     * 执行 JWT 认证过滤逻辑
     * 
     * 对请求进行 JWT Token 认证，执行以下步骤：
     * 1. 从 Authorization 请求头中提取 Token
     * 2. 验证 Token 格式是否正确（Bearer 前缀）
     * 3. 从 Token 中解析用户名
     * 4. 从数据库加载用户详细信息
     * 5. 验证 Token 的有效性
     * 6. 将认证信息设置到 Spring Security 上下文中
     * 7. 放行请求到下一个过滤器
     * 
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final String username;

        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Token 无效（过期或被篡改），返回 401 错误
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
