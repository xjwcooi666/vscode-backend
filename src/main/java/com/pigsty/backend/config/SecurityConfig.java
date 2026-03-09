package com.pigsty.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security 安全配置类
 * 
 * 该类负责配置整个应用的安全策略，包括：
 * - HTTP 安全过滤器链配置
 * - JWT 认证过滤器集成
 * - 会话管理策略（无状态）
 * - CORS 跨域配置
 * - 接口访问权限控制
 * 
 * 主要配置特点：
 * - 禁用 CSRF 保护（适用于前后端分离架构）
 * - 使用 JWT Token 进行无状态认证
 * - 认证接口公开，其他接口需要认证
 * 
 * @author 系统架构
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    /**
     * 配置安全过滤器链
     * 
     * 定义 Spring Security 的 HTTP 安全策略，包括：
     * 1. 禁用 CSRF 保护（前后端分离架构不需要）
     * 2. 启用 CORS 跨域支持
     * 3. 配置接口访问权限规则
     * 4. 设置会话管理策略为无状态（STATELESS）
     * 5. 集成 JWT 认证过滤器
     * 
     * 权限规则：
     * - OPTIONS 请求：全部允许
     * - /api/auth/**：公开访问（注册和登录）
     * - 其他 /api/** 接口：需要认证
     * 
     * @param http HttpSecurity 配置对象
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/devices/**").authenticated()
                .requestMatchers("/api/pigsties/**").authenticated()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/data/**").authenticated()
                .requestMatchers("/api/warnings/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置 CORS 跨域支持
     * 
     * 配置跨域资源共享（CORS）策略，允许前端应用访问后端 API。
     * 
     * 主要配置：
     * - 允许的源：http://localhost:3000（前端开发服务器）
     * - 允许的方法：GET, POST, PUT, DELETE, OPTIONS
     * - 允许的请求头：所有
     * - 允许携带凭证
     * 
     * @return WebMvcConfigurer 配置对象
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
