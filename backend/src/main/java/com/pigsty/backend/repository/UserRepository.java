package com.pigsty.backend.repository;

import com.pigsty.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA 会自动帮我们实现这个方法
    // Spring Security 登录时会需要它
    Optional<User> findByUsername(String username);
}
