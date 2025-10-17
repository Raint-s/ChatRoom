package com.rain.chatroom.common.model;

import java.net.Socket;
import java.time.LocalDateTime;

public class ClientInfo {
    private String clientId;
    private String username;
    private Socket socket;
    private long connectTime;
    private String remoteAddress;

    // 默认构造器
    public ClientInfo() {
        this.connectTime = System.currentTimeMillis();
    }

    // 带参数的构造器
    public ClientInfo(String clientId, Socket socket) {
        this();
        this.clientId = clientId;
        this.socket = socket;
        this.remoteAddress = socket != null ?
                socket.getInetAddress().getHostAddress() + ":" + socket.getPort() : "unknown";
    }

    // Getter和Setter
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Socket getSocket() { return socket; }
    public void setSocket(Socket socket) { this.socket = socket; }

    public long getConnectTime() { return connectTime; }
    public void setConnectTime(long connectTime) { this.connectTime = connectTime; }

    public String getRemoteAddress() { return remoteAddress; }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "clientId='" + clientId + '\'' +
                ", username='" + username + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", connectTime=" + connectTime +
                '}';
    }
}