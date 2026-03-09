package com.pigsty.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 令牌服务
 * 
 * 该服务负责 JWT（JSON Web Token）的生成、解析和验证操作。
 * 
 * 主要功能：
 * - 从 Token 中提取用户名
 * - 生成新的 JWT Token
 * - 验证 Token 的有效性
 * - 检查 Token 是否过期
 * - 提取 Token 中的声明（Claims）
 * 
 * @author 系统架构
 * @version 1.0
 */
@Service
public class JwtService {

    /**
     * JWT 签名密钥
     * 
     * 用于签名和验证 JWT Token 的密钥（Base64 编码）。
     * 注意：生产环境中应该使用更安全的密钥管理方式，不应硬编码。
     */
    private static final String SECRET_KEY = "YWlyc3R5LW1vbml0b3Jpbmctc3lzdGVtLXNlY3JldC1rZXktZm9yLWp3dC1hdXRoZW50aWNhdGlvbg==";

    /**
     * Token 过期时间
     * 
     * JWT Token 的有效期限，默认为 24 小时（86400000 毫秒）。
     */
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; 

    /**
     * 从 Token 中提取用户名
     * 
     * 解析 JWT Token 并提取其中的 subject（通常是用户名）。
     * 
     * @param token JWT Token 字符串
     * @return Token 中包含的用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 生成 JWT Token
     * 
     * 根据用户详情生成一个新的 JWT Token，不包含额外声明。
     * 
     * @param userDetails Spring Security 的用户详情对象
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 生成 JWT Token（带额外声明）
     * 
     * 根据用户详情和额外的自定义声明生成 JWT Token。
     * Token 包含以下内容：
     * - 自定义声明（extraClaims）
     * - subject：用户名
     * - 签发时间：当前时间
     * - 过期时间：当前时间 + 24小时
     * 
     * @param extraClaims 额外的自定义声明
     * @param userDetails Spring Security 的用户详情对象
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证 Token 是否有效
     * 
     * 检查 JWT Token 是否有效，验证两个条件：
     * 1. Token 中的用户名与用户详情匹配
     * 2. Token 未过期
     * 
     * @param token JWT Token 字符串
     * @param userDetails Spring Security 的用户详情对象
     * @return Token 有效返回 true，否则返回 false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 验证 Token 签名
     * 
     * 检查 JWT Token 的签名是否有效，即使 Token 已过期也会验证签名。
     * 
     * @param token JWT Token 字符串
     * @return Token 签名有效返回 true，否则返回 false
     */
    public boolean validateTokenSignature(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     * 
     * 验证 JWT Token 的过期时间是否已过。
     * 
     * @param token JWT Token 字符串
     * @return Token 已过期返回 true，否则返回 false
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 提取 Token 的过期时间
     * 
     * 从 JWT Token 中解析过期时间。
     * 
     * @param token JWT Token 字符串
     * @return Token 的过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取 Token 中的声明
     * 
     * 通用方法，从 JWT Token 中提取指定的声明。
     * 
     * @param token JWT Token 字符串
     * @param claimsResolver 声明解析函数
     * @param <T> 声明的类型
     * @return 解析后的声明值
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 提取 Token 中的所有声明
     * 
     * 解析 JWT Token 并获取其中的所有声明（Claims）。
     * 
     * @param token JWT Token 字符串
     * @return Token 中的所有声明对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取签名密钥
     * 
     * 将 Base64 编码的密钥字符串解码为 SecretKey 对象。
     * 
     * @return HMAC-SHA 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
