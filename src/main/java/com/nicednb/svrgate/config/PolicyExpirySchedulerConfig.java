package com.nicednb.svrgate.config;

import com.nicednb.svrgate.service.PolicyExpiryService;
import com.nicednb.svrgate.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

/**
 * 정책 만료 확인 스케줄러 설정
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PolicyExpirySchedulerConfig implements SchedulingConfigurer {

    private final Logger log = LoggerFactory.getLogger(PolicyExpirySchedulerConfig.class);
    private final SystemSettingService systemSettingService;
    private final PolicyExpiryService policyExpiryService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 시스템 설정에서 정책 만료 확인 주기 (초) 가져오기
        int checkCycleSeconds = systemSettingService.getIntegerValue(
                SystemSettingService.KEY_POLICY_EXPIRY_CHECK_CYCLE, 3600);
        
        log.info("정책 만료 확인 스케줄러 설정: {}초 주기", checkCycleSeconds);
        
        // 주기적 트리거 생성
        PeriodicTrigger trigger = new PeriodicTrigger(checkCycleSeconds, TimeUnit.SECONDS);
        trigger.setInitialDelay(60); // 초기 지연 60초
        
        // 스케줄 등록
        taskRegistrar.addTriggerTask(
                // 실행할 작업
                () -> {
                    try {
                        policyExpiryService.checkAndRemoveExpiredPolicies();
                    } catch (Exception e) {
                        log.error("정책 만료 확인 중 오류 발생: {}", e.getMessage(), e);
                    }
                },
                // 트리거 (실행 주기)
                triggerContext -> {
                    // 최신 설정값 다시 조회 (동적 변경 가능)
                    int currentCycle = systemSettingService.getIntegerValue(
                            SystemSettingService.KEY_POLICY_EXPIRY_CHECK_CYCLE, 3600);
                    
                    // 트리거 주기 갱신
                    trigger.setPeriod(currentCycle);
                    
                    return trigger.nextExecutionTime(triggerContext);
                }
        );
    }
}