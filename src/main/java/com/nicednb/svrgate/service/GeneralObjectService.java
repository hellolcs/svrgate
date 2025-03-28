package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.GeneralObjectDto;
import com.nicednb.svrgate.entity.GeneralObject;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeneralObjectService {

    private final Logger log = LoggerFactory.getLogger(GeneralObjectService.class);
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository; // 추가: 네트워크 객체 중복 체크를 위해
    private final ZoneObjectRepository zoneObjectRepository;
    private final OperationLogService operationLogService;
    // IP 중복 체크를 위해 ServerObjectRepository 추가
    private final ServerObjectRepository serverObjectRepository;

    /**
     * 모든 일반 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public List<GeneralObjectDto> findAllGeneralObjectsAsDto() {
        return generalObjectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * ID로 일반 객체 조회
     */
    @Transactional(readOnly = true)
    public GeneralObject findById(Long id) {
        return generalObjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일반 객체를 찾을 수 없습니다: " + id));
    }

    /**
     * ID로 일반 객체 조회하여 DTO로 반환
     */
    @Transactional(readOnly = true)
    public GeneralObjectDto findByIdAsDto(Long id) {
        return convertToDto(findById(id));
    }

    /**
     * 일반 객체 검색
     */
    @Transactional(readOnly = true)
    public Page<GeneralObject> searchGeneralObjects(String searchText, Pageable pageable) {
        return generalObjectRepository.searchGeneralObjects(searchText, pageable);
    }

    /**
     * 일반 객체 검색 결과를 DTO 페이지로 반환
     */
    @Transactional(readOnly = true)
    public Page<GeneralObjectDto> searchGeneralObjectsAsDto(String searchText, Pageable pageable) {
        Page<GeneralObject> objectPage = searchGeneralObjects(searchText, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(objectPage, this::convertToDto);
    }

    /**
     * GeneralObjectDto를 GeneralObject 엔티티로 변환
     */
    private GeneralObject convertToEntity(GeneralObjectDto dto) {
        GeneralObject generalObject = new GeneralObject();

        if (dto.getId() != null) {
            generalObject = findById(dto.getId());
        }

        generalObject.setName(dto.getName());
        generalObject.setIpAddress(dto.getIpAddress());
        generalObject.setDescription(dto.getDescription());

        // Zone 설정
        if (dto.getZoneId() != null) {
            ZoneObject zone = zoneObjectRepository.findById(dto.getZoneId())
                    .orElseThrow(() -> new IllegalArgumentException("Zone을 찾을 수 없습니다: " + dto.getZoneId()));
            generalObject.setZoneObject(zone);
        }

        return generalObject;
    }

    /**
     * GeneralObject 엔티티를 GeneralObjectDto로 변환
     */
    public GeneralObjectDto convertToDto(GeneralObject generalObject) {
        GeneralObjectDto dto = new GeneralObjectDto();
        dto.setId(generalObject.getId());
        dto.setName(generalObject.getName());
        dto.setIpAddress(generalObject.getIpAddress());
        dto.setDescription(generalObject.getDescription());

        // Zone ID 및 이름
        if (generalObject.getZoneObject() != null) {
            dto.setZoneId(generalObject.getZoneObject().getId());
            dto.setZoneName(generalObject.getZoneObject().getName());
        }

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
        if (objectId == null) {
            generalObjectRepository.findByIpAddress(ipAddress)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException(
                                "이미 사용 중인 IP 주소입니다: " + ipAddress + " (일반 객체: " + obj.getName() + ")");
                    });
        } else {
            generalObjectRepository.findByIpAddressAndIdNot(ipAddress, objectId)
                    .ifPresent(obj -> {
                        throw new IllegalArgumentException(
                                "이미 사용 중인 IP 주소입니다: " + ipAddress + " (일반 객체: " + obj.getName() + ")");
                    });
        }

        // 네트워크 객체 내에서 IP 중복 체크
        networkObjectRepository.findByIpAddress(ipAddress)
                .ifPresent(obj -> {
                    throw new IllegalArgumentException(
                            "이미 사용 중인 IP 주소입니다: " + ipAddress + " (네트워크 객체: " + obj.getName() + ")");
                });

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
     * 일반 객체 생성
     */
    @Transactional
    public GeneralObjectDto createGeneralObject(GeneralObjectDto dto, String ipAddress) {
        log.info("일반 객체 생성 시작: {}", dto.getName());

        // 일반 객체 이름 중복 체크
        generalObjectRepository.findByName(dto.getName())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 객체 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상)
        checkDuplicateIp(dto.getIpAddress(), null);

        // DTO를 엔티티로 변환
        GeneralObject generalObject = convertToEntity(dto);

        // 저장
        GeneralObject savedGeneralObject = generalObjectRepository.save(generalObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "일반 객체명: " + savedGeneralObject.getName(),
                "객체관리",
                "일반 객체 생성");

        log.info("일반 객체 생성 완료: {}", savedGeneralObject.getName());
        return convertToDto(savedGeneralObject);
    }

    /**
     * 일반 객체 수정
     */
    @Transactional
    public GeneralObjectDto updateGeneralObject(GeneralObjectDto dto, String ipAddress) {
        log.info("일반 객체 수정 시작: {}", dto.getName());

        // 일반 객체 이름 중복 체크 (자기 자신 제외)
        generalObjectRepository.findByNameAndIdNot(dto.getName(), dto.getId())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 객체 이름입니다: " + dto.getName());
                });

        // IP 중복 체크 (모든 객체 타입 대상, 자기 자신 제외)
        checkDuplicateIp(dto.getIpAddress(), dto.getId());

        // DTO를 엔티티로 변환
        GeneralObject generalObject = convertToEntity(dto);

        // 저장
        GeneralObject updatedGeneralObject = generalObjectRepository.save(generalObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "일반 객체명: " + updatedGeneralObject.getName(),
                "객체관리",
                "일반 객체 수정");

        log.info("일반 객체 수정 완료: {}", updatedGeneralObject.getName());
        return convertToDto(updatedGeneralObject);
    }

    /**
     * 일반 객체 삭제
     */
    @Transactional
    public void deleteGeneralObject(Long id, String ipAddress) {
        log.info("일반 객체 삭제 시작: ID={}", id);

        // 일반 객체 존재 여부 확인
        GeneralObject generalObject = findById(id);

        // 삭제
        generalObjectRepository.delete(generalObject);

        // 로그 기록
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "일반 객체명: " + generalObject.getName(),
                "객체관리",
                "일반 객체 삭제");

        log.info("일반 객체 삭제 완료: {}", generalObject.getName());
    }
}