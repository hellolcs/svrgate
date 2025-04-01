package com.nicednb.svrgate.config;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 커넥션 매니저 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        // 요청 타임아웃 설정 
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(3, TimeUnit.SECONDS))
                .setResponseTimeout(Timeout.of(5, TimeUnit.SECONDS))
                .build();

        // HttpClient 생성 (최소한의 설정으로)
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 타임아웃 설정을 가진 HttpComponentsClientHttpRequestFactory 생성
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // RestTemplateBuilder를 사용하여 RestTemplate 생성
        return builder
                .requestFactory(() -> factory)
                .build();
    }
}