package com.nicednb.svrgate.service.log;

import com.nicednb.svrgate.entity.OperationHistory;
import com.nicednb.svrgate.repository.OperationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationHistoryRepository operationHistoryRepository;

    @Transactional
    public void logOperation(String username, String ipAddress, boolean success, String failReason, String logType, String description) {
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
    }
}
