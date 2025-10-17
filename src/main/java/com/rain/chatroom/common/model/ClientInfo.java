package com.rain.chatroom.common.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.net.Socket;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {
    private String clientId;
    private String username;
    private Socket socket;
    private long connectTime;
    private String remoteAddress;

    // 自定义构造器
    public ClientInfo(String clientId, Socket socket) {
        this.clientId = clientId;
        this.socket = socket;
        this.connectTime = System.currentTimeMillis();
        this.remoteAddress = socket != null ?
                socket.getInetAddress().getHostAddress() + ":" + socket.getPort() : "unknown";
    }
}