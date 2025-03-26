package com.nicednb.svrgate.service;

import com.nicednb.svrgate.entity.Policy;
import com.nicednb.svrgate.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정책 만료 처리를 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PolicyExpiryService {

    private final Logger log = LoggerFactory.getLogger(PolicyExpiryService.class);
    private final PolicyRepository policyRepository;
    private final OperationLogService operationLogService;

    /**
     * 만료된 정책 확인 및 삭제
     */
    @Transactional
    public void checkAndRemoveExpiredPolicies() {
        log.info("만료된 정책 확인 시작");
        LocalDateTime now = LocalDateTime.now();
        
        List<Policy> expiredPolicies = policyRepository.findExpiredPolicies(now);
        if (expiredPolicies.isEmpty()) {
            log.info("만료된 정책이 없습니다.");
            return;
        }
        
        log.info("만료된 정책 발견: {} 개", expiredPolicies.size());
        
        for (Policy policy : expiredPolicies) {
            String serverName = policy.getServerObject() != null ? policy.getServerObject().getName() : "Unknown";
            
            // 정책 삭제
            policyRepository.delete(policy);
            
            // 작업 로그 기록
            operationLogService.logOperation(
                    "SYSTEM", // 시스템에 의한 자동 삭제
                    "127.0.0.1", // 내부 작업이므로 로컬 IP
                    true,
                    "정책 ID: " + policy.getId() + ", 서버: " + serverName + ", 만료 시간: " + policy.getExpiresAt(),
                    "정책관리",
                    "정책 자동 만료 삭제");
            
            log.info("만료된 정책 자동 삭제: ID={}, 서버={}, 만료시간={}", 
                     policy.getId(), serverName, policy.getExpiresAt());
        }
        
        log.info("만료된 정책 처리 완료: 총 {}개 삭제됨", expiredPolicies.size());
    }
}