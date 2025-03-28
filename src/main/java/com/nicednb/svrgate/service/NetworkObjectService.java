package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.NetworkObjectDto;
import com.nicednb.svrgate.entity.NetworkObject;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkObjectService {

    private final Logger log = LoggerFactory.getLogger(NetworkObjectService.class);
    private final NetworkObjectRepository networkObjectRepository;
    private final GeneralObjectRepository generalObjectRepository; // 추가: 일반 객체 중복 체크를 위해
    private final ZoneObjectRepository zoneObjectRepository;
    private final OperationLogService operationLogService;
    // IP 중복 체크를 위해 ServerObjectRepository 추가
    private final ServerObjectRepository serverObjectRepository;

    /**
     * 모든 네트워크 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public List<NetworkObjectDto> findAllNetworkObjectsAsDto() {
        return networkObjectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
     * ID로 네트워크 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public NetworkObjectDto findByIdAsDto(Long id) {
        return convertToDto(findById(id));
    }

    /**
     * 네트워크 객체 검색
     */
    @Transactional(readOnly = true)
    public Page<NetworkObject> searchNetworkObjects(String searchText, Pageable pageable) {
        return networkObjectRepository.searchNetworkObjects(searchText, pageable);
    }

    /**
     * 네트워크 객체 검색 결과를 DTO 페이지로 반환
     */
    @Transactional(readOnly = true)
    public Page<NetworkObjectDto> searchNetworkObjectsAsDto(String searchText, Pageable pageable) {
        Page<NetworkObject> objectPage = searchNetworkObjects(searchText, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(objectPage, this::convertToDto);
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
            Set<ZoneObject> zones = dto.getZoneIds().stream()
                    .map(zoneId -> zoneObjectRepository.findById(zoneId)
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
                    .map(ZoneObject::getId)
                    .collect(Collectors.toList());
            dto.setZoneIds(zoneIds);
        }

        // 화면 표시용: Zone 이름 목록
        dto.setZoneNames(networkObject.getZoneNames());

        return dto;
    }

    /**
     * IP 중복 체크 (객체 타입에 관계없이 전체 체크)
     * 
     * @param ipAddress 체크할 IP 주소
     * @param objectId  수정 시 자기 자신 제외를 위한 ID (새 객체 생성 시 null)
     * @throws IllegalArgumentException 중복된 IP가 존재하는 경우
     */
    @Transactional(readOnly = true)
    public void checkDuplicateIp(String ipAddress, Long objectId) {
        // 일반 객체 내에서 IP 중복 체크
        generalObjectRepository.findByIpAddress(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + ipAddress + " (일반 객체: " + obj.getName() + ")");
                });

        // 네트워크 객체 내에서 IP 중복 체크
        if (objectId == null) {
            networkObjectRepository.findByIpAddress(ipAddress)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException(
                                "이미 사용 중인 IP 주소입니다: " + ipAddress + " (네트워크 객체: " + obj.getName() + ")");
                    });
        } else {
            networkObjectRepository.findByIpAddressAndIdNot(ipAddress, objectId)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException(
                                "이미 사용 중인 IP 주소입니다: " + ipAddress + " (네트워크 객체: " + obj.getName() + ")");
                    });
        }

        // 연동서버 객체 내에서 IP 중복 체크 (추가)
        serverObjectRepository.findByIpAddress(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + ipAddress + " (연동서버 객체: " + obj.getName() + ")");
                });

        // 방화벽 IP와의 중복 체크 (Zone의 firewallIp와 중복되지 않도록)
        zoneObjectRepository.findByFirewallIp(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + ipAddress + " (Zone 방화벽IP: " + obj.getName() + ")");
                });
    }

    /**
     * 네트워크 객체 생성
     */
    @Transactional
    public NetworkObjectDto createNetworkObject(NetworkObjectDto dto, String ipAddress) {
        log.info("네트워크 객체 생성 시작: {}", dto.getName());

        // 네트워크 이름 중복 체크
        networkObjectRepository.findByName(dto.getName())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 네트워크 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상)
        checkDuplicateIp(dto.getIpAddress(), null);

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
        return convertToDto(savedNetworkObject);
    }

    /**
     * 네트워크 객체 수정
     */
    @Transactional
    public NetworkObjectDto updateNetworkObject(NetworkObjectDto dto, String ipAddress) {
        log.info("네트워크 객체 수정 시작: {}", dto.getName());

        // 네트워크 이름 중복 체크 (자기 자신 제외)
        networkObjectRepository.findByNameAndIdNot(dto.getName(), dto.getId())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 네트워크 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상, 자기 자신 제외)
        checkDuplicateIp(dto.getIpAddress(), dto.getId());

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
        return convertToDto(updatedNetworkObject);
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