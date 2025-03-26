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

import java.time.Instant;

/**
 * 정책 만료 확인 스케줄러 설정
 * 동적으로 주기를 변경할 수 있도록 개선
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
        log.info("정책 만료 확인 스케줄러 설정");
        
        // 스케줄 등록
        taskRegistrar.addTriggerTask(
                // 실행할 작업
                () -> {
                    try {
                        int currentCycle = systemSettingService.getIntegerValue(
                                SystemSettingService.KEY_POLICY_EXPIRY_CHECK_CYCLE, 3600);
                        log.info("정책 만료 확인 실행 (다음 실행 주기: {}초)", currentCycle);
                        policyExpiryService.checkAndRemoveExpiredPolicies();
                    } catch (Exception e) {
                        log.error("정책 만료 확인 중 오류 발생: {}", e.getMessage(), e);
                    }
                },
                // 트리거 (실행 주기) - 매번 현재 설정값으로 다음 실행 시간 계산
                triggerContext -> {
                    // 시스템 설정에서 최신 주기 값 직접 조회 (캐시 무시)
                    systemSettingService.refreshSettingCache();
                    int currentCycle = systemSettingService.getIntegerValue(
                            SystemSettingService.KEY_POLICY_EXPIRY_CHECK_CYCLE, 3600);
                    
                    // 마지막 실행 시간 기준으로 다음 실행 시간 계산
                    Instant lastExecution = triggerContext.lastActualExecution();
                    
                    if (lastExecution != null) {
                        return lastExecution.plusSeconds(currentCycle);
                    } else {
                        // 첫 실행은 60초 후
                        return Instant.now().plusSeconds(60);
                    }
                }
        );
    }
}