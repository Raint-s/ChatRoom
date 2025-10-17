package com.rain.chatroom.server.service;

import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;

import java.util.Collection;

/**
 * 广播服务 - 负责消息的广播和定向发送
 */
//BroadcastService：负责消息的广播和定向发送，它依赖于SessionManager来获取所有会话。
public class BroadcastService {
    private final SessionManager sessionManager;

    public BroadcastService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void broadcastToAll(String message) {
        broadcastToAll(message, null);
    }

    public void broadcastToAll(String message, ClientSession excludeSession) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();

        for (ClientSession session : sessions) {
            if (session != excludeSession && session.isActive()) {
                session.sendMessage(message);
            }
        }

        System.out.println("广播消息: " + message + ", 接收者: " + sessions.size());
    }

    public void sendToUser(String username, String message) {
        sessionManager.getAllSessions().stream()
                .filter(session -> username.equals(session.getUsername()) && session.isActive())
                .forEach(session -> session.sendMessage(message));
    }

    public void sendSystemMessage(String message) {
        String formattedMessage = "[系统] " + message;
        broadcastToAll(formattedMessage);
        System.out.println("系统消息: " + message);
    }
}