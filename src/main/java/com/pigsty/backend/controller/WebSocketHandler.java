package com.pigsty.backend.controller;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.model.EnvironmentalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket 处理器
 * 
 * 该类处理 WebSocket 连接、断开连接以及消息推送。
 * 维护所有活跃的 WebSocket 会话，并提供静态方法用于推送消息。
 * 
 * @author 系统架构
 * @version 1.0
 */
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    // 维护所有活跃的 WebSocket 会话
    private static final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    
    // Jackson  ObjectMapper 用于 JSON 序列化
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 新连接建立时，将会话添加到集合中
        sessions.add(session);
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // 连接关闭时，从集合中移除会话
        sessions.remove(session);
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 处理传输错误
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        // 移除出错的会话
        sessions.remove(session);
        // 关闭会话
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 推送预警消息给所有连接的客户端
     * 
     * @param warningLog 预警日志对象
     */
    public static void sendWarning(WarningLog warningLog) {
        try {
            // 创建预警消息对象
            WarningMessage message = new WarningMessage();
            message.setType("warning");
            message.setData(warningLog);
            
            // 序列化为 JSON 字符串
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // 发送给所有活跃会话
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        log.error("Failed to send warning message to session {}: {}", session.getId(), e.getMessage());
                        // 移除出错的会话
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send warning message: {}", e.getMessage());
        }
    }

    /**
     * 推送环境数据更新给所有连接的客户端
     * 
     * @param data 环境数据对象
     */
    public static void sendDataUpdate(EnvironmentalData data) {
        try {
            // 创建数据更新消息对象
            DataMessage message = new DataMessage();
            message.setType("data-update");
            message.setData(data);
            
            // 序列化为 JSON 字符串
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // 发送给所有活跃会话
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        log.error("Failed to send data update message to session {}: {}", session.getId(), e.getMessage());
                        // 移除出错的会话
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send data update message: {}", e.getMessage());
        }
    }

    /**
     * 预警消息包装类
     */
    private static class WarningMessage {
        private String type;
        private WarningLog data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public WarningLog getData() {
            return data;
        }

        public void setData(WarningLog data) {
            this.data = data;
        }
    }

    /**
     * 数据更新消息包装类
     */
    private static class DataMessage {
        private String type;
        private EnvironmentalData data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public EnvironmentalData getData() {
            return data;
        }

        public void setData(EnvironmentalData data) {
            this.data = data;
        }
    }
}
