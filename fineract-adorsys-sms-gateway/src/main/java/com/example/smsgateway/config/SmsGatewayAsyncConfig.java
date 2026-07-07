package com.example.smsgateway.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated executor for async SMS dispatch from {@code POST /sms/send}.
 *
 * <p>Provider send (with {@code RetryHelper} backoff) can block for up to ~15s per
 * attempt, longer with the full fallback cascade. Running that on Tomcat's worker
 * pool would starve OTP endpoints and the Fineract webhook that share the same
 * servlet container. A bounded dedicated pool isolates that latency.
 *
 * <p><b>Overload policy: {@link ThreadPoolExecutor.AbortPolicy}.</b> Once the pool
 * (8 workers) + queue (100) are saturated, {@code submit()} throws
 * {@link java.util.concurrent.RejectedExecutionException} on the submitting thread.
 * That is intentionally <i>not</i> {@code CallerRunsPolicy}: running the provider
 * send on the Tomcat request thread is exactly the ~15-30s blocking this pool
 * exists to prevent — it would silently serialize bursts and defeat the 202/async
 * contract. Instead the exception propagates to the controller and is mapped to
 * {@code 429 Too Many Requests} by {@code ApiExceptionHandler}, making overload
 * explicit to the BFF so it can back off rather than have its request blocked.
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
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.AbortPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
