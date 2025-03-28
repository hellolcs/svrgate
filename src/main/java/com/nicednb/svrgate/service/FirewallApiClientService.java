package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.entity.ServerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 방화벽 API를 호출하기 위한 클라이언트 서비스
 * 연동서버와의 통신을 담당합니다.
 */
@Service
public class FirewallApiClientService {

    private final Logger log = LoggerFactory.getLogger(FirewallApiClientService.class);
    private final RestTemplate restTemplate;

    public FirewallApiClientService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 방화벽 정책 추가 API를 호출합니다.
     * 
     * @param server 연동서버 정보
     * @param policyDto 추가할 정책 정보
     * @return API 호출 결과 (성공: true, 실패: false)
     * @throws FirewallApiException API 호출 중 오류 발생 시
     */
    public FirewallApiResponse addPolicy(ServerObject server, PolicyDto policyDto) throws FirewallApiException {
        try {
            log.info("방화벽 정책 추가 API 호출: 서버={}, 정책 우선순위={}", server.getName(), policyDto.getPriority());
            
            // API 엔드포인트 URL 구성
            String apiUrl = String.format("http://%s/api/firewall/policy", server.getIpAddress());
            
            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Api-Key", server.getApiKey()); // API 키를 헤더에 추가
            
            // 요청 바디 구성
            Map<String, Object> requestBody = createPolicyRequestBody(policyDto);
            
            // HTTP 요청 엔티티 생성
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            
            // 응답 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                boolean success = Boolean.TRUE.equals(response.getBody().get("success"));
                String message = response.getBody().get("message") != null 
                    ? response.getBody().get("message").toString() 
                    : "방화벽 정책이 성공적으로 추가되었습니다.";
                
                log.info("방화벽 정책 추가 API 응답: 성공={}, 메시지={}", success, message);
                return new FirewallApiResponse(success, message);
            } else {
                log.warn("방화벽 정책 추가 API 응답 실패: 상태 코드={}", response.getStatusCode());
                return new FirewallApiResponse(false, "방화벽 정책 추가에 실패했습니다. 상태 코드: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("방화벽 정책 추가 API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new FirewallApiException("방화벽 정책 추가 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 방화벽 정책 삭제 API를 호출합니다.
     * 
     * @param server 연동서버 정보
     * @param policyId 삭제할 정책 ID
     * @return API 호출 결과 (성공: true, 실패: false)
     * @throws FirewallApiException API 호출 중 오류 발생 시
     */
    public FirewallApiResponse deletePolicy(ServerObject server, Long policyId) throws FirewallApiException {
        try {
            log.info("방화벽 정책 삭제 API 호출: 서버={}, 정책 ID={}", server.getName(), policyId);
            
            // API 엔드포인트 URL 구성
            String apiUrl = String.format("http://%s/api/firewall/policy/%d", server.getIpAddress(), policyId);
            
            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", server.getApiKey()); // API 키를 헤더에 추가
            
            // HTTP 요청 엔티티 생성
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.DELETE, 
                requestEntity, 
                Map.class
            );
            
            // 응답 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                boolean success = Boolean.TRUE.equals(response.getBody().get("success"));
                String message = response.getBody().get("message") != null 
                    ? response.getBody().get("message").toString() 
                    : "방화벽 정책이 성공적으로 삭제되었습니다.";
                
                log.info("방화벽 정책 삭제 API 응답: 성공={}, 메시지={}", success, message);
                return new FirewallApiResponse(success, message);
            } else {
                log.warn("방화벽 정책 삭제 API 응답 실패: 상태 코드={}", response.getStatusCode());
                return new FirewallApiResponse(false, "방화벽 정책 삭제에 실패했습니다. 상태 코드: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("방화벽 정책 삭제 API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new FirewallApiException("방화벽 정책 삭제 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 정책 DTO를 API 요청 바디로 변환합니다.
     */
    private Map<String, Object> createPolicyRequestBody(PolicyDto policyDto) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("priority", policyDto.getPriority());
        requestBody.put("sourceObjectId", policyDto.getSourceObjectId());
        requestBody.put("sourceObjectType", policyDto.getSourceObjectType());
        requestBody.put("protocol", policyDto.getProtocol());
        requestBody.put("portMode", policyDto.getPortMode());
        requestBody.put("startPort", policyDto.getStartPort());
        requestBody.put("endPort", policyDto.getEndPort());
        requestBody.put("action", policyDto.getAction());
        requestBody.put("logging", policyDto.getLogging());
        requestBody.put("timeLimit", policyDto.getTimeLimit());
        requestBody.put("requester", policyDto.getRequester());
        requestBody.put("description", policyDto.getDescription());
        return requestBody;
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