package com.nicednb.svrgate.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 타임아웃 설정 증가
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());  // 연결 타임아웃: 10초
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());     // 읽기 타임아웃: 30초
        
        // 오류 처리 개선된 RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate(factory);
        
        return restTemplate;
    }
}