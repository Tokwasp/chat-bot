package com.chatbot.backend.config.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class StreamExecutorConfig {

    private static final int CORE_POOL_SIZE = 8;
    private static final int MAX_POOL_SIZE = 50;
    private static final int QUEUE_CAPACITY = 0;
    private static final String THREAD_NAME_PREFIX = "chat-sse-";

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor streamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.initialize();

        return executor;
    }
}
