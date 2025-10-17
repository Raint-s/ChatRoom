package com.rain.chatroom.server.service;

import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * 广播服务 - 负责消息的广播和定向发送
 */
//BroadcastService：负责消息的广播和定向发送，它依赖于SessionManager来获取所有会话。
@Slf4j
@RequiredArgsConstructor
public class BroadcastService {
    private final SessionManager sessionManager;

    public void broadcastToAll(String message) {
        broadcastToAll(message, null);
    }

    public void broadcastToAll(String message, ClientSession excludeSession) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();

        int sentCount = 0;
        for (ClientSession session : sessions) {
            if (session != excludeSession && session.isActive()) {
                session.sendMessage(message);
                sentCount++;
            }
        }

//        log.debug("广播消息: {}, 接收者: {}", message, sentCount);
    }

    public void sendToUser(String username, String message) {
        sessionManager.getAllSessions().stream()
                .filter(session -> username.equals(session.getUsername()) && session.isActive())
                .forEach(session -> session.sendMessage(message));
    }

    public void sendSystemMessage(String message) {
        String formattedMessage = "[系统] " + message;
        broadcastToAll(formattedMessage);
//        log.info("系统消息: {}", message);
    }
}