package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.GeneralObjectDto;
import com.nicednb.svrgate.entity.GeneralObject;
import com.nicednb.svrgate.entity.ZoneObject;
import com.nicednb.svrgate.repository.GeneralObjectRepository;
import com.nicednb.svrgate.repository.ZoneObjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneralObjectService {

    private final Logger log = LoggerFactory.getLogger(GeneralObjectService.class);
    private final GeneralObjectRepository generalObjectRepository;
    private final ZoneObjectRepository zoneObjectRepository;
    private final OperationLogService operationLogService;

    /**
     * 모든 일반 객체 조회
     */
    @Transactional(readOnly = true)
    public List<GeneralObject> findAllGeneralObjects() {
        return generalObjectRepository.findAll();
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
     * 일반 객체 검색
     */
    @Transactional(readOnly = true)
    public Page<GeneralObject> searchGeneralObjects(String searchText, Pageable pageable) {
        return generalObjectRepository.searchGeneralObjects(searchText, pageable);
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
            ZoneObject zone = zoneObjectRepository.findById(dto.getZoneId()) // Zone에서 ZoneObject로, zoneRepository에서
                                                                             // zoneObjectRepository로 변경
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
     * 일반 객체 생성
     */
    @Transactional
    public GeneralObject createGeneralObject(GeneralObjectDto dto, String ipAddress) {
        log.info("일반 객체 생성 시작: {}", dto.getName());

        // 일반 객체 이름 중복 체크
        if (generalObjectRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 객체 이름입니다: " + dto.getName());
        }

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
        return savedGeneralObject;
    }

    /**
     * 일반 객체 수정
     */
    @Transactional
    public GeneralObject updateGeneralObject(GeneralObjectDto dto, String ipAddress) {
        log.info("일반 객체 수정 시작: {}", dto.getName());

        // 일반 객체 존재 여부 확인
        GeneralObject existingGeneralObject = findById(dto.getId());

        // 일반 객체 이름 중복 체크 (자기 자신 제외)
        generalObjectRepository.findByNameAndIdNot(dto.getName(), dto.getId())
                .ifPresent(obj -> {
                    throw new IllegalArgumentException("이미 사용 중인 객체 이름입니다: " + dto.getName());
                });

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
        return updatedGeneralObject;
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