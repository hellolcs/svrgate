package com.nicednb.svrgate.config;

import com.nicednb.svrgate.service.FirewallPolicyCollectionService;
import com.nicednb.svrgate.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 정책 수집 스케줄러 설정
 * 시스템 설정의 서버정책 수집주기에 따라 방화벽 정책을 수집합니다.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PolicyCollectionSchedulerConfig implements SchedulingConfigurer {

    private final Logger log = LoggerFactory.getLogger(PolicyCollectionSchedulerConfig.class);
    private final SystemSettingService systemSettingService;
    private final FirewallPolicyCollectionService policyCollectionService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("정책 수집 스케줄러 설정");
        
        // 단일 스레드 스케줄러 설정
        taskRegistrar.setScheduler(taskScheduler());
        
        // 정책 수집 작업 등록
        taskRegistrar.addTriggerTask(
                // 실행할 작업
                () -> {
                    try {
                        int currentCycle = systemSettingService.getIntegerValue(
                                SystemSettingService.KEY_SERVER_POLICY_CYCLE, 300);
                        log.info("서버 정책 수집 실행 (다음 실행 주기: {}초)", currentCycle);
                        policyCollectionService.collectPoliciesFromAllServers();
                    } catch (Exception e) {
                        log.error("서버 정책 수집 중 오류 발생: {}", e.getMessage(), e);
                    }
                },
                // 트리거 (실행 주기) - 매번 현재 설정값으로 다음 실행 시간 계산
                triggerContext -> {
                    // 시스템 설정에서 최신 주기 값 직접 조회 (캐시 무시)
                    systemSettingService.refreshSettingCache();
                    int currentCycle = systemSettingService.getIntegerValue(
                            SystemSettingService.KEY_SERVER_POLICY_CYCLE, 300);
                    
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
    
    /**
     * 스케줄러용 스레드 풀 구성
     */
    private Executor taskScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}