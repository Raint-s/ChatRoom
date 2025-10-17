package com.rain.chatroom.server;

import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private final int port;
    private final SessionManager sessionManager;
    private final ExecutorService threadPool;
    private volatile boolean running = false;

    public ChatServer(int port) {
        this.port = port;
        this.sessionManager = new SessionManager();
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            running = true;
            System.out.println("聊天服务器启动在端口: " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接: " + clientSocket.getInetAddress());

                threadPool.execute(() -> handleClient(clientSocket));
            }

        } catch (Exception e) {
            System.err.println("服务器异常: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientSession session = new ClientSession(clientSocket);

        try {
            // 注册会话
            sessionManager.registerSession(session);

            // 认证流程
            if (!authenticate(session)) {
                return;
            }

            // 通知其他用户
            sessionManager.broadcastToAll("[系统] " + session.getUsername() + " 加入了聊天室");
            session.sendMessage("欢迎 " + session.getUsername() + "! 输入 'bye' 退出");

            // 消息处理循环
            processClientMessages(session);

        } catch (Exception e) {
            System.err.println("处理客户端异常: " + e.getMessage());
        } finally {
            // 清理资源
            cleanupSession(session);
        }
    }

    private boolean authenticate(ClientSession session) throws IOException {
        session.sendMessage("请输入你的昵称:");
        String username = null;
        try {
            username = session.readMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (username == null || username.trim().isEmpty()) {
            session.sendMessage("昵称不能为空");
            return false;
        }

        session.setUsername(username.trim());
        return true;
    }

    private void processClientMessages(ClientSession session) throws IOException {
        String message;
        while ((message = session.readMessage()) != null && session.isActive()) {
            if ("bye".equalsIgnoreCase(message)) {
                session.sendMessage("再见!");
                break;
            }

            // 广播用户消息
            String formattedMessage = "[" + session.getUsername() + "]: " + message;
            System.out.println("广播消息: " + formattedMessage);
            sessionManager.broadcastToAll(formattedMessage, session);
        }
    }

    private void cleanupSession(ClientSession session) {
        if (session.getUsername() != null) {
            sessionManager.broadcastToAll("[系统] " + session.getUsername() + " 离开了聊天室");
        }
        sessionManager.removeSession(session.getClientId());
        session.close();
    }

    private void shutdown() {
        running = false;
        threadPool.shutdown();
        System.out.println("服务器已关闭");
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(8888);
        server.start();
    }
}