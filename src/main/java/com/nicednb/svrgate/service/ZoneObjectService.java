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

    /**
     * 모든 Zone 목록 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public List<ZoneObjectDto> findAllZonesAsDto() {
        return zoneRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 드롭다운 선택용 Zone 목록 조회 (연동여부와 상관없이 모든 Zone 조회)
     */
    @Transactional(readOnly = true)
    public List<ZoneObject> findAllZonesForDropdown() {
        return zoneRepository.findAllByOrderByIdAsc();
    }

    /**
     * 드롭다운 선택용 Zone 목록 DTO로 조회
     */
    @Transactional(readOnly = true)
    public List<ZoneObjectDto> findAllZonesForDropdownAsDto() {
        return findAllZonesForDropdown().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
     * ID로 Zone 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public ZoneObjectDto findByIdAsDto(Long id) {
        return convertToDto(findById(id));
    }

    /**
     * Zone 검색
     */
    @Transactional(readOnly = true)
    public Page<ZoneObject> searchZones(String searchText, Boolean active, Pageable pageable) {
        return zoneRepository.searchZones(searchText, active, pageable);
    }

    /**
     * Zone 검색 결과를 DTO 페이지로 반환
     */
    @Transactional(readOnly = true)
    public Page<ZoneObjectDto> searchZonesAsDto(String searchText, Boolean active, Pageable pageable) {
        Page<ZoneObject> zonePage = searchZones(searchText, active, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(zonePage, this::convertToDto);
    }

    /**
     * 방화벽 IP 중복 체크 (객체 타입에 관계없이 전체 체크)
     * 
     * @param firewallIp 체크할 방화벽 IP 주소
     * @param zoneId     수정 시 자기 자신 제외를 위한 ID (새 객체 생성 시 null)
     * @throws IllegalArgumentException 중복된 IP가 존재하는 경우
     */
    @Transactional(readOnly = true)
    public void checkDuplicateFirewallIp(String firewallIp, Long zoneId) {
        log.debug("방화벽 IP 중복 검사: firewallIp={}, zoneId={}", firewallIp, zoneId);

        // 일반 객체의 IP와 중복 체크
        generalObjectRepository.findByIpAddress(firewallIp)
                .ifPresent(obj -> {
                    log.warn("IP 중복 발견 (일반 객체): {}, 객체명: {}", firewallIp, obj.getName());
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + firewallIp + " (일반 객체: " + obj.getName() + ")");
                });

        // 네트워크 객체의 IP와 중복 체크
        networkObjectRepository.findByIpAddress(firewallIp)
                .ifPresent(obj -> {
                    log.warn("IP 중복 발견 (네트워크 객체): {}, 객체명: {}", firewallIp, obj.getName());
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + firewallIp + " (네트워크 객체: " + obj.getName() + ")");
                });

        // 연동서버 객체의 IP와 중복 체크
        serverObjectRepository.findByIpAddress(firewallIp)
                .ifPresent(obj -> {
                    log.warn("IP 중복 발견 (연동서버 객체): {}, 객체명: {}", firewallIp, obj.getName());
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + firewallIp + " (연동서버 객체: " + obj.getName() + ")");
                });

        // Zone의 방화벽 IP 중복 체크
        if (zoneId == null) {
            zoneRepository.findByFirewallIp(firewallIp)
                    .ifPresent(obj -> {
                        log.warn("방화벽 IP 중복 발견 (Zone): {}, Zone명: {}", firewallIp, obj.getName());
                        throw new IllegalArgumentException(
                                "이미 사용 중인 방화벽 IP입니다: " + firewallIp + " (Zone: " + obj.getName() + ")");
                    });
        } else {
            zoneRepository.findByFirewallIpAndIdNot(firewallIp, zoneId)
                    .ifPresent(obj -> {
                        log.warn("방화벽 IP 중복 발견 (Zone): {}, Zone명: {}", firewallIp, obj.getName());
                        throw new IllegalArgumentException(
                                "이미 사용 중인 방화벽 IP입니다: " + firewallIp + " (Zone: " + obj.getName() + ")");
                    });
        }

        log.debug("방화벽 IP 중복 검사 완료: 중복 없음");
    }

    /**
     * ZoneDto를 Zone 엔티티로 변환
     */
    private ZoneObject convertToEntity(ZoneObjectDto zoneDto) {
        ZoneObject zone = new ZoneObject();

        if (zoneDto.getId() != null) {
            zone = findById(zoneDto.getId());
        }

        zone.setName(zoneDto.getName());
        zone.setFirewallIp(zoneDto.getFirewallIp());
        zone.setActive(zoneDto.isActive());
        zone.setDescription(zoneDto.getDescription());

        // 비보안Zone 설정
        if (zoneDto.getNonSecureZoneIds() != null && !zoneDto.getNonSecureZoneIds().isEmpty()) {
            Set<ZoneObject> nonSecureZones = zoneDto.getNonSecureZoneIds().stream()
                    .map(this::findById)
                    .collect(Collectors.toSet());
            zone.setNonSecureZones(nonSecureZones);
        } else {
            zone.setNonSecureZones(new HashSet<>());
        }

        // 보안Zone 설정
        if (zoneDto.getSecureZoneIds() != null && !zoneDto.getSecureZoneIds().isEmpty()) {
            Set<ZoneObject> secureZones = zoneDto.getSecureZoneIds().stream()
                    .map(this::findById)
                    .collect(Collectors.toSet());
            zone.setSecureZones(secureZones);
        } else {
            zone.setSecureZones(new HashSet<>());
        }

        return zone;
    }

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
     * Zone 생성
     */
    @Transactional
    public ZoneObjectDto createZone(ZoneObjectDto zoneDto, String ipAddress) {
        log.info("Zone 생성 시작: {}", zoneDto.getName());

        // Zone명 중복 체크
        zoneRepository.findByName(zoneDto.getName())
                .ifPresent(zone -> {
                    log.warn("Zone명 중복 발견: {}", zoneDto.getName());
                    throw new IllegalArgumentException("이미 사용 중인 Zone명입니다: " + zoneDto.getName());
                });

        // 방화벽 IP 중복 체크 (모든 객체 타입 대상)
        checkDuplicateFirewallIp(zoneDto.getFirewallIp(), null);

        // 보안Zone과 비보안Zone의 중복 체크
        if (zoneDto.getNonSecureZoneIds() != null && zoneDto.getSecureZoneIds() != null) {
            Set<Long> nonSecureSet = new HashSet<>(zoneDto.getNonSecureZoneIds());
            Set<Long> secureSet = new HashSet<>(zoneDto.getSecureZoneIds());

            // 교집합이 있는지 확인
            Set<Long> intersection = new HashSet<>(nonSecureSet);
            intersection.retainAll(secureSet);

            if (!intersection.isEmpty()) {
                log.warn("Zone이 보안Zone과 비보안Zone에 동시에 속함");
                throw new IllegalArgumentException("Zone은 보안Zone과 비보안Zone에 동시에 속할 수 없습니다.");
            }
        }

        // DTO를 엔티티로 변환
        ZoneObject zone = convertToEntity(zoneDto);

        // Zone 저장
        ZoneObject savedZone = zoneRepository.save(zone);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "Zone명: " + savedZone.getName(), // 로그에 추가 정보 기록
                "객체관리",
                "Zone 생성");

        log.info("Zone 생성 완료: {}", savedZone.getName());
        return convertToDto(savedZone);
    }

    /**
     * Zone 수정
     */
    @Transactional
    public ZoneObjectDto updateZone(ZoneObjectDto zoneDto, String ipAddress) {
        log.info("Zone 수정 시작: {}", zoneDto.getName());

        // Zone명 중복 체크 (자기 자신 제외)
        zoneRepository.findByNameAndIdNot(zoneDto.getName(), zoneDto.getId())
                .ifPresent(zone -> {
                    log.warn("Zone명 중복 발견 (수정): {}", zoneDto.getName());
                    throw new IllegalArgumentException("이미 사용 중인 Zone명입니다: " + zoneDto.getName());
                });

        // 방화벽 IP 중복 체크 (모든 객체 타입 대상, 자기 자신 제외)
        checkDuplicateFirewallIp(zoneDto.getFirewallIp(), zoneDto.getId());

        // 보안Zone과 비보안Zone의 중복 체크
        if (zoneDto.getNonSecureZoneIds() != null && zoneDto.getSecureZoneIds() != null) {
            Set<Long> nonSecureSet = new HashSet<>(zoneDto.getNonSecureZoneIds());
            Set<Long> secureSet = new HashSet<>(zoneDto.getSecureZoneIds());

            // 교집합이 있는지 확인
            Set<Long> intersection = new HashSet<>(nonSecureSet);
            intersection.retainAll(secureSet);

            if (!intersection.isEmpty()) {
                log.warn("Zone이 보안Zone과 비보안Zone에 동시에 속함 (수정)");
                throw new IllegalArgumentException("Zone은 보안Zone과 비보안Zone에 동시에 속할 수 없습니다.");
            }
        }

        // 자기 자신을 보안/비보안 Zone으로 설정하는지 확인
        if ((zoneDto.getNonSecureZoneIds() != null && zoneDto.getNonSecureZoneIds().contains(zoneDto.getId())) ||
                (zoneDto.getSecureZoneIds() != null && zoneDto.getSecureZoneIds().contains(zoneDto.getId()))) {
            log.warn("자기 자신을 보안/비보안 Zone으로 설정 시도: {}", zoneDto.getId());
            throw new IllegalArgumentException("자기 자신을 보안/비보안 Zone으로 설정할 수 없습니다.");
        }

        // DTO를 엔티티로 변환
        ZoneObject zone = convertToEntity(zoneDto);

        // Zone 저장
        ZoneObject updatedZone = zoneRepository.save(zone);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "Zone명: " + updatedZone.getName(), // 로그에 추가 정보 기록
                "객체관리",
                "Zone 수정");

        log.info("Zone 수정 완료: {}", updatedZone.getName());
        return convertToDto(updatedZone);
    }

    /**
     * 특정 Zone이 다른 Zone에서 참조되고 있는지 확인
     * 
     * @param zoneId 확인할 Zone ID
     * @return 참조 정보 객체 (참조 여부와 참조하는 Zone 목록)
     */
    @Transactional(readOnly = true)
    public ZoneReferenceInfo checkZoneReferences(Long zoneId) {
        log.debug("Zone 참조 확인: ID={}", zoneId);

        List<ZoneObject> allZones = zoneRepository.findAll();
        List<ZoneObject> referencingZones = new ArrayList<>();

        for (ZoneObject zone : allZones) {
            // 자기 자신은 제외
            if (zone.getId().equals(zoneId)) {
                continue;
            }

            // 비보안 Zone으로 참조되는지 확인
            for (ZoneObject nonSecureZone : zone.getNonSecureZones()) {
                if (nonSecureZone.getId().equals(zoneId)) {
                    referencingZones.add(zone);
                    break;
                }
            }

            // 이미 참조가 확인되었으면 다음 Zone으로
            if (referencingZones.contains(zone)) {
                continue;
            }

            // 보안 Zone으로 참조되는지 확인
            for (ZoneObject secureZone : zone.getSecureZones()) {
                if (secureZone.getId().equals(zoneId)) {
                    referencingZones.add(zone);
                    break;
                }
            }
        }

        boolean isReferenced = !referencingZones.isEmpty();
        return new ZoneReferenceInfo(isReferenced, referencingZones);
    }

    /**
     * Zone 삭제
     * 
     * @param id        Zone ID
     * @param ipAddress 클라이언트 IP 주소
     * @throws IllegalStateException 다른 Zone에서 참조 중인 경우 발생
     */
    @Transactional
    public void deleteZone(Long id, String ipAddress) {
        log.info("Zone 삭제 시작: ID={}", id);

        // Zone 존재 여부 확인
        ZoneObject zone = findById(id);

        // 참조 관계 확인 (다른 Zone에서 참조 중인지 확인)
        ZoneReferenceInfo referenceInfo = checkZoneReferences(id);
        if (referenceInfo.isReferenced()) {
            String referencingZoneNames = referenceInfo.getReferencingZones().stream()
                    .map(ZoneObject::getName)
                    .collect(Collectors.joining(", "));

            log.warn("Zone 삭제 실패: 다른 Zone에서 참조 중입니다. Zone={}, 참조 Zone 목록: {}",
                    zone.getName(), referencingZoneNames);

            throw new IllegalStateException(
                    "삭제하려는 Zone이 다른 Zone에서 참조되고 있어 삭제할 수 없습니다. 참조 Zone 목록: " + referencingZoneNames);
        }

        // Zone 삭제
        zoneRepository.delete(zone);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "Zone명: " + zone.getName(), // 로그에 추가 정보 기록
                "객체관리",
                "Zone 삭제");

        log.info("Zone 삭제 완료: {}", zone.getName());
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

    /**
     * Zone 참조 정보를 담는 내부 클래스
     */
    public static class ZoneReferenceInfo {
        private final boolean isReferenced;
        private final List<ZoneObject> referencingZones;

        public ZoneReferenceInfo(boolean isReferenced, List<ZoneObject> referencingZones) {
            this.isReferenced = isReferenced;
            this.referencingZones = referencingZones;
        }

        public boolean isReferenced() {
            return isReferenced;
        }

        public List<ZoneObject> getReferencingZones() {
            return referencingZones;
        }
    }
}