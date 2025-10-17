package com.rain.chatroom.server.handler;

import com.rain.chatroom.common.model.ClientInfo;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * 客户端会话 - 封装单个客户端的连接状态和IO操作
 */
public class ClientSession {
    private final String clientId;
    private final ClientInfo clientInfo;
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean active = true;

    public ClientSession(Socket socket) {
        this.clientId = UUID.randomUUID().toString();
        this.socket = socket;
        this.clientInfo = new ClientInfo(clientId, socket);

        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            this.active = false;
            System.err.println("初始化客户端会话失败: " + e.getMessage());
        }
    }

    //读消息
    public String readMessage() throws IOException {
        return reader.readLine();
    }

    //发送消息
    public void sendMessage(String message) {
        if (active && writer != null) {
            writer.println(message);
        }
    }

    //关闭会话需要把申请的资源都关闭
    public void close() {
        this.active = false;
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("关闭客户端连接失败: " + e.getMessage());
        }
    }

    // getter方法
    public String getClientId() { return clientId; }
    public String getUsername() { return clientInfo.getUsername(); }
    public void setUsername(String username) { clientInfo.setUsername(username); }
    public boolean isActive() { return active; }
    public ClientInfo getClientInfo() { return clientInfo; }
}