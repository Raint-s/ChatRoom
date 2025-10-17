// ThreadPoolConfig.java
package com.rain.chatroom.server.config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolConfig {

    public static ThreadPoolExecutor createChatThreadPool() {
        return new ThreadPoolExecutor(
                5, 50, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ChatThreadFactory(),
                new ChatRejectionHandler()
        );
    }

    private static class ChatThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "chat-worker-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private static class ChatRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("任务被拒绝，当前活跃线程: " + executor.getActiveCount());
            if (!executor.isShutdown()) {
                r.run(); // 降级策略：由调用线程执行
            }
        }
    }
}