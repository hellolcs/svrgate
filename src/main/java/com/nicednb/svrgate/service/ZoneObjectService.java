package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.ZoneObjectDto;
import com.nicednb.svrgate.entity.ZoneObject;
import com.nicednb.svrgate.repository.GeneralObjectRepository;
import com.nicednb.svrgate.repository.NetworkObjectRepository;
import com.nicednb.svrgate.repository.ServerObjectRepository;
import com.nicednb.svrgate.repository.ZoneObjectRepository;
import com.nicednb.svrgate.util.PageConversionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneObjectService {

    private final Logger log = LoggerFactory.getLogger(ZoneObjectService.class);
    private final ZoneObjectRepository zoneRepository;
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;
    private final ServerObjectRepository serverObjectRepository;
    private final OperationLogService operationLogService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 기존 메서드는 유지하고 변경이 필요한 메서드만 수정합니다...

    /**
     * Zone 엔티티를 ZoneDto로 변환 (lastSyncTime 추가)
     */
    public ZoneObjectDto convertToDto(ZoneObject zone) {
        ZoneObjectDto dto = new ZoneObjectDto();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        dto.setFirewallIp(zone.getFirewallIp());
        dto.setActive(zone.isActive());
        dto.setDescription(zone.getDescription());
        dto.setLastSyncTime(zone.getLastSyncTime());

        // 마지막 연동 시각 포맷팅 (추가)
        if (zone.getLastSyncTime() != null) {
            dto.setLastSyncTimeFormatted(zone.getLastSyncTime().format(DATE_TIME_FORMATTER));
        } else {
            dto.setLastSyncTimeFormatted("-");
        }

        // 비보안Zone ID 목록
        if (zone.getNonSecureZones() != null && !zone.getNonSecureZones().isEmpty()) {
            List<Long> nonSecureZoneIds = zone.getNonSecureZones().stream()
                    .map(ZoneObject::getId)
                    .collect(Collectors.toList());
            dto.setNonSecureZoneIds(nonSecureZoneIds);
        }

        // 보안Zone ID 목록
        if (zone.getSecureZones() != null && !zone.getSecureZones().isEmpty()) {
            List<Long> secureZoneIds = zone.getSecureZones().stream()
                    .map(ZoneObject::getId)
                    .collect(Collectors.toList());
            dto.setSecureZoneIds(secureZoneIds);
        }

        // 화면 표시용 필드
        dto.setNonSecureZoneNames(zone.getNonSecureZoneNames());
        dto.setSecureZoneNames(zone.getSecureZoneNames());

        return dto;
    }

    /**
     * ID로 Zone 조회
     */
    @Transactional(readOnly = true)
    public ZoneObject findById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone을 찾을 수 없습니다: " + id));
    }

    /**
     * 드롭다운 목록에 사용할 간소화된 Zone 목록 조회
     * 연동 여부에 관계없이 모든 Zone 반환
     */
    @Transactional(readOnly = true)
    public List<ZoneObjectDto> findAllZonesForDropdownAsDto() {
        List<ZoneObject> zones = zoneRepository.findAllByOrderByIdAsc();
        return zones.stream()
                .map(zone -> {
                    ZoneObjectDto dto = new ZoneObjectDto();
                    dto.setId(zone.getId());
                    dto.setName(zone.getName());
                    // 필요한 최소한의 정보만 설정
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 방화벽과 RestAPI 통신
     * 
     * @param id       Zone ID
     * @param clientIp 클라이언트 IP 주소
     * @return 연동된 Zone 객체 DTO
     */
    @Transactional
    public ZoneObjectDto syncWithFirewall(Long id, String clientIp) {
        log.info("방화벽 동기화 시작: zoneId={}", id);

        // Zone 존재 여부 확인
        ZoneObject zoneObject = findById(id);

        // 연동 여부 확인
        if (!zoneObject.isActive()) {
            throw new IllegalStateException("연동이 비활성화된 Zone입니다: " + zoneObject.getName());
        }

        try {
            // TODO: 실제 방화벽과의 연동 로직 구현 (현재는 마지막 연동 시각만 업데이트)
            zoneObject.setLastSyncTime(LocalDateTime.now());
            ZoneObject updatedZoneObject = zoneRepository.save(zoneObject);

            // 로그 기록
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            operationLogService.logOperation(
                    username,
                    clientIp,
                    true,
                    "Zone명: " + updatedZoneObject.getName(),
                    "객체관리",
                    "방화벽 연동 수행");

            log.info("방화벽 연동 완료: {}", updatedZoneObject.getName());
            return convertToDto(updatedZoneObject);

        } catch (Exception e) {
            log.error("방화벽 연동 실패: {}", e.getMessage(), e);

            // 로그 기록 (실패)
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            operationLogService.logOperation(
                    username,
                    clientIp,
                    false,
                    "실패 사유: " + e.getMessage(),
                    "객체관리",
                    "방화벽 연동 실패");

            throw new RuntimeException("방화벽 연동 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}