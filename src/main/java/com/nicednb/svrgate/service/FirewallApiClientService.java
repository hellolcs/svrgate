package com.nicednb.svrgate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicednb.svrgate.dto.FirewallRuleRequest;
import com.nicednb.svrgate.dto.FirewallRuleResponse;
import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.entity.GeneralObject;
import com.nicednb.svrgate.entity.NetworkObject;
import com.nicednb.svrgate.entity.ServerObject;
import com.nicednb.svrgate.repository.GeneralObjectRepository;
import com.nicednb.svrgate.repository.NetworkObjectRepository;
import com.nicednb.svrgate.repository.PolicyRepository;
import com.nicednb.svrgate.repository.ServerObjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 방화벽 API를 호출하기 위한 클라이언트 서비스
 * 연동서버와의 통신을 담당합니다.
 * SvrGate 방화벽 관리 Agent API 정의서에 맞게 구현되었습니다.
 */
@Service
@RequiredArgsConstructor
public class FirewallApiClientService {

    private final Logger log = LoggerFactory.getLogger(FirewallApiClientService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ServerObjectRepository serverObjectRepository;
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;
    private final PolicyRepository policyRepository;
    private final SystemSettingService systemSettingService; // 시스템 설정 서비스 주입

    // API 응답 코드 상수 정의
    public static final String CODE_SUCCESS = "SUCCESS";
    public static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String CODE_RULE_NOT_FOUND = "RULE_NOT_FOUND";
    public static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String CODE_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String CODE_INVALID_PARAMETER = "INVALID_PARAMETER";
    public static final String CODE_FIREWALL_ERROR = "FIREWALL_ERROR";

    /**
     * 방화벽 정책 추가 API를 호출합니다.
     * 
     * @param server    연동서버 정보
     * @param policyDto 추가할 정책 정보
     * @return API 호출 결과
     */
    public FirewallApiResponse addPolicy(ServerObject server, PolicyDto policyDto) {
        try {
            log.info("방화벽 정책 추가 API 호출: 서버={}, 정책 우선순위={}", server.getName(), policyDto.getPriority());

            // 시스템 설정에서 포트 가져오기
            int serverPort = systemSettingService.getServerConnectionPort();

            // API 엔드포인트 URL 구성 - HTTP로 변경
            String apiUrl = String.format("http://%s:%d/api/v1/firewall/rules/add", server.getIpAddress(), serverPort);
            log.debug("호출 URL: {}", apiUrl);

            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (server.getApiKey() != null && !server.getApiKey().isEmpty()) {
                headers.set("X-API-Key", server.getApiKey()); // API 키를 헤더에 추가
            }

            // 요청 바디 구성
            FirewallRuleRequest requestBody = createAddPolicyRequest(policyDto);

            // HTTP 요청 엔티티 생성
            HttpEntity<FirewallRuleRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                // API 호출 - 응답 타입을 String으로 받아서 FirewallRuleResponse로 변환
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

                log.debug("API 응답 상태 코드: {}", response.getStatusCode());
                log.debug("API 응답 본문: {}", response.getBody());

                return processApiResponse(response.getBody(), "정책 추가");
            } catch (HttpClientErrorException.NotFound e) {
                // 404 에러 처리
                log.warn("방화벽 API 엔드포인트를 찾을 수 없습니다 (404): {}", apiUrl);
                log.debug("404 응답 본문: {}", e.getResponseBodyAsString());

                return processErrorResponse(e.getResponseBodyAsString(), CODE_RULE_NOT_FOUND,
                        "API 엔드포인트를 찾을 수 없습니다. (404 Not Found) 방화벽 서버가 실행 중인지 확인하세요.");
            } catch (HttpClientErrorException e) {
                // 4xx 에러 처리
                log.warn("방화벽 API 호출 중 클라이언트 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return processErrorResponse(e.getResponseBodyAsString(), CODE_INVALID_REQUEST,
                        "방화벽 API 호출 중 클라이언트 오류가 발생했습니다: " + e.getStatusCode());
            } catch (HttpServerErrorException e) {
                // 5xx 에러 처리
                log.warn("방화벽 API 호출 중 서버 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return processErrorResponse(e.getResponseBodyAsString(), CODE_INTERNAL_ERROR,
                        "방화벽 API 호출 중 서버 오류가 발생했습니다: " + e.getStatusCode());
            }
        } catch (Exception e) {
            handleApiException(e, "방화벽 정책 추가");
            // 예외를 발생시키지 않고 실패 응답으로 처리
            return new FirewallApiResponse(false, CODE_INTERNAL_ERROR,
                    "방화벽 정책 추가 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 방화벽 정책 삭제 API를 호출합니다.
     * API 문서에 맞게 수정됨
     * 
     * @param server   연동서버 정보
     * @param policyId 삭제할 정책 ID
     * @return API 호출 결과
     */
    public FirewallApiResponse deletePolicy(ServerObject server, Long policyId) {
        try {
            log.info("방화벽 정책 삭제 API 호출: 서버={}, 정책 ID={}", server.getName(), policyId);

            // ID로 정책 조회 - 실제 저장소에서 조회
            PolicyDto policyDto = getPolicyById(policyId);
            if (policyDto == null) {
                return new FirewallApiResponse(false, CODE_RULE_NOT_FOUND, "삭제할 정책을 찾을 수 없습니다: " + policyId);
            }

            // 시스템 설정에서 포트 가져오기
            int serverPort = systemSettingService.getServerConnectionPort();

            // API 엔드포인트 URL 구성 - HTTP로 변경
            String apiUrl = String.format("http://%s:%d/api/v1/firewall/rules/delete", server.getIpAddress(), serverPort);
            log.debug("호출 URL: {}", apiUrl);

            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (server.getApiKey() != null && !server.getApiKey().isEmpty()) {
                headers.set("X-API-Key", server.getApiKey()); // API 키를 헤더에 추가
            }

            // 요청 바디 구성 - API 문서에 맞게 수정
            FirewallRuleRequest requestBody = createDeletePolicyRequest(policyDto);

            // HTTP 요청 엔티티 생성
            HttpEntity<FirewallRuleRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                // API 호출
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

                log.debug("API 응답 상태 코드: {}", response.getStatusCode());
                log.debug("API 응답 본문: {}", response.getBody());

                return processApiResponse(response.getBody(), "정책 삭제");
            } catch (HttpClientErrorException.NotFound e) {
                // 404 에러 처리
                log.warn("방화벽 API 엔드포인트를 찾을 수 없습니다 (404): {}", apiUrl);
                log.debug("404 응답 본문: {}", e.getResponseBodyAsString());

                return processErrorResponse(e.getResponseBodyAsString(), CODE_RULE_NOT_FOUND,
                        "API 엔드포인트를 찾을 수 없습니다. (404 Not Found) 방화벽 서버가 실행 중인지 확인하세요.");
            } catch (HttpClientErrorException e) {
                // 4xx 에러 처리
                log.warn("방화벽 API 호출 중 클라이언트 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return processErrorResponse(e.getResponseBodyAsString(), CODE_INVALID_REQUEST,
                        "방화벽 API 호출 중 클라이언트 오류가 발생했습니다: " + e.getStatusCode());
            } catch (HttpServerErrorException e) {
                // 5xx 에러 처리
                log.warn("방화벽 API 호출 중 서버 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                return processErrorResponse(e.getResponseBodyAsString(), CODE_INTERNAL_ERROR,
                        "방화벽 API 호출 중 서버 오류가 발생했습니다: " + e.getStatusCode());
            }
        } catch (Exception e) {
            handleApiException(e, "방화벽 정책 삭제");
            // 예외를 발생시키지 않고 실패 응답으로 처리
            return new FirewallApiResponse(false, CODE_INTERNAL_ERROR,
                    "방화벽 정책 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * API 응답을 처리하고 FirewallApiResponse 객체로 변환합니다.
     * 
     * @param responseBody API 응답 본문
     * @param operation    수행 중인 작업 명칭
     * @return FirewallApiResponse 객체
     */
    private FirewallApiResponse processApiResponse(String responseBody, String operation) {
        if (responseBody == null || responseBody.isEmpty()) {
            log.warn("{} API 응답 본문이 비어있습니다.", operation);
            return new FirewallApiResponse(false, CODE_INTERNAL_ERROR, operation + " 응답이 비어있습니다.");
        }

        try {
            // 전체 FirewallRuleResponse 객체로 파싱 시도
            FirewallRuleResponse apiResponse = objectMapper.readValue(responseBody, FirewallRuleResponse.class);

            if (apiResponse != null) {
                return new FirewallApiResponse(
                        apiResponse.getSuccess() != null && apiResponse.getSuccess(),
                        apiResponse.getCode(),
                        apiResponse.getMessage());
            }

            // 파싱 실패 시 JsonNode로 부분 파싱 시도
            return extractBasicResponseInfo(responseBody, operation);
        } catch (Exception e) {
            log.warn("{} API 응답 파싱 실패: {}", operation, e.getMessage());
            // JsonNode로 부분 파싱 시도
            return extractBasicResponseInfo(responseBody, operation);
        }
    }

    /**
     * API 오류 응답을 처리하고 FirewallApiResponse 객체로 변환합니다.
     * 
     * @param responseBody   에러 응답 본문
     * @param defaultCode    기본 에러 코드
     * @param defaultMessage 기본 에러 메시지
     * @return FirewallApiResponse 객체
     */
    private FirewallApiResponse processErrorResponse(String responseBody, String defaultCode, String defaultMessage) {
        if (responseBody == null || responseBody.isEmpty()) {
            return new FirewallApiResponse(false, defaultCode, defaultMessage);
        }

        try {
            // 응답을 JsonNode로 파싱해서 success, code, message 필드 추출 시도
            JsonNode root = objectMapper.readTree(responseBody);

            Boolean success = root.has("success") ? root.get("success").asBoolean(false) : false;
            String code = root.has("code") ? root.get("code").asText(defaultCode) : defaultCode;
            String message = root.has("message") ? root.get("message").asText(defaultMessage) : defaultMessage;

            return new FirewallApiResponse(success, code, message);
        } catch (Exception e) {
            log.warn("오류 응답 파싱 실패: {}", e.getMessage());
            return new FirewallApiResponse(false, defaultCode, defaultMessage);
        }
    }

    /**
     * API 응답에서 기본 정보(success, code, message)만 추출합니다.
     * 
     * @param responseBody API 응답 본문
     * @param operation    수행 중인 작업 명칭
     * @return FirewallApiResponse 객체
     */
    private FirewallApiResponse extractBasicResponseInfo(String responseBody, String operation) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            Boolean success = false;
            String code = CODE_INTERNAL_ERROR;
            String message = operation + " 응답을 처리할 수 없습니다.";

            if (root.has("success")) {
                success = root.get("success").asBoolean(false);
            }

            if (root.has("code")) {
                code = root.get("code").asText(CODE_INTERNAL_ERROR);
            }

            if (root.has("message")) {
                message = root.get("message").asText(message);
            }

            return new FirewallApiResponse(success, code, message);
        } catch (Exception e) {
            log.error("{} API 응답에서 기본 정보 추출 실패: {}", operation, e.getMessage());
            return new FirewallApiResponse(false, CODE_INTERNAL_ERROR,
                    operation + " API 응답 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * API 응답 문자열을 FirewallRuleResponse 객체로 파싱합니다.
     * 
     * @param responseBody API 응답 본문 (JSON 문자열)
     * @return 파싱된 FirewallRuleResponse 객체 또는 파싱 실패 시 null
     */
    private FirewallRuleResponse parseApiResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, FirewallRuleResponse.class);
        } catch (Exception e) {
            log.error("API 응답 파싱 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * API 예외 처리 메서드
     * 
     * @param e         발생한 예외
     * @param operation 수행 중이던 작업 이름
     */
    private void handleApiException(Exception e, String operation) {
        if (e instanceof ResourceAccessException) {
            log.error("{} API 호출 중 서버 연결 실패 (타임아웃 또는 연결 거부): {}", operation, e.getMessage());
        } else if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            log.error("{} API 호출 중 클라이언트 오류: {} - {}",
                    operation, clientError.getStatusCode(), clientError.getResponseBodyAsString());
        } else if (e instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) e;
            log.error("{} API 호출 중 서버 오류: {} - {}",
                    operation, serverError.getStatusCode(), serverError.getResponseBodyAsString());
        } else {
            log.error("{} API 호출 중 예상치 못한 오류: {}", operation, e.getMessage(), e);
        }
    }

    /**
     * ID로 정책 조회 - 실제 저장소에서 정책 조회하도록 수정
     * 
     * @param policyId 정책 ID
     * @return 정책 DTO 또는 null
     */
    private PolicyDto getPolicyById(Long policyId) {
        try {
            return policyRepository.findById(policyId)
                    .map(policy -> {
                        PolicyDto dto = new PolicyDto();
                        dto.setId(policy.getId());
                        dto.setSourceObjectId(policy.getSourceObjectId());
                        dto.setSourceObjectType(policy.getSourceObjectType());
                        dto.setProtocol(policy.getProtocol());
                        dto.setPortMode(policy.getPortMode());
                        dto.setStartPort(policy.getStartPort());
                        dto.setEndPort(policy.getEndPort());
                        dto.setAction(policy.getAction());
                        return dto;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("정책 조회 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 정책 추가 요청 객체 생성 (API 정의서에 맞게 구현)
     */
    private FirewallRuleRequest createAddPolicyRequest(PolicyDto policyDto) {
        FirewallRuleRequest request = new FirewallRuleRequest();

        // rule.priority 설정 (필드명 변경: rule -> ruleInfo)
        FirewallRuleRequest.RuleInfo ruleInfo = new FirewallRuleRequest.RuleInfo();
        ruleInfo.setPriority(policyDto.getPriority());
        request.setRuleInfo(ruleInfo);

        // rule 설정 (accept/reject)
        request.setRuleType(policyDto.getAction());

        // 나머지 코드는 동일...
        FirewallRuleRequest.IpInfo ipInfo = new FirewallRuleRequest.IpInfo();
        ipInfo.setIpv4Ip(getSourceObjectIpAddress(policyDto.getSourceObjectId(), policyDto.getSourceObjectType()));
        ipInfo.setBit(getSourceObjectNetmaskBit(policyDto.getSourceObjectId(), policyDto.getSourceObjectType()));
        request.setIp(ipInfo);

        FirewallRuleRequest.PortInfo portInfo = new FirewallRuleRequest.PortInfo();
        portInfo.setMode(policyDto.getPortMode());
        if ("single".equals(policyDto.getPortMode())) {
            portInfo.setPort(policyDto.getStartPort());
        } else {
            portInfo.setPort(policyDto.getStartPort() + "-" + policyDto.getEndPort());
        }
        request.setPort(portInfo);

        request.setProtocol(policyDto.getProtocol());
        request.setLog(policyDto.getLogging());

        boolean useTimeout = policyDto.getTimeLimit() != null && policyDto.getTimeLimit() > 0;
        request.setUseTimeout(useTimeout);
        if (useTimeout) {
            request.setTimeout(policyDto.getTimeLimit() * 3600);
        }

        request.setDescription(policyDto.getDescription());

        return request;
    }

    /**
     * 정책 삭제 요청 객체 생성 - API 문서에 맞게 수정
     * 
     * @param policyDto 삭제할 정책 정보
     * @return 삭제 요청 객체
     */
    private FirewallRuleRequest createDeletePolicyRequest(PolicyDto policyDto) {
        FirewallRuleRequest request = new FirewallRuleRequest();

        // ip 정보
        FirewallRuleRequest.IpInfo ipInfo = new FirewallRuleRequest.IpInfo();
        ipInfo.setIpv4Ip(getSourceObjectIpAddress(policyDto.getSourceObjectId(), policyDto.getSourceObjectType()));
        ipInfo.setBit(getSourceObjectNetmaskBit(policyDto.getSourceObjectId(), policyDto.getSourceObjectType()));
        request.setIp(ipInfo);

        // port 정보
        FirewallRuleRequest.PortInfo portInfo = new FirewallRuleRequest.PortInfo();
        portInfo.setMode(policyDto.getPortMode());
        if ("single".equals(policyDto.getPortMode())) {
            portInfo.setPort(policyDto.getStartPort());
        } else {
            portInfo.setPort(policyDto.getStartPort() + "-" + policyDto.getEndPort());
        }
        request.setPort(portInfo);

        // protocol
        request.setProtocol(policyDto.getProtocol());

        // rule 설정 (accept/reject)
        request.setRuleType(policyDto.getAction());

        return request;
    }

    /**
     * 출발지 객체 ID와 타입으로 IP 주소 조회
     */
    private String getSourceObjectIpAddress(Long objectId, String objectType) {
        if (objectId == null || objectType == null) {
            log.warn("출발지 객체 정보 누락: objectId={}, objectType={}", objectId, objectType);
            return "0.0.0.0"; // 기본값
        }

        try {
            switch (objectType) {
                case "SERVER":
                    return serverObjectRepository.findById(objectId)
                            .map(ServerObject::getIpAddress)
                            .orElse("0.0.0.0");
                case "GENERAL":
                    return generalObjectRepository.findById(objectId)
                            .map(GeneralObject::getIpAddress)
                            .orElse("0.0.0.0");
                case "NETWORK":
                    // 네트워크 객체의 경우 CIDR 형식(예: 192.168.1.0/24)일 수 있음
                    // IP 부분만 추출해서 반환하거나, 필요에 따라 처리
                    String ipWithMask = networkObjectRepository.findById(objectId)
                            .map(NetworkObject::getIpAddress)
                            .orElse("0.0.0.0/32");

                    // CIDR 형식에서 IP 부분만 추출
                    return ipWithMask.split("/")[0];
                default:
                    log.warn("알 수 없는 객체 타입: {}", objectType);
                    return "0.0.0.0";
            }
        } catch (Exception e) {
            log.error("객체 IP 주소 조회 중 오류 발생: {}", e.getMessage(), e);
            return "0.0.0.0";
        }
    }

    /**
     * 출발지 객체 ID와 타입으로 넷마스크 비트 조회
     */
    private int getSourceObjectNetmaskBit(Long objectId, String objectType) {
        if (objectId == null || objectType == null) {
            return 32; // 기본값
        }

        try {
            switch (objectType) {
                case "SERVER":
                case "GENERAL":
                    // 일반적으로 단일 IP는 32비트 마스크
                    return 32;
                case "NETWORK":
                    // 네트워크 객체의 경우 CIDR 형식(예: 192.168.1.0/24)일 수 있음
                    // 마스크 부분 추출
                    String ipWithMask = networkObjectRepository.findById(objectId)
                            .map(NetworkObject::getIpAddress)
                            .orElse("0.0.0.0/32");

                    // CIDR 형식에서 비트 부분 추출
                    String[] parts = ipWithMask.split("/");
                    if (parts.length > 1) {
                        try {
                            int bit = Integer.parseInt(parts[1]);
                            // API 정의서에 따라 8, 16, 24, 32 중 하나로 제한
                            if (bit <= 8)
                                return 8;
                            else if (bit <= 16)
                                return 16;
                            else if (bit <= 24)
                                return 24;
                            else
                                return 32;
                        } catch (NumberFormatException e) {
                            log.warn("잘못된 넷마스크 비트: {}", parts[1]);
                            return 32;
                        }
                    } else {
                        return 32;
                    }
                default:
                    return 32;
            }
        } catch (Exception e) {
            log.error("객체 넷마스크 비트 조회 중 오류 발생: {}", e.getMessage(), e);
            return 32;
        }
    }

    /**
     * 방화벽 API 응답 결과를 담는 클래스
     * code 필드 추가하여 API 응답 코드 표시
     */
    public static class FirewallApiResponse {
        private final boolean success;
        private final String code;
        private final String message;

        public FirewallApiResponse(boolean success, String code, String message) {
            this.success = success;
            this.code = code != null ? code : CODE_INTERNAL_ERROR;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}