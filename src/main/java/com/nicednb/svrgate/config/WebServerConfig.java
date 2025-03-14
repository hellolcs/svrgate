package com.nicednb.svrgate.config;

import com.nicednb.svrgate.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebServerConfig {

    private final Logger log = LoggerFactory.getLogger(WebServerConfig.class);
    private final SystemSettingService systemSettingService;

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            // 시스템 설정의 최대 유휴시간을 초 단위로 설정
            int maxIdleTimeSeconds = systemSettingService.getMaxIdleTime();
            log.info("서블릿 세션 타임아웃 설정: {} 초", maxIdleTimeSeconds);
            
            // Session 객체를 생성하여 설정
            Session session = new Session();
            session.setTimeout(Duration.ofSeconds(maxIdleTimeSeconds));
            factory.setSession(session);
        };
    }
}