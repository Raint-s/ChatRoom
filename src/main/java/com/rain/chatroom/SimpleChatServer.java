package com.rain.chatroom;
import java.io.*;
import java.net.*;

/**
 * 最简单的聊天服务器 - 只能处理一个客户端连接
 */
public class SimpleChatServer {
    private ServerSocket serverSocket;

    //构造方法实例化出具体的Server
    public SimpleChatServer(int port) {
        try {
            //这里不应该直接回车ServerSocket对象，因为定位的不是这个ServerSocket
            serverSocket = new ServerSocket(port);
            System.out.println("聊天服务器启动在端口: " + port);
        } catch (IOException e) {
            System.out.println("启动服务器失败: " + e.getMessage());
        }
    }

    public void start() {
        while (true) {
            try {
                System.out.println("等待客户端连接...");
                //阻塞，等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功: " + clientSocket.getInetAddress());

                //处理连接消息
                handleClient(clientSocket);

            } catch (IOException e) {
                System.out.println("处理客户端连接失败: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine;
            out.println("欢迎连接到简单聊天室! 输入 'bye' 退出");

            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到客户端消息: " + inputLine);

                if ("bye".equalsIgnoreCase(inputLine)) {
                    out.println("再见!");
                    break;
                }

                out.println("服务器回复: 我收到了你的消息 -> " + inputLine);
            }

            System.out.println("客户端断开连接");
            clientSocket.close();

        } catch (IOException e) {
            System.out.println("处理客户端消息失败: " + e.getMessage());
        }
    }

    // 这是main方法 - 程序的入口点
    public static void main(String[] args) {
        SimpleChatServer server = new SimpleChatServer(8888);
        server.start();
    }
}