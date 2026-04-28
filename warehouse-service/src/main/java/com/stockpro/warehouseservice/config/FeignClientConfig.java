package com.stockpro.warehouseservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            String authorizationHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
        };
    }
}
