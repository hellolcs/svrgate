package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.NetworkObjectDto;
import com.nicednb.svrgate.entity.NetworkObject;
import com.nicednb.svrgate.entity.ZoneObject;
import com.nicednb.svrgate.repository.NetworkObjectRepository;
import com.nicednb.svrgate.repository.ZoneObjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkObjectService {

    private final Logger log = LoggerFactory.getLogger(NetworkObjectService.class);
    private final NetworkObjectRepository networkObjectRepository;
    private final ZoneObjectRepository zoneObjectRepository;
    private final OperationLogService operationLogService;

    /**
     * 모든 네트워크 객체 조회
     */
    @Transactional(readOnly = true)
    public List<NetworkObject> findAllNetworkObjects() {
        return networkObjectRepository.findAll();
    }

    /**
     * ID로 네트워크 객체 조회
     */
    @Transactional(readOnly = true)
    public NetworkObject findById(Long id) {
        return networkObjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("네트워크 객체를 찾을 수 없습니다: " + id));
    }

    /**
     * 네트워크 객체 검색
     */
    @Transactional(readOnly = true)
    public Page<NetworkObject> searchNetworkObjects(String searchText, Pageable pageable) {
        return networkObjectRepository.searchNetworkObjects(searchText, pageable);
    }

    /**
     * NetworkObjectDto를 NetworkObject 엔티티로 변환
     */
    private NetworkObject convertToEntity(NetworkObjectDto dto) {
        NetworkObject networkObject = new NetworkObject();

        if (dto.getId() != null) {
            networkObject = findById(dto.getId());
        }

        networkObject.setName(dto.getName());
        networkObject.setIpAddress(dto.getIpAddress());
        networkObject.setDescription(dto.getDescription());

        // Zones 설정
        if (dto.getZoneIds() != null && !dto.getZoneIds().isEmpty()) {
            Set<ZoneObject> zones = dto.getZoneIds().stream() // Zone에서 ZoneObject로 변경
                    .map(zoneId -> zoneObjectRepository.findById(zoneId) // zoneRepository에서 zoneObjectRepository로 변경
                            .orElseThrow(() -> new IllegalArgumentException("Zone을 찾을 수 없습니다: " + zoneId)))
                    .collect(Collectors.toSet());
            networkObject.setZones(zones);
        } else {
            networkObject.setZones(new HashSet<>());
        }

        return networkObject;
    }

    /**
     * NetworkObject 엔티티를 NetworkObjectDto로 변환
     */
    public NetworkObjectDto convertToDto(NetworkObject networkObject) {
        NetworkObjectDto dto = new NetworkObjectDto();
        dto.setId(networkObject.getId());
        dto.setName(networkObject.getName());
        dto.setIpAddress(networkObject.getIpAddress());
        dto.setDescription(networkObject.getDescription());

        // Zone ID 목록
        if (networkObject.getZones() != null && !networkObject.getZones().isEmpty()) {
            List<Long> zoneIds = networkObject.getZones().stream()
                    .map(ZoneObject::getId) // Zone에서 ZoneObject로 변경
                    .collect(Collectors.toList());
            dto.setZoneIds(zoneIds);
        }

        // 화면 표시용: Zone 이름 목록
        dto.setZoneNames(networkObject.getZoneNames());

        return dto;
    }

    /**
     * 네트워크 객체 생성
     */
    @Transactional
    public NetworkObject createNetworkObject(NetworkObjectDto dto, String ipAddress) {
        log.info("네트워크 객체 생성 시작: {}", dto.getName());

        // 네트워크 이름 중복 체크
        if (networkObjectRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 네트워크 이름입니다: " + dto.getName());
        }

        // DTO를 엔티티로 변환
        NetworkObject networkObject = convertToEntity(dto);

        // 저장
        NetworkObject savedNetworkObject = networkObjectRepository.save(networkObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "네트워크 객체명: " + savedNetworkObject.getName(),
                "객체관리",
                "네트워크 객체 생성");

        log.info("네트워크 객체 생성 완료: {}", savedNetworkObject.getName());
        return savedNetworkObject;
    }

    /**
     * 네트워크 객체 수정
     */
    @Transactional
    public NetworkObject updateNetworkObject(NetworkObjectDto dto, String ipAddress) {
        log.info("네트워크 객체 수정 시작: {}", dto.getName());

        // 네트워크 객체 존재 여부 확인
        NetworkObject existingNetworkObject = findById(dto.getId());

        // 네트워크 이름 중복 체크 (자기 자신 제외)
        networkObjectRepository.findByNameAndIdNot(dto.getName(), dto.getId())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 네트워크 이름입니다: " + dto.getName());
                });

        // DTO를 엔티티로 변환
        NetworkObject networkObject = convertToEntity(dto);

        // 저장
        NetworkObject updatedNetworkObject = networkObjectRepository.save(networkObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "네트워크 객체명: " + updatedNetworkObject.getName(),
                "객체관리",
                "네트워크 객체 수정");

        log.info("네트워크 객체 수정 완료: {}", updatedNetworkObject.getName());
        return updatedNetworkObject;
    }

    /**
     * 네트워크 객체 삭제
     */
    @Transactional
    public void deleteNetworkObject(Long id, String ipAddress) {
        log.info("네트워크 객체 삭제 시작: ID={}", id);

        // 네트워크 객체 존재 여부 확인
        NetworkObject networkObject = findById(id);

        // 삭제
        networkObjectRepository.delete(networkObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "네트워크 객체명: " + networkObject.getName(),
                "객체관리",
                "네트워크 객체 삭제");

        log.info("네트워크 객체 삭제 완료: {}", networkObject.getName());
    }
}