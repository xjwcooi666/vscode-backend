package com.pigsty.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.pigsty.backend.controller.WebSocketHandler;

/**
 * WebSocket 配置类
 * 
 * 该配置类启用 WebSocket 支持并注册 WebSocket 处理器。
 * 
 * @author 系统架构
 * @version 1.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器，路径为 /ws，允许所有跨域请求
        registry.addHandler(new WebSocketHandler(), "/ws").setAllowedOrigins("*");
    }
}
