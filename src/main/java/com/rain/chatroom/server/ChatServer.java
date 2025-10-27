package com.rain.chatroom.server;

import com.rain.chatroom.server.config.ThreadPoolConfig;
import com.rain.chatroom.server.dao.MessageDao;
import com.rain.chatroom.server.dao.UserDao;
import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;
import com.rain.chatroom.server.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 处理客户端消息
 *
 * 设计思路：
 * 1. 使用线程池处理并发连接
 * 2. 每个会话独立线程，避免阻塞
 * 3. 消息广播使用CopyOnWriteArraySet保证线程安全
 *
 * 性能考虑：
 * - 读多写少场景使用CopyOnWriteArraySet
 * - 消息发送使用异步非阻塞方式
 * - 数据库操作使用连接池
 */
@Slf4j
public class ChatServer {
    private final int port;
    private final SessionManager sessionManager;
    private final BroadcastService broadcastService;
    private final ThreadPoolExecutor threadPool;
    private volatile boolean running = false;

    // 在ChatServer类中添加
    private final UserDao userDao = new UserDao();
    private final MessageDao messageDao = new MessageDao();

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

            // 启动监控
            startMonitor();

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

    // 在ChatServer.java中添加监控方法
    private void startMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(30000); // 每30秒输出一次状态
                    log.info("服务器状态 - 活跃线程: {}/{}, 队列大小: {}, 完成任务: {}",
                            threadPool.getActiveCount(),
                            threadPool.getPoolSize(),
                            threadPool.getQueue().size(),
                            threadPool.getCompletedTaskCount());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "server-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
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

//    private boolean authenticate(ClientSession session) throws IOException {
//        session.sendMessage("请输入你的昵称:");
//        String username = session.readMessage();
//
//        if (username == null || username.trim().isEmpty()) {
//            session.sendMessage("昵称不能为空");
//            return false;
//        }
//
//        session.setUsername(username.trim());
//        return true;
//    }

    // 修改认证流程
    private boolean authenticate(ClientSession session) throws IOException {
        session.sendMessage("请选择: 1. 登录 2. 注册");
        String choice = session.readMessage();

        if ("1".equals(choice)) {
            return login(session);
        } else if ("2".equals(choice)) {
            return register(session);
        } else {
            session.sendMessage("无效选择，连接关闭");
            return false;
        }
    }

    private boolean login(ClientSession session) throws IOException {
        session.sendMessage("请输入用户名:");
        String username = session.readMessage();
        session.sendMessage("请输入密码:");
        String password = session.readMessage();

        UserDao.User user = userDao.findUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) { // 实际应该加密验证
            userDao.updateUserLoginTime(user.getId());
            session.setUser(user);
            session.setUsername(user.getNickname());
            session.sendMessage("登录成功！欢迎 " + user.getNickname());
            return true;
        } else {
            session.sendMessage("登录失败，用户名或密码错误");
            return false;
        }
    }

    private boolean register(ClientSession session) throws IOException {
        session.sendMessage("请输入用户名:");
        String username = session.readMessage();
        session.sendMessage("请输入密码:");
        String password = session.readMessage();
        session.sendMessage("请输入昵称:");
        String nickname = session.readMessage();
        session.sendMessage("请输入邮箱(可选):");
        String email = session.readMessage();

        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            session.sendMessage("用户名、密码和昵称不能为空");
            return false;
        }

        if (userDao.findUserByUsername(username) != null) {
            session.sendMessage("用户名已存在");
            return false;
        }

        if (userDao.createUser(username, password, nickname, email)) {
            session.sendMessage("注册成功！请登录");
            return login(session);
        } else {
            session.sendMessage("注册失败，请重试");
            return false;
        }
    }

    // 在ChatServer.java的processClientMessages方法中修改
//    private void processClientMessages(ClientSession session) throws IOException {
//        String message;
//        while ((message = session.readMessage()) != null && session.isActive()) {
//            if ("bye".equalsIgnoreCase(message)) {
//                session.sendMessage("再见!");
//                break;
//            }
//
//            // 检查是否是命令
//            if (message.startsWith("/")) {
//                broadcastService.handleCommand(session, message);
//            } else {
//                // 广播用户消息
//                String formattedMessage = "[" + session.getUsername() + "]: " + message;
//                System.out.println("广播消息: " + formattedMessage);
//                broadcastService.broadcastToAll(formattedMessage, session);
//            }
//        }
//    }

    // 修改消息处理，添加持久化
    private void processClientMessages(ClientSession session) throws IOException {
        String message;
        while ((message = session.readMessage()) != null && session.isActive()) {
            if ("bye".equalsIgnoreCase(message)) {
                session.sendMessage("再见!");
                break;
            }

            // 检查是否是命令
            if (message.startsWith("/")) {
                handleCommand(session, message);
            } else {
                // 广播用户消息并保存到数据库
                String formattedMessage = "[" + session.getUsername() + "]: " + message;
                System.out.println("广播消息: " + formattedMessage);

                // 保存到数据库 (群聊消息，group_id为null)
                UserDao.User user = session.getUser();
                if (user != null) {
                    messageDao.saveMessage(2, user.getId(), null, null, message, 1, null);
                }

                broadcastService.broadcastToAll(formattedMessage, session);
            }
        }
    }

    // 在 ChatServer 类中添加如下方法
    private void handleCommand(ClientSession session, String command) {
        broadcastService.handleCommand(session, command);
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