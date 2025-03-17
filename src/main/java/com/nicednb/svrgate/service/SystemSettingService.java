package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.SystemSettingDto;
import com.nicednb.svrgate.entity.SystemSetting;
import com.nicednb.svrgate.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final Logger log = LoggerFactory.getLogger(SystemSettingService.class);
    private final SystemSettingRepository systemSettingRepository;
    private final OperationLogService operationLogService;
    
    // 키 상수 정의
    public static final String KEY_MAX_IDLE_TIME = "max_idle_time";
    public static final String KEY_PASSWORD_CHANGE_CYCLE = "password_change_cycle";
    public static final String KEY_POLICY_CYCLE = "policy_cycle";
    public static final String KEY_CONCURRENT_SERVERS = "concurrent_servers";
    
    // 그룹 상수 정의
    public static final String GROUP_BASIC = "basic";
    public static final String GROUP_INTEGRATION = "integration";
    
    // 캐시 저장용 맵
    private final Map<String, String> settingsCache = new HashMap<>();
    
    // 설정 설명 매핑
    private final Map<String, String> settingDescriptions = new HashMap<>();
    
    /**
     * 애플리케이션 시작 시 설정값 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSettings() {
        log.info("시스템 설정 초기화 시작");
        
        // 설명 초기화
        initializeDescriptions();
        
        // 기본 설정이 없으면 생성
        createDefaultSettingIfNotExists(KEY_MAX_IDLE_TIME, "300", "최대 유휴시간(초)", GROUP_BASIC);
        createDefaultSettingIfNotExists(KEY_PASSWORD_CHANGE_CYCLE, "90", "패스워드 변경주기(일)", GROUP_BASIC);
        createDefaultSettingIfNotExists(KEY_POLICY_CYCLE, "300", "정책 수집주기(초)", GROUP_INTEGRATION);
        createDefaultSettingIfNotExists(KEY_CONCURRENT_SERVERS, "10", "동시 수집 서버 수(개)", GROUP_INTEGRATION);
        
        // 캐시에 모든 설정 로드
        refreshCache();
        
        log.info("시스템 설정 초기화 완료");
    }
    
    /**
     * 설정 설명 초기화
     */
    private void initializeDescriptions() {
        settingDescriptions.put(KEY_MAX_IDLE_TIME, "최대 유휴시간(초)");
        settingDescriptions.put(KEY_PASSWORD_CHANGE_CYCLE, "패스워드 변경주기(일)");
        settingDescriptions.put(KEY_POLICY_CYCLE, "정책 수집주기(초)");
        settingDescriptions.put(KEY_CONCURRENT_SERVERS, "동시 수집 서버 수(개)");
    }
    
    /**
     * 캐시 새로고침
     */
    private void refreshCache() {
        log.debug("시스템 설정 캐시 새로고침");
        settingsCache.clear();
        
        List<SystemSetting> settings = systemSettingRepository.findAll();
        for (SystemSetting setting : settings) {
            settingsCache.put(setting.getKey(), setting.getValue());
        }
    }
    
    /**
     * 설정값이 없는 경우 기본값 생성
     */
    @Transactional
    public void createDefaultSettingIfNotExists(String key, String defaultValue, String description, String group) {
        if (!systemSettingRepository.findByKey(key).isPresent()) {
            log.info("기본 시스템 설정 생성: {}={}", key, defaultValue);
            SystemSetting setting = SystemSetting.builder()
                    .key(key)
                    .value(defaultValue)
                    .description(description)
                    .group(group)
                    .build();
            systemSettingRepository.save(setting);
        }
    }
    
    /**
     * 현재 설정값을 DTO로 변환
     */
    @Transactional(readOnly = true)
    public SystemSettingDto getSettings() {
        SystemSettingDto dto = new SystemSettingDto();
        
        // 기본 설정
        dto.setMaxIdleTime(getIntegerValue(KEY_MAX_IDLE_TIME, 300));
        dto.setPasswordChangeCycle(getIntegerValue(KEY_PASSWORD_CHANGE_CYCLE, 90));
        
        // 연동 설정
        dto.setPolicyCycle(getIntegerValue(KEY_POLICY_CYCLE, 300));
        dto.setConcurrentServers(getIntegerValue(KEY_CONCURRENT_SERVERS, 10));
        
        return dto;
    }
    
    /**
     * 설정값 저장
     */
    @Transactional
    public void saveSettings(SystemSettingDto dto, String username, String ipAddress) {
        log.info("시스템 설정 저장 시작: username={}", username);
        
        // 변경된 설정 추적
        Map<String, String> changeLog = new HashMap<>();
        
        // 기본 설정 저장 및 변경 추적
        trackSettingChange(changeLog, KEY_MAX_IDLE_TIME, dto.getMaxIdleTime(), GROUP_BASIC);
        trackSettingChange(changeLog, KEY_PASSWORD_CHANGE_CYCLE, dto.getPasswordChangeCycle(), GROUP_BASIC);
        
        // 연동 설정 저장 및 변경 추적
        trackSettingChange(changeLog, KEY_POLICY_CYCLE, dto.getPolicyCycle(), GROUP_INTEGRATION);
        trackSettingChange(changeLog, KEY_CONCURRENT_SERVERS, dto.getConcurrentServers(), GROUP_INTEGRATION);
        
        // 캐시 새로고침
        refreshCache();
        
        // 변경 내용 로그 메시지 생성
        String logMessage = formatChangeLogMessage(changeLog);
        
        // 로그 기록
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                logMessage, // 변경된 설정 로그를 failReason 필드에 기록
                "시스템설정",
                "시스템 설정 변경"
        );
        
        log.info("시스템 설정 저장 완료: username={}, 변경사항={}", username, logMessage);
    }
    
    /**
     * 설정 변경 추적 및 저장
     */
    private void trackSettingChange(Map<String, String> changeLog, String key, Integer newValue, String group) {
        String oldValue = getStringValue(key, null);
        String newValueStr = String.valueOf(newValue);
        
        // 값이 변경된 경우에만 로그에 추가
        if (oldValue == null || !oldValue.equals(newValueStr)) {
            changeLog.put(key, oldValue + " → " + newValueStr);
        }
        
        // 설정 저장
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElse(SystemSetting.builder()
                        .key(key)
                        .group(group)
                        .build());
        
        setting.setValue(newValueStr);
        systemSettingRepository.save(setting);
    }
    
    /**
     * 변경 로그 메시지 포맷
     */
    private String formatChangeLogMessage(Map<String, String> changeLog) {
        if (changeLog.isEmpty()) {
            return "변경된 설정 없음";
        }
        
        return changeLog.entrySet().stream()
                .map(entry -> {
                    String description = settingDescriptions.getOrDefault(entry.getKey(), entry.getKey());
                    return description + ": " + entry.getValue();
                })
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 문자열 설정값 조회
     */
    public String getStringValue(String key, String defaultValue) {
        // 캐시에서 조회
        if (settingsCache.containsKey(key)) {
            return settingsCache.get(key);
        }
        
        // DB에서 조회
        Optional<SystemSetting> setting = systemSettingRepository.findByKey(key);
        return setting.map(SystemSetting::getValue).orElse(defaultValue);
    }
    
    /**
     * 정수형 설정값 조회
     */
    public Integer getIntegerValue(String key, Integer defaultValue) {
        String value = getStringValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("설정값 {}이 정수형이 아닙니다: {}", key, value);
            return defaultValue;
        }
    }
    
    /**
     * 최대 유휴시간 조회 (초 단위)
     */
    public int getMaxIdleTime() {
        return getIntegerValue(KEY_MAX_IDLE_TIME, 300);
    }
    
    /**
     * 패스워드 변경주기 조회 (일 단위)
     */
    public int getPasswordChangeCycle() {
        return getIntegerValue(KEY_PASSWORD_CHANGE_CYCLE, 90);
    }
}