package com.conk.batch.common.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 내부 서비스 호출용 Feign 공통 설정이다.
 */
@Configuration
public class FeignConfig {

    private static final String HEADER_INTERNAL_CALL = "X-Internal-Call";

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public RequestInterceptor internalCallRequestInterceptor() {
        return template -> template.header(HEADER_INTERNAL_CALL, "true");
    }
}
