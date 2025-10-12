package com.rain.chatroom;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优化版多线程聊天服务器 - 使用自定义线程池
 */
public class OptimizedChatServer {
    private ServerSocket serverSocket;
    private static final CopyOnWriteArraySet<ClientSession> clientSessions = new CopyOnWriteArraySet<>();

    // 自定义命名的线程工厂
    private static class ChatThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        ChatThreadFactory(String poolName) {
            namePrefix = poolName + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true); // 设置为守护线程
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    // 自定义拒绝策略
    private static class ChatRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println("任务被拒绝，当前活跃线程: " + executor.getActiveCount() +
                    ", 队列大小: " + executor.getQueue().size());
            // 可以在这里记录日志、发送告警等
            if (!executor.isShutdown()) {
                r.run(); // 由调用线程直接执行（降级策略）
            }
        }
    }

    // 自定义线程池
    private final ThreadPoolExecutor threadPool;

    public OptimizedChatServer(int port) {
        // 创建自定义线程池
        this.threadPool = new ThreadPoolExecutor(
                5,                          // 核心线程数
                50,                         // 最大线程数
                60L,                        // 空闲线程存活时间
                TimeUnit.SECONDS,           // 时间单位
                new ArrayBlockingQueue<>(1000), // 有界队列，防止内存溢出
                new ChatThreadFactory("chat-server"), // 自定义线程工厂
                new ChatRejectionHandler()           // 自定义拒绝策略
        );

        // 允许核心线程超时销毁
        threadPool.allowCoreThreadTimeOut(true);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("优化版聊天服务器启动在端口: " + port);
            System.out.println("线程池状态: 核心" + threadPool.getCorePoolSize() +
                    ", 最大" + threadPool.getMaximumPoolSize());
        } catch (IOException e) {
            System.out.println("启动服务器失败: " + e.getMessage());
        }
    }

    public void start() {
        // 添加关闭钩子，优雅关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接: " + clientSocket.getInetAddress() +
                        ", 活跃线程: " + threadPool.getActiveCount());

                // 提交任务到线程池
                threadPool.execute(new ClientSession(clientSocket));

            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.out.println("接受连接失败: " + e.getMessage());
                }
            }
        }
    }

    private void shutdown() {
        System.out.println("正在关闭服务器...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 客户端会话 - 封装客户端连接状态和行为
     */
    private class ClientSession implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private final long connectTime;

        public ClientSession(Socket socket) {
            this.clientSocket = socket;
            this.connectTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // 注册客户端
                clientSessions.add(this);

                // 身份验证流程
                if (!authenticate()) {
                    return;
                }

                System.out.println(clientName + " 认证成功，加入聊天室");
                broadcastSystemMessage(clientName + " 加入了聊天室");
                out.println("欢迎 " + clientName + "! 输入 'bye' 退出");

                // 消息处理循环
                processMessages();

            } catch (IOException e) {
                System.out.println("处理客户端消息失败: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private boolean authenticate() throws IOException {
            out.println("请输入你的昵称:");
            String name = in.readLine();
            if (name == null || name.trim().isEmpty()) {
                out.println("昵称不能为空，连接关闭");
                return false;
            }
            this.clientName = name.trim();
            return true;
        }

        private void processMessages() throws IOException {
            String message;
            while ((message = in.readLine()) != null) {
                if ("bye".equalsIgnoreCase(message)) {
                    out.println("再见!");
                    break;
                }
                // 处理特殊命令
                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    broadcastChatMessage(message);
                }
            }
        }

        private void handleCommand(String command) {
            if ("/users".equals(command)) {
                out.println("当前在线用户: " + getOnlineUsers());
            } else if ("/stats".equals(command)) {
                out.println("服务器状态: 活跃连接 " + clientSessions.size() +
                        ", 线程池活跃 " + threadPool.getActiveCount());
            } else {
                out.println("未知命令: " + command);
            }
        }

        private void broadcastChatMessage(String message) {
            String formattedMessage = "[" + clientName + "]: " + message;
            System.out.println("广播消息: " + formattedMessage);

            // 使用CopyOnWriteArraySet不需要显式同步
            for (ClientSession session : clientSessions) {
                if (session != this) { // 不发送给自己
                    session.sendMessage(formattedMessage);
                }
            }
        }

        private void broadcastSystemMessage(String message) {
            System.out.println("系统消息: " + message);
            for (ClientSession session : clientSessions) {
                session.sendMessage("[系统]: " + message);
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private String getOnlineUsers() {
            StringBuilder users = new StringBuilder();
            for (ClientSession session : clientSessions) {
                users.append(session.clientName).append(", ");
            }
            return users.length() > 0 ? users.substring(0, users.length() - 2) : "无";
        }

        private void cleanup() {
            try {
                if (out != null) {
                    clientSessions.remove(this);
                }
                if (clientName != null) {
                    System.out.println(clientName + " 离开聊天室，连接时长: " +
                            (System.currentTimeMillis() - connectTime) + "ms");
                    broadcastSystemMessage(clientName + " 离开了聊天室");
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("清理资源失败: " + e.getMessage());
            }
        }

        public String getClientName() {
            return clientName;
        }
    }

    public static void main(String[] args) {
        OptimizedChatServer server = new OptimizedChatServer(8888);
        server.start();
    }
}