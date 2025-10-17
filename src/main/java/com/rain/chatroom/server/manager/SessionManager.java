package com.rain.chatroom.server.manager;

import com.rain.chatroom.common.model.ClientInfo;
import com.rain.chatroom.server.handler.ClientSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 会话管理器 - 负责客户端会话的注册、查找、移除
 */
//SessionManager：负责管理客户端的会话（注册、移除、查找等）
@Slf4j
public class SessionManager {
    private final ConcurrentMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(ClientSession session) {
        sessions.put(session.getClientId(), session);
//        log.info("用户注册: {}, 当前在线: {}", session.getUsername(), sessions.size());
    }

    public void removeSession(String clientId) {
        ClientSession session = sessions.remove(clientId);
        if (session != null) {
//            log.info("用户移除: {}, 剩余在线: {}", session.getUsername(), sessions.size());
        }
    }

    public ClientSession getSession(String clientId) {
        return sessions.get(clientId);
    }

    public Collection<ClientSession> getAllSessions() {
        return sessions.values();
    }

    public int getOnlineCount() {
        return sessions.size();
    }

    public boolean isUserOnline(String username) {
        return sessions.values().stream()
                .anyMatch(session -> username.equals(session.getUsername()));
    }
}