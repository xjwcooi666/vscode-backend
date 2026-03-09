package com.pigsty.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 用户实体类
 * 
 * 该类表示系统中的一个用户，实现了 Spring Security 的 UserDetails 接口，
 * 用于身份认证和授权。用户可以拥有不同的角色（普通用户或管理员）。
 * 
 * @author 系统架构
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    
    private String name; 

    @Enumerated(EnumType.STRING)
    private Role role;

    // ---- UserDetails 接口方法 ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // [!!! 最终修复 !!!]
        // 我们必须在这里添加 "ROLE_" 前缀
        // 这样 Spring Security 和我们的手动检查才能正确识别
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
