package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.FirewallRuleRequest;
import com.nicednb.svrgate.dto.FirewallRuleResponse;
import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.entity.GeneralObject;
import com.nicednb.svrgate.entity.NetworkObject;
import com.nicednb.svrgate.entity.ServerObject;
import com.nicednb.svrgate.repository.GeneralObjectRepository;
import com.nicednb.svrgate.repository.NetworkObjectRepository;
import com.nicednb.svrgate.repository.ServerObjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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
    private final RetryTemplate retryTemplate;
    private final ServerObjectRepository serverObjectRepository;
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;

    /**
     * 방화벽 정책 추가 API를 호출합니다.
     * 
     * @param server 연동서버 정보
     * @param policyDto 추가할 정책 정보
     * @return API 호출 결과
     * @throws FirewallApiException API 호출 중 오류 발생 시
     */
    public FirewallApiResponse addPolicy(ServerObject server, PolicyDto policyDto) throws FirewallApiException {
        try {
            return retryTemplate.execute(context -> {
                log.info("방화벽 정책 추가 API 호출: 서버={}, 정책 우선순위={}", server.getName(), policyDto.getPriority());
                
                // API 엔드포인트 URL 구성 - API 정의서에 맞게 수정
                String apiUrl = String.format("https://%s/api/v1/firewall/rules", server.getIpAddress());
                
                // 요청 헤더 구성
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-API-Key", server.getApiKey()); // API 키를 헤더에 추가
                
                // 요청 바디 구성 - API 정의서에 맞게 변경
                FirewallRuleRequest requestBody = createAddPolicyRequest(policyDto);
                
                // HTTP 요청 엔티티 생성
                HttpEntity<FirewallRuleRequest> requestEntity = new HttpEntity<>(requestBody, headers);
                
                // API 호출
                ResponseEntity<FirewallRuleResponse> response = restTemplate.exchange(
                    apiUrl, 
                    HttpMethod.POST, 
                    requestEntity, 
                    FirewallRuleResponse.class
                );
                
                // 응답 처리 - API 정의서에 맞게 수정
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    FirewallRuleResponse responseBody = response.getBody();
                    boolean success = responseBody.getSuccess();
                    String code = responseBody.getCode();
                    String message = responseBody.getMessage();
                    
                    log.info("방화벽 정책 추가 API 응답: 성공={}, 코드={}, 메시지={}", success, code, message);
                    
                    if (success && policyDto.getTimeLimit() != null && policyDto.getTimeLimit() > 0 
                            && responseBody.getData() != null && responseBody.getData().getExpiresAt() != null) {
                        log.info("정책 만료 시간: {}", responseBody.getData().getExpiresAt());
                    }
                    
                    return new FirewallApiResponse(success, message);
                } else {
                    log.warn("방화벽 정책 추가 API 응답 실패: 상태 코드={}", response.getStatusCode());
                    return new FirewallApiResponse(false, "방화벽 정책 추가에 실패했습니다. 상태 코드: " + response.getStatusCode());
                }
            });
        } catch (Exception e) {
            handleApiException(e, "방화벽 정책 추가");
            throw new FirewallApiException("방화벽 정책 추가 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 방화벽 정책 삭제 API를 호출합니다.
     * 
     * @param server 연동서버 정보
     * @param policyId 삭제할 정책 ID
     * @return API 호출 결과
     * @throws FirewallApiException API 호출 중 오류 발생 시
     */
    public FirewallApiResponse deletePolicy(ServerObject server, Long policyId) throws FirewallApiException {
        try {
            return retryTemplate.execute(context -> {
                log.info("방화벽 정책 삭제 API 호출: 서버={}, 정책 ID={}", server.getName(), policyId);
                
                // ID로 정책 조회 - 실제로는 이 부분이 구현되어야 함
                PolicyDto policyDto = getPolicyById(policyId);
                
                // API 엔드포인트 URL 구성
                String apiUrl = String.format("https://%s/api/v1/firewall/rules", server.getIpAddress());
                
                // 요청 헤더 구성
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-API-Key", server.getApiKey()); // API 키를 헤더에 추가
                
                // 요청 바디 구성 - API 정의서에 맞게 변경
                FirewallRuleRequest requestBody = createDeletePolicyRequest(policyDto);
                
                // HTTP 요청 엔티티 생성
                HttpEntity<FirewallRuleRequest> requestEntity = new HttpEntity<>(requestBody, headers);
                
                // API 호출
                ResponseEntity<FirewallRuleResponse> response = restTemplate.exchange(
                    apiUrl, 
                    HttpMethod.DELETE, 
                    requestEntity, 
                    FirewallRuleResponse.class
                );
                
                // 응답 처리 - API 정의서에 맞게 수정
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    FirewallRuleResponse responseBody = response.getBody();
                    boolean success = responseBody.getSuccess();
                    String code = responseBody.getCode();
                    String message = responseBody.getMessage();
                    
                    log.info("방화벽 정책 삭제 API 응답: 성공={}, 코드={}, 메시지={}", success, code, message);
                    if (success && responseBody.getData() != null && responseBody.getData().getDeletedAt() != null) {
                        log.info("정책 삭제 시간: {}", responseBody.getData().getDeletedAt());
                    }
                    
                    return new FirewallApiResponse(success, message);
                } else {
                    log.warn("방화벽 정책 삭제 API 응답 실패: 상태 코드={}", response.getStatusCode());
                    return new FirewallApiResponse(false, "방화벽 정책 삭제에 실패했습니다. 상태 코드: " + response.getStatusCode());
                }
            });
        } catch (Exception e) {
            handleApiException(e, "방화벽 정책 삭제");
            throw new FirewallApiException("방화벽 정책 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * API 예외 처리 메서드
     * 
     * @param e 발생한 예외
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
            log.error("{} API 호출 중 예상치 못한 오류: {}", operation, e.getMessage());
        }
    }
    
    /**
     * ID로 정책 조회 - 삭제를 위한 정책 정보 조회
     * 실제 구현 필요 - 현재는 임시 구현
     */
    private PolicyDto getPolicyById(Long policyId) {
        // TODO: 정책 저장소에서 ID로 정책 조회
        // 임시 구현: 삭제 시 필요한 최소 정보만 설정
        PolicyDto policyDto = new PolicyDto();
        policyDto.setId(policyId);
        policyDto.setSourceObjectId(1L); // 임시값
        policyDto.setSourceObjectType("SERVER"); // 임시값
        policyDto.setProtocol("tcp"); // 임시값
        policyDto.setPortMode("single"); // 임시값
        policyDto.setStartPort(80); // 임시값
        policyDto.setAction("accept"); // 임시값
        return policyDto;
    }
    
    /**
     * 정책 추가 요청 객체 생성 (API 정의서에 맞게 구현)
     */
    private FirewallRuleRequest createAddPolicyRequest(PolicyDto policyDto) {
        FirewallRuleRequest request = new FirewallRuleRequest();
        
        // rule.priority
        FirewallRuleRequest.RuleInfo ruleInfo = new FirewallRuleRequest.RuleInfo();
        ruleInfo.setPriority(policyDto.getPriority());
        request.setRule(ruleInfo);
        
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
        
        // rule (action)
        request.setRule(policyDto.getAction());
        
        // log
        request.setLog(policyDto.getLogging());
        
        // timeout 관련
        boolean useTimeout = policyDto.getTimeLimit() != null && policyDto.getTimeLimit() > 0;
        request.setUseTimeout(useTimeout);
        if (useTimeout) {
            // timeLimit은 시간(h) 단위, API는 초(s) 단위로 가정
            request.setTimeout(policyDto.getTimeLimit() * 3600);
        }
        
        // description
        request.setDescription(policyDto.getDescription());
        
        return request;
    }
    
    /**
     * 정책 삭제 요청 객체 생성 (API 정의서에 맞게 구현)
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
        
        // rule (action)
        request.setRule(policyDto.getAction());
        
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
                            if (bit <= 8) return 8;
                            else if (bit <= 16) return 16;
                            else if (bit <= 24) return 24;
                            else return 32;
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
     */
    public static class FirewallApiResponse {
        private final boolean success;
        private final String message;

        public FirewallApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 방화벽 API 호출 중 발생하는 예외
     */
    public static class FirewallApiException extends Exception {
        public FirewallApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}