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

/**
 * 管理员控制器
 * 
 * 该控制器负责处理管理员专属的用户管理操作。
 * 所有接口路径都以 /api/admin 为前缀。
 * 
 * 主要功能：
 * - 获取用户列表：所有已认证用户可访问
 * - 添加用户：仅 ADMIN 角色可操作
 * - 删除用户：仅 ADMIN 角色可操作
 * 
 * @author 系统架构
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户数据传输对象
     * 
     * 用于返回用户列表时的数据格式，避免暴露敏感信息（如密码）。
     */
    public record UserDTO(Long id, String username, String name, Role role) {}

    /**
     * 检查当前用户是否为管理员
     * 
     * 从安全上下文中获取当前用户的权限，判断是否具有 ADMIN 角色。
     * 
     * @return 如果当前用户是管理员返回true，否则返回false
     */
    private boolean checkIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin;
    }

    /**
     * 获取所有用户列表
     * 
     * 查询系统中所有用户的基本信息。
     * 所有已登录认证的用户都可以访问此接口。
     * 
     * 接口路径: GET /api/admin/users
     * 
     * @return 用户列表，包含用户的ID、用户名、姓名和角色信息
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
     * 添加新用户
     * 
     * 在系统中创建一个新的用户账号，默认角色为 USER（技术员）。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: POST /api/admin/users
     * 
     * @param request 包含用户名、密码和姓名的请求体
     * @return 创建成功的用户信息，参数缺失返回400，用户名冲突返回409，无权限返回403
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
     * 删除用户
     * 
     * 根据用户ID从系统中删除用户记录。
     * 注意：用户不能删除自己的账号。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: DELETE /api/admin/users/{id}
     * 
     * @param id 要删除的用户ID
     * @return 删除成功返回200，找不到返回404，删除自己返回400，无权限返回403
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

