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
            
            // 정책 상세 정보 구성
            StringBuilder policyInfo = new StringBuilder();
            policyInfo.append("정책 ID: ").append(policy.getId());
            policyInfo.append(", 서버: ").append(serverName);
            policyInfo.append(", 우선순위: ").append(policy.getPriority());
            policyInfo.append(", 출발지 타입: ").append(policy.getSourceObjectType());
            policyInfo.append(", 출발지 ID: ").append(policy.getSourceObjectId());
            policyInfo.append(", 프로토콜: ").append(policy.getProtocol());
            
            // 포트 정보 추가
            if ("single".equals(policy.getPortMode())) {
                policyInfo.append(", 포트: ").append(policy.getStartPort());
            } else {
                policyInfo.append(", 포트 범위: ").append(policy.getStartPort())
                        .append("-").append(policy.getEndPort());
            }
            
            policyInfo.append(", 동작: ").append(policy.getAction());
            policyInfo.append(", 만료 시간: ").append(policy.getExpiresAt());
            
            // 정책 삭제
            policyRepository.delete(policy);
            
            // 작업 로그 기록 - 개선된 정책 정보 포함
            operationLogService.logOperation(
                    "SYSTEM", // 시스템에 의한 자동 삭제
                    "127.0.0.1", // 내부 작업이므로 로컬 IP
                    true,
                    policyInfo.toString(), // 개선된 상세 정책 정보
                    "정책관리",
                    "정책 자동 만료 삭제");
            
            log.info("만료된 정책 자동 삭제: {}", policyInfo);
        }
        
        log.info("만료된 정책 처리 완료: 총 {}개 삭제됨", expiredPolicies.size());
    }
}