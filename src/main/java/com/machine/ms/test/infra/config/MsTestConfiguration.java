package com.machine.ms.test.infra.config;

import com.machine.ms.test.infra.observability.CorrelationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MsTestConfiguration implements WebMvcConfigurer {

    @Bean
    public CorrelationInterceptor correlationInterceptor() {
        return new CorrelationInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationInterceptor());
    }
}
