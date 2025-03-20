package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.ServerObjectDto;
import com.nicednb.svrgate.entity.ServerObject;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 연동서버 객체 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ServerObjectService {

    private final Logger log = LoggerFactory.getLogger(ServerObjectService.class);
    private final ServerObjectRepository serverObjectRepository;
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;
    private final ZoneObjectRepository zoneObjectRepository;
    private final OperationLogService operationLogService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 모든 연동서버 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public List<ServerObjectDto> findAllServerObjectsAsDto() {
        return serverObjectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * ID로 연동서버 객체 조회
     */
    @Transactional(readOnly = true)
    public ServerObject findById(Long id) {
        return serverObjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("연동서버 객체를 찾을 수 없습니다: " + id));
    }

    /**
     * ID로 연동서버 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public ServerObjectDto findByIdAsDto(Long id) {
        return convertToDto(findById(id));
    }

    /**
     * 연동서버 객체 검색
     */
    @Transactional(readOnly = true)
    public Page<ServerObject> searchServerObjects(String searchText, Boolean active, Pageable pageable) {
        return serverObjectRepository.searchServerObjects(searchText, active, pageable);
    }

    /**
     * 연동서버 객체 검색 결과를 DTO 페이지로 반환
     */
    @Transactional(readOnly = true)
    public Page<ServerObjectDto> searchServerObjectsAsDto(String searchText, Boolean active, Pageable pageable) {
        Page<ServerObject> objectPage = searchServerObjects(searchText, active, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(objectPage, this::convertToDto);
    }

    /**
     * ServerObjectDto를 ServerObject 엔티티로 변환
     */
    private ServerObject convertToEntity(ServerObjectDto dto) {
        ServerObject serverObject = new ServerObject();

        if (dto.getId() != null) {
            serverObject = findById(dto.getId());
        }

        serverObject.setName(dto.getName());
        serverObject.setIpAddress(dto.getIpAddress());
        serverObject.setActive(dto.isActive());
        serverObject.setDescription(dto.getDescription());
        serverObject.setLastSyncTime(dto.getLastSyncTime());

        // Zone 설정
        if (dto.getZoneId() != null) {
            ZoneObject zone = zoneObjectRepository.findById(dto.getZoneId())
                    .orElseThrow(() -> new IllegalArgumentException("Zone을 찾을 수 없습니다: " + dto.getZoneId()));
            serverObject.setZoneObject(zone);
        }

        return serverObject;
    }

    /**
     * ServerObject 엔티티를 ServerObjectDto로 변환
     */
    public ServerObjectDto convertToDto(ServerObject serverObject) {
        ServerObjectDto dto = new ServerObjectDto();
        dto.setId(serverObject.getId());
        dto.setName(serverObject.getName());
        dto.setIpAddress(serverObject.getIpAddress());
        dto.setActive(serverObject.isActive());
        dto.setDescription(serverObject.getDescription());
        dto.setLastSyncTime(serverObject.getLastSyncTime());
        
        if (serverObject.getLastSyncTime() != null) {
            dto.setLastSyncTimeFormatted(serverObject.getLastSyncTime().format(DATE_TIME_FORMATTER));
        } else {
            dto.setLastSyncTimeFormatted("-");
        }

        // Zone ID 및 이름
        if (serverObject.getZoneObject() != null) {
            dto.setZoneId(serverObject.getZoneObject().getId());
            dto.setZoneName(serverObject.getZoneObject().getName());
        }

        return dto;
    }

    /**
     * IP 중복 체크 (객체 타입에 관계없이 전체 체크)
     * 
     * @param ipAddress 체크할 IP 주소
     * @param objectId 수정 시 자기 자신 제외를 위한 ID (새 객체 생성 시 null)
     * @throws IllegalArgumentException 중복된 IP가 존재하는 경우
     */
    @Transactional(readOnly = true)
    public void checkDuplicateIp(String ipAddress, Long objectId) {
        // 일반 객체 내에서 IP 중복 체크
        generalObjectRepository.findByIpAddress(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 IP 주소입니다: " + ipAddress + " (일반 객체: " + obj.getName() + ")");
                });

        // 네트워크 객체 내에서 IP 중복 체크
        networkObjectRepository.findByIpAddress(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 IP 주소입니다: " + ipAddress + " (네트워크 객체: " + obj.getName() + ")");
                });

        // 연동서버 객체 내에서 IP 중복 체크
        if (objectId == null) {
            serverObjectRepository.findByIpAddress(ipAddress)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException("이미 사용 중인 IP 주소입니다: " + ipAddress + " (연동서버 객체: " + obj.getName() + ")");
                    });
        } else {
            serverObjectRepository.findByIpAddressAndIdNot(ipAddress, objectId)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException("이미 사용 중인 IP 주소입니다: " + ipAddress + " (연동서버 객체: " + obj.getName() + ")");
                    });
        }

        // 방화벽 IP와의 중복 체크 (Zone의 firewallIp와 중복되지 않도록)
        zoneObjectRepository.findByFirewallIp(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 IP 주소입니다: " + ipAddress + " (Zone 방화벽IP: " + obj.getName() + ")");
                });
    }

    /**
     * 연동서버 객체 생성
     */
    @Transactional
    public ServerObjectDto createServerObject(ServerObjectDto dto, String ipAddress) {
        log.info("연동서버 객체 생성 시작: {}", dto.getName());

        // 연동서버 객체 이름 중복 체크
        serverObjectRepository.findByName(dto.getName())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 서버 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상)
        checkDuplicateIp(dto.getIpAddress(), null);

        // DTO를 엔티티로 변환
        ServerObject serverObject = convertToEntity(dto);

        // 저장
        ServerObject savedServerObject = serverObjectRepository.save(serverObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "연동서버 객체명: " + savedServerObject.getName(),
                "객체관리",
                "연동서버 객체 생성");

        log.info("연동서버 객체 생성 완료: {}", savedServerObject.getName());
        return convertToDto(savedServerObject);
    }

    /**
     * 연동서버 객체 수정
     */
    @Transactional
    public ServerObjectDto updateServerObject(ServerObjectDto dto, String ipAddress) {
        log.info("연동서버 객체 수정 시작: {}", dto.getName());

        // 연동서버 객체 존재 여부 확인
        ServerObject existingServerObject = findById(dto.getId());

        // 연동서버 객체 이름 중복 체크 (자기 자신 제외)
        serverObjectRepository.findByNameAndIdNot(dto.getName(), dto.getId())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 서버 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상, 자기 자신 제외)
        checkDuplicateIp(dto.getIpAddress(), dto.getId());

        // DTO를 엔티티로 변환
        ServerObject serverObject = convertToEntity(dto);

        // 저장
        ServerObject updatedServerObject = serverObjectRepository.save(serverObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "연동서버 객체명: " + updatedServerObject.getName(),
                "객체관리",
                "연동서버 객체 수정");

        log.info("연동서버 객체 수정 완료: {}", updatedServerObject.getName());
        return convertToDto(updatedServerObject);
    }

    /**
     * 연동서버 객체 삭제
     */
    @Transactional
    public void deleteServerObject(Long id, String ipAddress) {
        log.info("연동서버 객체 삭제 시작: ID={}", id);

        // 연동서버 객체 존재 여부 확인
        ServerObject serverObject = findById(id);

        // 삭제
        serverObjectRepository.delete(serverObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "연동서버 객체명: " + serverObject.getName(),
                "객체관리",
                "연동서버 객체 삭제");

        log.info("연동서버 객체 삭제 완료: {}", serverObject.getName());
    }
    
    /**
     * 서버와 연동(동기화) 수행
     * 
     * @param id 연동할 서버 객체 ID
     * @param clientIp 클라이언트 IP 주소
     * @return 연동된 서버 객체 DTO
     */
    @Transactional
    public ServerObjectDto syncWithServer(Long id, String clientIp) {
        log.info("서버 연동 시작: ID={}", id);
        
        // 연동서버 객체 존재 여부 확인
        ServerObject serverObject = findById(id);
        
        // 연동 여부 확인
        if (!serverObject.isActive()) {
            throw new IllegalStateException("연동이 비활성화된 서버입니다: " + serverObject.getName());
        }
        
        try {
            // TODO: 실제 서버와의 연동 로직 구현 (현재는 마지막 연동 시각만 업데이트)
            serverObject.setLastSyncTime(LocalDateTime.now());
            ServerObject updatedServerObject = serverObjectRepository.save(serverObject);
            
            // 로그 기록
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            operationLogService.logOperation(
                    username,
                    clientIp,
                    true,
                    "연동서버 객체명: " + updatedServerObject.getName(),
                    "객체관리",
                    "서버 연동 수행");
            
            log.info("서버 연동 완료: {}", updatedServerObject.getName());
            return convertToDto(updatedServerObject);
            
        } catch (Exception e) {
            log.error("서버 연동 실패: {}", e.getMessage(), e);
            
            // 로그 기록 (실패)
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            operationLogService.logOperation(
                    username,
                    clientIp,
                    false,
                    "실패 사유: " + e.getMessage(),
                    "객체관리",
                    "서버 연동 실패");
            
            throw new RuntimeException("서버 연동 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}