package com.rain.chatroom.common.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.net.Socket;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 客户端信息模型，存储连接信息
// 一个客户端对应一个ClientSession，其中如果要知道username,clientId，要靠clientInfo结构维护，clientSession是一个基本单位，clientInfo补充了信息
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