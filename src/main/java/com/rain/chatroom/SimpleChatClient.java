package com.rain.chatroom;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * 简单的聊天客户端 - 用于测试服务器
 */
public class SimpleChatClient {
    private String host;
    private int port;

    public SimpleChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    //实现了发送和接收同时进行，不会因为其中一个操作而阻塞另一个。
    public void start() {
        //try这里创建了要使用道德Socket以及input output等资源。
        //在try语句结束时，会自动调用实现了AutoCloseable或Closeable接口的对象的close()方法
        //不需要手动编写finally块来关闭资源
        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("连接到聊天服务器成功!");

            //程序启动后，后台线程开始监听服务端消息
            //服务端回复消息 → 后台线程接收并显示 → 继续监听下一个消息
            //匿名函数重写了Thread的run方法，start调用
            Thread readThread = new Thread(() -> {
                //独立线程：通过创建新的 Thread 线程，读取操作在后台持续运行
                //阻塞读取：in.readLine() 会一直等待服务端发送数据，不会立即返回
                //并发执行：主线程处理用户输入，后台线程处理服务端消息接收
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println("服务器: " + serverResponse);
                    }
                } catch (IOException e) {
                    System.out.println("与服务器断开连接");
                }
            });
            readThread.start();

            String userInput;
            //这里循环读取用户输入，然后把输入给out，out资源在try进来时定义了，对应socket的发送流，直到发bye
            //主线程等待用户输入
            //用户输入消息 → 主线程发送给服务端 → 继续等待下一个输入
            while (true) {
                userInput = scanner.nextLine();
                out.println(userInput);

                if ("bye".equalsIgnoreCase(userInput)) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("连接服务器失败: " + e.getMessage());
        }
    }

    // 这是main方法 - 程序的入口点
    public static void main(String[] args) {
        SimpleChatClient client = new SimpleChatClient("localhost", 8888);
        client.start();
    }
}