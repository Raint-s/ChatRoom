package com.rain.chatroom.server.config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolConfig {

    public static ThreadPoolExecutor createChatThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 4;

        return new ThreadPoolExecutor(
                corePoolSize,              // 核心线程数 = CPU核心数
                maxPoolSize,               // 最大线程数 = CPU核心数 * 4
                60L,                       // 空闲线程存活时间
                TimeUnit.SECONDS,          // 时间单位
                new ArrayBlockingQueue<>(1000), // 有界队列，防止内存溢出
                new ChatThreadFactory(),   // 自定义线程工厂
                new ChatRejectionHandler() // 自定义拒绝策略
        );
    }

    private static class ChatThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "chat-worker-" + counter.getAndIncrement());
            thread.setDaemon(true); // 设置为守护线程，不会阻止JVM退出
            return thread;
        }
    }

    private static class ChatRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 记录警告日志，但不阻塞主线程
            System.err.println("警告: 线程池任务被拒绝，当前活跃线程: " +
                    executor.getActiveCount() + ", 队列大小: " + executor.getQueue().size());

            // 简单的降级策略：如果线程池没有关闭，由调用线程直接执行
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}