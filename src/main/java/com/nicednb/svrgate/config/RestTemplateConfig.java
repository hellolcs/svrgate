package com.nicednb.svrgate.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Connection timeout 설정 (10초)
        factory.setConnectTimeout(3_000);
        
        // Read timeout 설정 (30초)
        factory.setReadTimeout(3_000);
        
        // RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 유니코드(한글) 문자 처리를 위한 MessageConverter 설정
        // StringHttpMessageConverter를 UTF-8로 설정
        restTemplate.getMessageConverters().stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        // JSON 변환기 설정 (Java8 날짜/시간 모듈 추가)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // MappingJackson2HttpMessageConverter 설정
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        
        // 기존 JSON 변환기를 새 변환기로 교체
        restTemplate.getMessageConverters().replaceAll(converter -> 
            converter instanceof MappingJackson2HttpMessageConverter ? jsonConverter : converter);
        
        return restTemplate;
    }
}