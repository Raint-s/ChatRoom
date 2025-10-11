package com.rain.chatroom;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 多线程聊天服务器 - 可以处理多个客户端连接
 */
public class MultiThreadChatServer {
    private ServerSocket serverSocket;
    // 使用线程安全的Set来存储所有客户端输出流
    //这里为什么可以存下所有客户端socket连接的信息？因为printWriter是根据传入客户端socket取出的outputStream获得的
    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());

    public MultiThreadChatServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("多线程聊天服务器启动在端口: " + port);
        } catch (IOException e) {
            System.out.println("启动服务器失败: " + e.getMessage());
        }
    }

    public void start() {
        // 使用线程池来管理客户端连接
        ExecutorService pool = Executors.newFixedThreadPool(10);

        while (true) {
            try {
                System.out.println("等待客户端连接...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功: " + clientSocket.getInetAddress());

                // 为每个客户端创建一个新的Handler线程
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);

            } catch (IOException e) {
                System.out.println("处理客户端连接失败: " + e.getMessage());
            }
        }
    }

    /**
     * 客户端处理线程 - 每个客户端连接都会创建这个线程
     */
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // 将当前客户端的输出流添加到全局集合中
                clientWriters.add(out);

                // 获取客户端名称，阻塞等待客户端输入
                out.println("请输入你的昵称:");
                clientName = in.readLine();
                System.out.println(clientName + " 加入了聊天室");

                // 广播欢迎消息：内部需要广播给所有客户端，外部每一个客户端都需要打印出来
                broadcastMessage(clientName + " 加入了聊天室");
                out.println("欢迎 " + clientName + "! 输入 'bye' 退出聊天室");

                String inputLine;
                // 循环读取客户端消息
                while ((inputLine = in.readLine()) != null) {
                    if ("bye".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                    // 广播消息给所有客户端
                    broadcastMessage(clientName + ": " + inputLine);
                }

            } catch (IOException e) {
                System.out.println("处理客户端消息失败: " + e.getMessage());
            } finally {
                // 客户端断开连接，清理资源
                try {
                    if (out != null) {
                        clientWriters.remove(out);
                    }
                    if (clientName != null) {
                        System.out.println(clientName + " 离开了聊天室");
                        broadcastMessage(clientName + " 离开了聊天室");
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 广播消息给所有连接的客户端
         */
        private void broadcastMessage(String message) {
            System.out.println("广播消息: " + message);
            // 同步块确保线程安全
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        MultiThreadChatServer server = new MultiThreadChatServer(8888);
        server.start();
    }
}