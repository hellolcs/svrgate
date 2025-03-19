package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.ZoneDto;
import com.nicednb.svrgate.entity.Zone;
import com.nicednb.svrgate.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final Logger log = LoggerFactory.getLogger(ZoneService.class);
    private final ZoneRepository zoneRepository;
    private final OperationLogService operationLogService;

    /**
     * 모든 Zone 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Zone> findAllZones() {
        return zoneRepository.findAll();
    }

    /**
     * 활성화된 Zone 목록 조회 (드롭다운 선택용)
     */
    @Transactional(readOnly = true)
    public List<Zone> findActiveZones() {
        return zoneRepository.findByActiveOrderById(true);
    }

    /**
     * ID로 Zone 조회
     */
    @Transactional(readOnly = true)
    public Zone findById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone을 찾을 수 없습니다: " + id));
    }

    /**
     * Zone 검색
     */
    @Transactional(readOnly = true)
    public Page<Zone> searchZones(String searchText, Boolean active, Pageable pageable) {
        return zoneRepository.searchZones(searchText, active, pageable);
    }

    /**
     * ZoneDto를 Zone 엔티티로 변환
     */
    private Zone convertToEntity(ZoneDto zoneDto) {
        Zone zone = new Zone();

        if (zoneDto.getId() != null) {
            zone = findById(zoneDto.getId());
        }

        zone.setName(zoneDto.getName());
        zone.setFirewallIp(zoneDto.getFirewallIp());
        zone.setActive(zoneDto.isActive());
        zone.setDescription(zoneDto.getDescription());

        // 비보안Zone 설정
        if (zoneDto.getNonSecureZoneIds() != null && !zoneDto.getNonSecureZoneIds().isEmpty()) {
            Set<Zone> nonSecureZones = zoneDto.getNonSecureZoneIds().stream()
                    .map(this::findById)
                    .collect(Collectors.toSet());
            zone.setNonSecureZones(nonSecureZones);
        } else {
            zone.setNonSecureZones(new HashSet<>());
        }

        // 보안Zone 설정
        if (zoneDto.getSecureZoneIds() != null && !zoneDto.getSecureZoneIds().isEmpty()) {
            Set<Zone> secureZones = zoneDto.getSecureZoneIds().stream()
                    .map(this::findById)
                    .collect(Collectors.toSet());
            zone.setSecureZones(secureZones);
        } else {
            zone.setSecureZones(new HashSet<>());
        }

        return zone;
    }

    /**
     * Zone 엔티티를 ZoneDto로 변환
     */
    public ZoneDto convertToDto(Zone zone) {
        ZoneDto dto = new ZoneDto();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        dto.setFirewallIp(zone.getFirewallIp());
        dto.setActive(zone.isActive());
        dto.setDescription(zone.getDescription());

        // 비보안Zone ID 목록
        if (zone.getNonSecureZones() != null && !zone.getNonSecureZones().isEmpty()) {
            List<Long> nonSecureZoneIds = zone.getNonSecureZones().stream()
                    .map(Zone::getId)
                    .collect(Collectors.toList());
            dto.setNonSecureZoneIds(nonSecureZoneIds);
        }

        // 보안Zone ID 목록
        if (zone.getSecureZones() != null && !zone.getSecureZones().isEmpty()) {
            List<Long> secureZoneIds = zone.getSecureZones().stream()
                    .map(Zone::getId)
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
    public Zone createZone(ZoneDto zoneDto, String ipAddress) {
        log.info("Zone 생성 시작: {}", zoneDto.getName());

        // Zone명 중복 체크
        if (zoneRepository.findByName(zoneDto.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 Zone명입니다: " + zoneDto.getName());
        }

        // 보안Zone과 비보안Zone의 중복 체크
        if (zoneDto.getNonSecureZoneIds() != null && zoneDto.getSecureZoneIds() != null) {
            Set<Long> nonSecureSet = new HashSet<>(zoneDto.getNonSecureZoneIds());
            Set<Long> secureSet = new HashSet<>(zoneDto.getSecureZoneIds());

            // 교집합이 있는지 확인
            Set<Long> intersection = new HashSet<>(nonSecureSet);
            intersection.retainAll(secureSet);

            if (!intersection.isEmpty()) {
                throw new IllegalArgumentException("Zone은 보안Zone과 비보안Zone에 동시에 속할 수 없습니다.");
            }
        }

        // DTO를 엔티티로 변환
        Zone zone = convertToEntity(zoneDto);

        // Zone 저장
        Zone savedZone = zoneRepository.save(zone);

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
        return savedZone;
    }

    /**
     * Zone 수정
     */
    @Transactional
    public Zone updateZone(ZoneDto zoneDto, String ipAddress) {
        log.info("Zone 수정 시작: {}", zoneDto.getName());

        // Zone 존재 여부 확인
        Zone existingZone = findById(zoneDto.getId());

        // Zone명 중복 체크 (자기 자신 제외)
        zoneRepository.findByNameAndIdNot(zoneDto.getName(), zoneDto.getId())
                .ifPresent(zone -> {
                    throw new IllegalArgumentException("이미 사용 중인 Zone명입니다: " + zoneDto.getName());
                });

        // 보안Zone과 비보안Zone의 중복 체크
        if (zoneDto.getNonSecureZoneIds() != null && zoneDto.getSecureZoneIds() != null) {
            Set<Long> nonSecureSet = new HashSet<>(zoneDto.getNonSecureZoneIds());
            Set<Long> secureSet = new HashSet<>(zoneDto.getSecureZoneIds());

            // 교집합이 있는지 확인
            Set<Long> intersection = new HashSet<>(nonSecureSet);
            intersection.retainAll(secureSet);

            if (!intersection.isEmpty()) {
                throw new IllegalArgumentException("Zone은 보안Zone과 비보안Zone에 동시에 속할 수 없습니다.");
            }
        }

        // 자기 자신을 보안/비보안 Zone으로 설정하는지 확인
        if ((zoneDto.getNonSecureZoneIds() != null && zoneDto.getNonSecureZoneIds().contains(zoneDto.getId())) ||
                (zoneDto.getSecureZoneIds() != null && zoneDto.getSecureZoneIds().contains(zoneDto.getId()))) {
            throw new IllegalArgumentException("자기 자신을 보안/비보안 Zone으로 설정할 수 없습니다.");
        }

        // DTO를 엔티티로 변환
        Zone zone = convertToEntity(zoneDto);

        // Zone 저장
        Zone updatedZone = zoneRepository.save(zone);

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
        return updatedZone;
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

        List<Zone> allZones = zoneRepository.findAll();
        List<Zone> referencingZones = new ArrayList<>();

        for (Zone zone : allZones) {
            // 자기 자신은 제외
            if (zone.getId().equals(zoneId)) {
                continue;
            }

            // 비보안 Zone으로 참조되는지 확인
            for (Zone nonSecureZone : zone.getNonSecureZones()) {
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
            for (Zone secureZone : zone.getSecureZones()) {
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
        Zone zone = findById(id);

        // 참조 관계 확인 (다른 Zone에서 참조 중인지 확인)
        ZoneReferenceInfo referenceInfo = checkZoneReferences(id);
        if (referenceInfo.isReferenced()) {
            String referencingZoneNames = referenceInfo.getReferencingZones().stream()
                    .map(Zone::getName)
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
     * 방화벽과 RestAPI 통신 (TODO: 실제 구현 필요)
     */
    public void syncWithFirewall(Long zoneId) {
        log.info("방화벽 동기화 시작: zoneId={}", zoneId);
        // TODO: 방화벽과 RestAPI 통신 구현
        log.info("방화벽 동기화 완료: zoneId={}", zoneId);
    }

    /**
     * Zone 참조 정보를 담는 내부 클래스
     */
    public static class ZoneReferenceInfo {
        private final boolean isReferenced;
        private final List<Zone> referencingZones;

        public ZoneReferenceInfo(boolean isReferenced, List<Zone> referencingZones) {
            this.isReferenced = isReferenced;
            this.referencingZones = referencingZones;
        }

        public boolean isReferenced() {
            return isReferenced;
        }

        public List<Zone> getReferencingZones() {
            return referencingZones;
        }
    }
}