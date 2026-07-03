package com.example.smsgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated executor for async SMS dispatch from {@code POST /sms/send}.
 *
 * <p>Provider send (with {@code RetryHelper} backoff) can block for up to ~15s per
 * attempt and ~30s with a fallback. Running that on Tomcat's worker pool would
 * starve OTP endpoints and the Fineract webhook that share the same servlet
 * container. A bounded dedicated pool isolates that latency, and a
 * {@link ThreadPoolExecutor.CallerRunsPolicy} applies backpressure by running the
 * send on the submitting thread once the queue is full rather than dropping it.
 */
@Configuration
public class SmsGatewayAsyncConfig {

    @Bean(name = "smsSendExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor smsSendExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sms-send-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
