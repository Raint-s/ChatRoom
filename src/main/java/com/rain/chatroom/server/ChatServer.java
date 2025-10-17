package com.rain.chatroom.server;

import com.rain.chatroom.server.config.ThreadPoolConfig;
import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;
import com.rain.chatroom.server.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ChatServer {
    private final int port;
    private final SessionManager sessionManager;
    private final BroadcastService broadcastService;
    private final ThreadPoolExecutor threadPool;
    private volatile boolean running = false;

    public ChatServer(int port) {
        this.port = port;
        this.sessionManager = new SessionManager();
        this.broadcastService = new BroadcastService(sessionManager);
        this.threadPool = ThreadPoolConfig.createChatThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            running = true;
            log.info("聊天服务器启动在端口: {}", port);

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running && !Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                log.info("客户端连接: {}", clientSocket.getInetAddress());

                // 提交客户端处理任务到线程池
                threadPool.execute(() -> handleClient(clientSocket));
            }

        } catch (Exception e) {
            log.error("服务器异常: {}", e.getMessage());
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

            // 通知其他用户 - 使用BroadcastService
            broadcastService.sendSystemMessage(session.getUsername() + " 加入了聊天室");
            session.sendMessage("欢迎 " + session.getUsername() + "! 输入 'bye' 退出");

            // 消息处理循环
            processClientMessages(session);

        } catch (Exception e) {
            log.error("处理客户端异常: {}", e.getMessage());
        } finally {
            // 清理资源
            cleanupSession(session);
        }
    }

    private boolean authenticate(ClientSession session) throws IOException {
        session.sendMessage("请输入你的昵称:");
        String username = session.readMessage();

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

            // 广播用户消息 - 使用BroadcastService
            String formattedMessage = "[" + session.getUsername() + "]: " + message;
            broadcastService.broadcastToAll(formattedMessage, session);
        }
    }

    private void cleanupSession(ClientSession session) {
        if (session.getUsername() != null) {
            // 使用BroadcastService发送系统消息
            broadcastService.sendSystemMessage(session.getUsername() + " 离开了聊天室");
        }
        sessionManager.removeSession(session.getClientId());
        session.close();
    }

    private void shutdown() {
        running = false;
        threadPool.shutdown();
        log.info("服务器已关闭");
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(8888);
        server.start();
    }
}