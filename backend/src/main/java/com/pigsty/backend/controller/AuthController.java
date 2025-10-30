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

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // 认证接口都在 /api/auth 路径下
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
     * API 1: 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: Username is already taken!");
        }

        // [!! 关键改动 !!] 
        // 检查数据库里是不是还没有用户
        boolean isFirstUser = userRepository.count() == 0;
        
        // 创建新用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password)) // 必须加密密码！
                // [!! 关键改动 !!] 如果是第一个用户，设为 ADMIN, 否则设为 USER
                .role(isFirstUser ? Role.ADMIN : Role.USER) 
                .name(username) // [!! 新增 !!] 默认让 name 和 username 一样
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully! Role: " + user.getRole());
    }

    /**
     * API 2: 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // 1. 使用 Spring Security 的 AuthenticationManager 验证身份
        Authentication authentication;
        try {
             authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        
        // 2. 身份验证成功，获取 User 对象
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 3. 生成 JWT Token
        String token = jwtService.generateToken(user);

        // 4. 返回 Token 给前端
        return ResponseEntity.ok(Map.of("token", token));
    }
}