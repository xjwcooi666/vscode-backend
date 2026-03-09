package com.pigsty.backend.controller;

import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.repository.UserRepository;
import com.pigsty.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 认证控制器
 * 
 * 该控制器负责处理用户注册、登录等认证相关的 HTTP 请求。
 * 所有接口路径都以 /api/auth 为前缀。
 * 
 * 主要功能：
 * - 用户注册：支持新用户注册，首个用户自动获得 ADMIN 角色
 * - 用户登录：验证用户凭证并返回 JWT Token
 * 
 * @author 系统架构
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 用户注册接口
     * 
     * 处理新用户注册请求，执行以下逻辑：
     * 1. 检查用户名是否已被占用
     * 2. 判断是否为系统首个用户，首个用户自动获得 ADMIN 角色
     * 3. 使用 BCrypt 加密用户密码
     * 4. 保存用户信息到数据库
     * 
     * 接口路径: POST /api/auth/register
     * 
     * @param request 包含 username 和 password 的请求体 Map
     * @return 注册成功返回成功消息和用户角色，用户名冲突返回 409 状态码
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Username is already taken!");
        }

        boolean isFirstUser = userRepository.count() == 0;
        
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(isFirstUser ? Role.ADMIN : Role.USER)
                .name(username)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully! Role: " + user.getRole());
    }

    /**
     * 用户登录接口
     * 
     * 处理用户登录请求，执行以下逻辑：
     * 1. 使用 Spring Security AuthenticationManager 验证用户名和密码
     * 2. 验证通过后从数据库获取用户详细信息
     * 3. 调用 JwtService 生成 JWT Token
     * 4. 返回 Token 给前端用于后续请求认证
     * 
     * 接口路径: POST /api/auth/login
     * 
     * @param request 包含 username 和 password 的请求体 Map
     * @return 登录成功返回包含 JWT Token 的 JSON 对象，验证失败返回 401 状态码
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Authentication authentication;
        try {
             authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * 刷新 Token 接口
     * 
     * 处理 Token 刷新请求，执行以下逻辑：
     * 1. 从请求头中提取当前 Token
     * 2. 验证 Token 的有效性
     * 3. 从 Token 中获取用户名
     * 4. 从数据库加载用户详细信息
     * 5. 生成新的 Token 并返回
     * 
     * 接口路径: POST /api/auth/refresh
     * 
     * @param request HTTP 请求对象
     * @return 刷新成功返回包含新 JWT Token 的 JSON 对象，验证失败返回 401 状态码
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }

        final String token = authHeader.substring(7);
        final String username;

        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 验证当前 Token 是否有效（即使过期，只要签名正确就允许刷新）
        if (!jwtService.validateTokenSignature(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        String newToken = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of("token", newToken));
    }
}
