package com.nicednb.svrgate.config;

import com.nicednb.svrgate.service.PolicyExpiryService;
import com.nicednb.svrgate.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 정책 만료 확인 스케줄러 설정
 */
@Component
@RequiredArgsConstructor
public class PolicyExpirySchedulerConfig implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(PolicyExpirySchedulerConfig.class);
    private final SystemSettingService systemSettingService;
    private final PolicyExpiryService policyExpiryService;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("정책 만료 확인 스케줄러 초기화");
        
        // 스케줄러 생성
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("policy-expiry-");
        scheduler.initialize();
        
        // 시스템 설정에서 정책 만료 확인 주기 (초) 가져오기
        int checkCycleSeconds = systemSettingService.getIntegerValue(
                SystemSettingService.KEY_POLICY_EXPIRY_CHECK_CYCLE, 3600);
        
        // 정책 만료 확인 작업 등록 (1시간마다)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("정책 만료 확인 실행");
                policyExpiryService.checkAndRemoveExpiredPolicies();
            } catch (Exception e) {
                log.error("정책 만료 확인 중 오류 발생: {}", e.getMessage(), e);
            }
        }, checkCycleSeconds * 1000); // 밀리초 단위로 변환
        
        log.info("정책 만료 확인 스케줄러 설정 완료: {}초 주기", checkCycleSeconds);
    }
}