package com.pigsty.backend.controller;

import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// [!!! 关键改动 !!!] 我们移除了所有 @PreAuthorize 注解

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 内部 DTO (数据传输对象)，用于安全地返回用户信息
    public record UserDTO(Long id, String username, String name, Role role) {}

    /**
     * 手动检查当前用户是否为 ADMIN
     * @return true 如果是 ADMIN, 否则抛出异常
     */
    private boolean checkIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
             // 如果不是 Admin，我们可以在这里记录日志或直接返回错误
             // 为了安全，我们不抛出异常，而是返回 false
             return false;
        }
        return true;
    }

    /**
     * API 1: 获取所有用户 (仅限 Admin)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        // [!!! 手动安全检查 !!!]
        if (!checkIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 禁止访问
        }

        List<UserDTO> users = userRepository.findAll().stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        // [!!! 关键修复 !!!] 检查 name 是否为 null
                        (user.getName() != null) ? user.getName() : "No Name", 
                        user.getRole()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * API 2: 添加新用户 (技术员) (仅限 Admin)
     */
    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> request) {
        // [!!! 手动安全检查 !!!]
        if (!checkIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMINs can add users.");
        }

        String username = request.get("username");
        String password = request.get("password");
        String name = request.get("name"); // "管理员", "技术员A"

        if (username == null || password == null || name == null) {
            return ResponseEntity.badRequest().body("Username, password, and name are required.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username is already taken.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(Role.USER) // 管理员添加的账号默认为 USER (技术员)
                .build();

        userRepository.save(user);
        return ResponseEntity.ok(new UserDTO(user.getId(), user.getUsername(), user.getName(), user.getRole()));
    }

    /**
     * API 3: 删除用户 (仅限 Admin)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // [!!! 手动安全检查 !!!]
        if (!checkIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 防止 Admin 删除自己
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (user.getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // 400 不能删除自己
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
