package com.pigsty.backend.controller;

import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // (UserDTO 保持不变)
    public record UserDTO(Long id, String username, String name, Role role) {}

    /**
     * 手动检查当前用户是否为 ADMIN
     */
    private boolean checkIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin;
    }

    /**
     * API 1: 获取所有用户
     * [!!! 关键修复 !!!]
     * 移除手动的 `checkIsAdmin()` 检查。
     * 现在 `SecurityConfig` 里的 `authenticated()` 规则
     * 允许 *任何* 登录的用户（包括技术员）查看用户列表。
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        // if (!checkIsAdmin()) {  <-- [!!! 关键修复 !!!] 删除这一行
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); 
        // } <-- [!!! 关键修复 !!!] 删除这一行

        List<UserDTO> users = userRepository.findAll().stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        (user.getName() != null) ? user.getName() : "No Name", 
                        user.getRole()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * API 2: 添加新用户 (技术员)
     * (保持不变，仍然受 Admin 保护)
     */
    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> request) {
        if (!checkIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMINs can add users.");
        }

        String username = request.get("username");
        String password = request.get("password");
        String name = request.get("name");

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
     * API 3: 删除用户 (按 ID)
     * (保持不变，仍然受 Admin 保护)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!checkIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
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

