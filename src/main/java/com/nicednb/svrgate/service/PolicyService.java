package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.dto.SourceObjectDto;
import com.nicednb.svrgate.entity.*;
import com.nicednb.svrgate.repository.*;
import com.nicednb.svrgate.util.PageConversionUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final Logger log = LoggerFactory.getLogger(PolicyService.class);
    private final PolicyRepository policyRepository;
    private final ServerObjectRepository serverObjectRepository;
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;
    private final OperationLogService operationLogService;
    private final AccountRepository accountRepository; // 계정 이름 조회를 위해 추가

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 모든 활성화된 서버 객체 ID와 이름, 및 정책 수 조회
     */
    @Transactional(readOnly = true)
    public List<ServerPolicySummary> getAllServerPolicySummaries() {
        // 활성화된 서버만 필터링
        List<ServerObject> servers = serverObjectRepository.findAll().stream()
                .filter(ServerObject::isActive)
                .collect(Collectors.toList());

        return servers.stream()
                .map(server -> {
                    long policyCount = policyRepository.countByServerObjectId(server.getId());
                    return new ServerPolicySummary(server.getId(), server.getName(), policyCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 서버의 정책 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByServerId(Long serverId) {
        List<Policy> policies = policyRepository.findByServerObjectIdOrderByPriorityAsc(serverId);
        return policies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 서버의 정책 목록 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<PolicyDto> getPoliciesByServerIdPaged(Long serverId, Pageable pageable) {
        Page<Policy> policyPage = policyRepository.findByServerObjectIdOrderByPriorityAsc(serverId, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(policyPage, this::convertToDto);
    }

    /**
     * 검색 조건으로 정책 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PolicyDto> searchPolicies(Long serverId, String searchText, Pageable pageable) {
        Page<Policy> policyPage = policyRepository.searchPolicies(serverId, searchText, pageable);
        return PageConversionUtil.convertEntityPageToDtoPage(policyPage, this::convertToDto);
    }

    /**
     * ID로 정책 조회
     */
    @Transactional(readOnly = true)
    public PolicyDto getPolicyById(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + id));
        return convertToDto(policy);
    }

    /**
     * 출발지 객체 검색
     */
    @Transactional(readOnly = true)
    public List<SourceObjectDto> searchSourceObjects(String searchText) {
        List<SourceObjectDto> result = new ArrayList<>();

        // 연동서버 객체 검색
        if (searchText == null || searchText.isEmpty()) {
            serverObjectRepository.findAll().forEach(server -> {
                SourceObjectDto dto = new SourceObjectDto();
                dto.setId(server.getId());
                dto.setName(server.getName());
                dto.setType("SERVER");
                dto.setIpAddress(server.getIpAddress());
                if (server.getZoneObject() != null) {
                    dto.setZone(server.getZoneObject().getName());
                }
                result.add(dto);
            });
        } else {
            serverObjectRepository.findAll().stream()
                    .filter(server -> server.getName().contains(searchText) ||
                            server.getIpAddress().contains(searchText))
                    .forEach(server -> {
                        SourceObjectDto dto = new SourceObjectDto();
                        dto.setId(server.getId());
                        dto.setName(server.getName());
                        dto.setType("SERVER");
                        dto.setIpAddress(server.getIpAddress());
                        if (server.getZoneObject() != null) {
                            dto.setZone(server.getZoneObject().getName());
                        }
                        result.add(dto);
                    });
        }

        // 일반 객체 검색
        if (searchText == null || searchText.isEmpty()) {
            generalObjectRepository.findAll().forEach(obj -> {
                SourceObjectDto dto = new SourceObjectDto();
                dto.setId(obj.getId());
                dto.setName(obj.getName());
                dto.setType("GENERAL");
                dto.setIpAddress(obj.getIpAddress());
                if (obj.getZoneObject() != null) {
                    dto.setZone(obj.getZoneObject().getName());
                }
                result.add(dto);
            });
        } else {
            generalObjectRepository.findAll().stream()
                    .filter(obj -> obj.getName().contains(searchText) ||
                            obj.getIpAddress().contains(searchText))
                    .forEach(obj -> {
                        SourceObjectDto dto = new SourceObjectDto();
                        dto.setId(obj.getId());
                        dto.setName(obj.getName());
                        dto.setType("GENERAL");
                        dto.setIpAddress(obj.getIpAddress());
                        if (obj.getZoneObject() != null) {
                            dto.setZone(obj.getZoneObject().getName());
                        }
                        result.add(dto);
                    });
        }

        // 네트워크 객체 검색
        if (searchText == null || searchText.isEmpty()) {
            networkObjectRepository.findAll().forEach(obj -> {
                SourceObjectDto dto = new SourceObjectDto();
                dto.setId(obj.getId());
                dto.setName(obj.getName());
                dto.setType("NETWORK");
                dto.setIpAddress(obj.getIpAddress());
                dto.setZone(obj.getZoneNames()); // 네트워크 객체는 여러 Zone에 속할 수 있음
                result.add(dto);
            });
        } else {
            networkObjectRepository.findAll().stream()
                    .filter(obj -> obj.getName().contains(searchText) ||
                            obj.getIpAddress().contains(searchText))
                    .forEach(obj -> {
                        SourceObjectDto dto = new SourceObjectDto();
                        dto.setId(obj.getId());
                        dto.setName(obj.getName());
                        dto.setType("NETWORK");
                        dto.setIpAddress(obj.getIpAddress());
                        dto.setZone(obj.getZoneNames()); // 네트워크 객체는 여러 Zone에 속할 수 있음
                        result.add(dto);
                    });
        }

        return result;
    }

    /**
     * 출발지 객체 이름 조회
     */
    @Transactional(readOnly = true)
    public String getSourceObjectName(Long id, String type) {
        if (id == null || type == null) {
            return "";
        }

        switch (type) {
            case "SERVER":
                return serverObjectRepository.findById(id)
                        .map(ServerObject::getName)
                        .orElse("");
            case "GENERAL":
                return generalObjectRepository.findById(id)
                        .map(GeneralObject::getName)
                        .orElse("");
            case "NETWORK":
                return networkObjectRepository.findById(id)
                        .map(NetworkObject::getName)
                        .orElse("");
            default:
                return "";
        }
    }

    /**
     * 정책 생성
     */
    @Transactional
    public PolicyDto createPolicy(PolicyDto policyDto, String ipAddress) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 등록자 이름 조회 (username -> name)
        String registrarName = accountRepository.findByUsername(username)
                .map(Account::getName)
                .orElse(username);

        // 등록자 정보 설정 (이름으로 설정)
        policyDto.setRegistrar(registrarName);

        // 서버 존재 여부 확인
        ServerObject serverObject = serverObjectRepository.findById(policyDto.getServerObjectId())
                .orElseThrow(() -> new IllegalArgumentException("서버를 찾을 수 없습니다: " + policyDto.getServerObjectId()));

        // 서버가 활성화 상태인지 확인
        if (!serverObject.isActive()) {
            throw new IllegalArgumentException("비활성화된 서버에는 정책을 추가할 수 없습니다: " + serverObject.getName());
        }

        // 출발지 객체 존재 여부 확인
        validateSourceObject(policyDto.getSourceObjectId(), policyDto.getSourceObjectType());

        // 포트 범위 유효성 검사 (Multi 모드인 경우)
        if ("multi".equals(policyDto.getPortMode()) &&
                policyDto.getEndPort() != null &&
                policyDto.getStartPort() != null &&
                policyDto.getEndPort() <= policyDto.getStartPort()) {
            throw new IllegalArgumentException("종료 포트는 시작 포트보다 커야 합니다.");
        }

        // DTO를 엔티티로 변환
        Policy policy = convertToEntity(policyDto);
        policy.setServerObject(serverObject);
        policy.setRegistrationDate(LocalDateTime.now());

        // 정책 저장
        Policy savedPolicy = policyRepository.save(policy);

        // 작업 로그 기록
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "정책 ID: " + savedPolicy.getId() + ", 서버: " + serverObject.getName(),
                "정책관리",
                "정책 생성");

        return convertToDto(savedPolicy);
    }

    /**
     * 정책 수정 - 요청자와 설명만 수정 가능
     */
    @Transactional
    public PolicyDto updatePolicy(PolicyDto policyDto, String ipAddress) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 정책 존재 여부 확인
        Policy existingPolicy = policyRepository.findById(policyDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + policyDto.getId()));

        // 서버 존재 여부 확인
        ServerObject serverObject = existingPolicy.getServerObject(); // 기존 서버 객체 사용

        // 요청자와 설명만 업데이트
        existingPolicy.setRequester(policyDto.getRequester());
        existingPolicy.setDescription(policyDto.getDescription());

        // 정책 저장
        Policy updatedPolicy = policyRepository.save(existingPolicy);

        // 작업 로그 기록
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "정책 ID: " + updatedPolicy.getId() + ", 서버: " + serverObject.getName(),
                "정책관리",
                "정책 메타정보 수정(요청자/설명)");

        return convertToDto(updatedPolicy);
    }

    /**
     * 정책 삭제
     */
    @Transactional
    public void deletePolicy(Long id, String ipAddress) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 정책 존재 여부 확인
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + id));

        // 서버 이름 저장 (로깅용)
        String serverName = policy.getServerObject() != null ? policy.getServerObject().getName() : "Unknown";

        // 정책 삭제
        policyRepository.delete(policy);

        // 작업 로그 기록
        operationLogService.logOperation(
                username,
                ipAddress,
                true,
                "정책 ID: " + id + ", 서버: " + serverName,
                "정책관리",
                "정책 삭제");
    }

    /**
     * 출발지 객체 유효성 검사
     */
    private void validateSourceObject(Long id, String type) {
        if (id == null || type == null) {
            throw new IllegalArgumentException("출발지 객체 정보가 올바르지 않습니다.");
        }

        switch (type) {
            case "SERVER":
                serverObjectRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("연동서버 객체를 찾을 수 없습니다: " + id));
                break;
            case "GENERAL":
                generalObjectRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("일반 객체를 찾을 수 없습니다: " + id));
                break;
            case "NETWORK":
                networkObjectRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("네트워크 객체를 찾을 수 없습니다: " + id));
                break;
            default:
                throw new IllegalArgumentException("유효하지 않은 출발지 객체 유형입니다: " + type);
        }
    }

    /**
     * PolicyDto를 Policy 엔티티로 변환
     */
    private Policy convertToEntity(PolicyDto dto) {
        Policy policy = new Policy();

        if (dto.getId() != null) {
            policy.setId(dto.getId());
        }

        policy.setPriority(dto.getPriority());
        policy.setSourceObjectId(dto.getSourceObjectId());
        policy.setSourceObjectType(dto.getSourceObjectType());
        policy.setProtocol(dto.getProtocol());
        policy.setPortMode(dto.getPortMode());
        policy.setStartPort(dto.getStartPort());

        // portMode가 multi인 경우에만 endPort 설정
        if ("multi".equals(dto.getPortMode()) && dto.getEndPort() != null) {
            policy.setEndPort(dto.getEndPort());
        } else {
            // single인 경우 startPort와 동일하게 설정
            policy.setEndPort(dto.getStartPort());
        }

        policy.setAction(dto.getAction());
        policy.setTimeLimit(dto.getTimeLimit());
        policy.setLogging(dto.getLogging());
        policy.setRequester(dto.getRequester());
        policy.setRegistrar(dto.getRegistrar());
        policy.setDescription(dto.getDescription());

        return policy;
    }

    /**
     * Policy 엔티티를 PolicyDto로 변환
     */
    private PolicyDto convertToDto(Policy policy) {
        PolicyDto dto = new PolicyDto();

        dto.setId(policy.getId());
        dto.setPriority(policy.getPriority());
        dto.setSourceObjectId(policy.getSourceObjectId());
        dto.setSourceObjectType(policy.getSourceObjectType());
        dto.setSourceObjectName(getSourceObjectName(policy.getSourceObjectId(), policy.getSourceObjectType()));
        dto.setProtocol(policy.getProtocol());
        dto.setPortMode(policy.getPortMode());
        dto.setStartPort(policy.getStartPort());
        dto.setEndPort(policy.getEndPort());
        dto.setAction(policy.getAction());
        dto.setTimeLimit(policy.getTimeLimit());
        dto.setLogging(policy.getLogging());
        dto.setRegistrationDate(policy.getRegistrationDate());
        dto.setExpiresAt(policy.getExpiresAt()); // 만료 시간 설정

        if (policy.getRegistrationDate() != null) {
            dto.setRegistrationDateFormatted(policy.getRegistrationDate().format(DATE_TIME_FORMATTER));
        }

        if (policy.getExpiresAt() != null) {
            dto.setExpiresAtFormatted(policy.getExpiresAt().format(DATE_TIME_FORMATTER));
        } else {
            dto.setExpiresAtFormatted("무기한");
        }

        dto.setRequester(policy.getRequester());
        dto.setRegistrar(policy.getRegistrar());
        dto.setDescription(policy.getDescription());

        if (policy.getServerObject() != null) {
            dto.setServerObjectId(policy.getServerObject().getId());
            dto.setServerObjectName(policy.getServerObject().getName());
        }

        return dto;
    }

    /**
     * 서버별 정책 요약 정보 클래스
     */
    @Getter
    public static class ServerPolicySummary {
        private final Long serverId;
        private final String serverName;
        private final long policyCount;

        public ServerPolicySummary(Long serverId, String serverName, long policyCount) {
            this.serverId = serverId;
            this.serverName = serverName;
            this.policyCount = policyCount;
        }
    }
}