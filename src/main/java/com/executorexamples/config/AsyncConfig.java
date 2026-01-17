package com.executorexamples.config;
/*
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. Core Pool Size: The minimum number of workers to keep alive.
        executor.setCorePoolSize(10);

        // 2. Max Pool Size: The maximum number of workers allowed.
        // Threads grow from core to max ONLY if the Queue is full.
        executor.setMaxPoolSize(50);

        // 3. Queue Capacity: The buffer for tasks before creating new threads (beyond core).
        executor.setQueueCapacity(100);

        // 4. Thread Name Prefix: Helps in reading logs (e.g. "Async-1", "Async-2")
        executor.setThreadNamePrefix("AsyncThread-");

        // 5. Rejection Policy: What to do if Queue is full AND Max pool is reached?
        // CallerRunsPolicy: The calling thread (Main/Http Thread) executes the task itself.
        // This slows down the input and prevents OutOfMemoryError.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}

*/
