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
    //广播的本质还是靠sessionManageer去遍历所有的clientSession，这样才能给所有客户端发消息
    private final SessionManager sessionManager;

    public void broadcastToAll(String message) {
        broadcastToAll(message, null);
    }

    //遍历所有在线的session，除了自己都发消息
    public void broadcastToAll(String message, ClientSession excludeSession) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();

        int sentCount = 0;
        for (ClientSession session : sessions) {
            if (session != excludeSession && session.isActive()) {
                session.sendMessage(message);
                sentCount++;
            }
        }

        log.debug("广播消息: {}, 接收者: {}", message, sentCount);
    }

    //私聊：发消息给指定用户
    public void sendToUser(String username, String message) {
        //流遍历，过滤实现
        sessionManager.getAllSessions().stream()
                .filter(session -> username.equals(session.getUsername()) && session.isActive())
                .forEach(session -> session.sendMessage(message));
    }

    // 私聊功能重置
    public boolean sendPrivateMessage(String fromUser, String toUser, String message) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();
        ClientSession targetSession = null;

        // 查找目标用户
        for (ClientSession session : sessions) {
            if (toUser.equals(session.getUsername()) && session.isActive()) {
                targetSession = session;
                break;
            }
        }

        if (targetSession == null) {
            return false; // 用户不在线
        }

        String privateMessage = "[私聊][" + fromUser + "->你]: " + message;
        targetSession.sendMessage(privateMessage);

        // 也给发送者一个确认
        for (ClientSession session : sessions) {
            if (fromUser.equals(session.getUsername()) && session.isActive()) {
                session.sendMessage("[私聊][你->" + toUser + "]: " + message);
                break;
            }
        }

        log.info("私聊消息: {} -> {}: {}", fromUser, toUser, message);
        return true;
    }

    public void sendSystemMessage(String message) {
        String formattedMessage = "[系统] " + message;
        broadcastToAll(formattedMessage);
        log.info("系统消息: {}", message);
    }

    // 在BroadcastService.java中添加
    public void handleCommand(ClientSession session, String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        String param = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "/users":
                listOnlineUsers(session);
                break;
            case "/stats":
                showStats(session);
                break;
            // 添加/msg、/whisper case，处理私聊
            case "/msg":
            case "/whisper":
                handlePrivateMessageCommand(session, param);
                break;
            case "/help":
                showHelp(session);
                break;
            default:
                session.sendMessage("[系统] 未知命令: " + cmd + "，输入 /help 查看帮助");
        }
    }

    private void listOnlineUsers(ClientSession session) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();
        StringBuilder userList = new StringBuilder("[系统] 在线用户 (" + sessions.size() + "): ");

        for (ClientSession s : sessions) {
            userList.append(s.getUsername()).append(", ");
        }

        if (sessions.size() > 0) {
            userList.setLength(userList.length() - 2); // 移除最后的逗号和空格
        }

        session.sendMessage(userList.toString());
    }

    //在线用户数：通过session.size()获取
    private void showStats(ClientSession session) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();
        session.sendMessage("[系统] 服务器状态 - 在线用户: " + sessions.size());
    }

    private void handlePrivateMessageCommand(ClientSession session, String param) {
        if (param.isEmpty()) {
            session.sendMessage("[系统] 用法: /msg 用户名 消息内容");
            return;
        }

        String[] msgParts = param.split(" ", 2);
        if (msgParts.length < 2) {
            session.sendMessage("[系统] 用法: /msg 用户名 消息内容");
            return;
        }

        String targetUser = msgParts[0];
        String privateMsg = msgParts[1];
        if (sendPrivateMessage(session.getUsername(), targetUser, privateMsg)) {
            log.info("{} 发送私聊给 {}: {}", session.getUsername(), targetUser, privateMsg);
        } else {
            session.sendMessage("[系统] 用户 " + targetUser + " 不在线或不存在");
        }
    }

    private void showHelp(ClientSession session) {
        String help = "[系统] 可用命令:\n" +
                "/users          - 查看在线用户\n" +
                "/stats          - 查看服务器状态\n" +
                "/msg 用户 消息   - 发送私聊消息\n" +
                "/whisper 用户 消息 - 发送私聊消息\n" +
                "/help           - 显示此帮助信息";
        session.sendMessage(help);
    }

}