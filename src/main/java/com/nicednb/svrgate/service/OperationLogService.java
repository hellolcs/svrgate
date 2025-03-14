package com.nicednb.svrgate.service;

import com.nicednb.svrgate.entity.OperationHistory;
import com.nicednb.svrgate.repository.OperationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationHistoryRepository operationHistoryRepository;
    private final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    /**
     * 사용자 작업을 기록합니다.
     * 
     * @param username 작업을 수행한 사용자
     * @param ipAddress 사용자 IP 주소
     * @param success 작업 성공 여부
     * @param failReason 실패 이유 (실패 시에만 사용, 성공 시 null)
     * @param logType 로그 유형 (예: "로그인", "계정관리", "정책관리" 등)
     * @param description 작업 설명
     */
    @Transactional
    public void logOperation(String username, String ipAddress, boolean success, String failReason, String logType, String description) {
        try {
            OperationHistory history = OperationHistory.builder()
                    .username(username)
                    .ipAddress(ipAddress)
                    .operationTime(LocalDateTime.now())
                    .success(success)
                    .failReason(failReason)
                    .logType(logType)
                    .description(description)
                    .build();
            
            operationHistoryRepository.save(history);
            
            if (success) {
                log.debug("작업 로그 저장 성공: [{}] {}, user={}, ip={}", logType, description, username, ipAddress);
            } else {
                log.debug("실패 작업 로그 저장: [{}] {}, user={}, ip={}, 실패사유={}", 
                        logType, description, username, ipAddress, failReason);
            }
        } catch (Exception e) {
            log.error("작업 로그 저장 실패: user={}, type={}, 오류={}", username, logType, e.getMessage(), e);
        }
    }
}