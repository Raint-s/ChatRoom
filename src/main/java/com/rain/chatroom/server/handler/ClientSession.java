package com.rain.chatroom.server.handler;

import com.rain.chatroom.common.model.ClientInfo;
import com.rain.chatroom.server.dao.UserDao;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * 客户端会话 - 封装单个客户端的连接状态和IO操作
 */
@Slf4j
public class ClientSession {
    @Getter
    private final String clientId;
    @Getter
    private final ClientInfo clientInfo;
    @Getter
    private final Socket socket;

    //这边关联用户信息
    private UserDao.User user;

    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean active = true;

    // 在ClientSession中添加用户信息
    public void setUser(UserDao.User user) {
        this.user = user;
    }

    public UserDao.User getUser() {
        return user;
    }

    public ClientSession(Socket socket) {
        this.clientId = UUID.randomUUID().toString();
        this.socket = socket;
        this.clientInfo = new ClientInfo(clientId, socket);

        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            this.active = false;
            log.error("初始化客户端会话失败: {}", e.getMessage());
        }
    }

    public String readMessage() throws IOException {
        return reader.readLine();
    }

    public void sendMessage(String message) {
        if (active && writer != null) {
            writer.println(message);
        }
    }

    public String getUsername() {
        return clientInfo.getUsername();
    }

    public void setUsername(String username) {
        clientInfo.setUsername(username);
    }

    public boolean isActive() {
        return active;
    }

    public void close() {
        this.active = false;
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("关闭客户端连接失败: {}", e.getMessage());
        }
    }
}