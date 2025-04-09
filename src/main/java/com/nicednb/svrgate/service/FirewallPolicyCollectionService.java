package com.nicednb.svrgate.service;

import com.nicednb.svrgate.dto.FirewallRulesResponse;
import com.nicednb.svrgate.entity.*;
import com.nicednb.svrgate.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 방화벽 정책 수집 서비스
 * 연동서버 객체의 연동 여부가 연동인 서버에서 방화벽 정책을 수집하고 DB를 갱신합니다.
 */
@Service
@RequiredArgsConstructor
public class FirewallPolicyCollectionService {
    
    private final Logger log = LoggerFactory.getLogger(FirewallPolicyCollectionService.class);
    private final ServerObjectRepository serverObjectRepository;
    private final PolicyRepository policyRepository;
    private final SystemSettingService systemSettingService;
    private final OperationLogService operationLogService;
    private final RestTemplate restTemplate;
    // 추가: GeneralObject와 NetworkObject 리포지토리 주입
    private final GeneralObjectRepository generalObjectRepository;
    private final NetworkObjectRepository networkObjectRepository;
    
    // 서버별 작업 실행 상태를 관리하는 맵
    private final Map<Long, Boolean> serverProcessingFlags = new ConcurrentHashMap<>();
    
    /**
     * 모든 활성화된 서버에서 방화벽 정책을 수집합니다.
     * 시스템 설정의 동시 수집 서버 수를 참고하여 병렬 처리합니다.
     */
    public void collectPoliciesFromAllServers() {
        log.info("모든 서버에서 방화벽 정책 수집 시작");
        
        // 활성화된 서버 목록 조회
        List<ServerObject> activeServers = serverObjectRepository.findByActiveTrue();
        
        if (activeServers.isEmpty()) {
            log.info("활성화된 서버가 없습니다. 정책 수집을 건너뜁니다.");
            return;
        }
        
        // 동시 수집 서버 수 설정 조회
        int concurrentServers = systemSettingService.getIntegerValue(
                SystemSettingService.KEY_CONCURRENT_SERVERS, 10);
        
        // 서버 연동 포트 조회
        int serverPort = systemSettingService.getServerConnectionPort();
        
        log.info("방화벽 정책 수집: 총 {}개 서버, 동시 수집 서버 수: {}", activeServers.size(), concurrentServers);
        
        // 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(concurrentServers);
        
        // 각 서버에 대해 정책 수집 작업 제출
        for (ServerObject server : activeServers) {
            // 이미 처리 중인 서버는 건너뜀
            if (serverProcessingFlags.getOrDefault(server.getId(), false)) {
                log.info("서버 {}({})는 이미 처리 중입니다. 건너뜁니다.", server.getName(), server.getId());
                continue;
            }
            
            executor.submit(() -> {
                try {
                    // 처리 중 플래그 설정
                    serverProcessingFlags.put(server.getId(), true);
                    
                    // 정책 수집 실행
                    collectPoliciesFromServer(server, serverPort);
                } catch (Exception e) {
                    log.error("서버 {}에서 정책 수집 중 오류 발생: {}", server.getName(), e.getMessage(), e);
                    
                } finally {
                    // 처리 완료 플래그 해제
                    serverProcessingFlags.put(server.getId(), false);
                }
            });
        }
        
        // 모든 작업이 완료될 때까지 대기 후 종료
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                log.warn("일부 정책 수집 작업이 30분 내에 완료되지 않았습니다.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("정책 수집 작업 대기 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        }
        
        log.info("모든 서버에서 방화벽 정책 수집 완료");
    }
    
    /**
     * 특정 서버에서 방화벽 정책을 수집하고 DB를 업데이트합니다.
     * 
     * @param server 방화벽 정책을 수집할 서버
     * @param serverPort 서버 연동 포트
     */
    @Transactional
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void collectPoliciesFromServer(ServerObject server, int serverPort) {
        log.info("서버 {}({})에서 방화벽 정책 수집 시작", server.getName(), server.getId());
        
        try {
            // API 엔드포인트 URL 구성 - 페이징 파라미터 제거
            String apiUrl = String.format("http://%s:%d/api/v1/firewall/rules", 
                    server.getIpAddress(), serverPort);
            
            // 요청 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            if (server.getApiKey() != null && !server.getApiKey().isEmpty()) {
                headers.set("X-API-Key", server.getApiKey());
            }
            
            // HTTP 요청 엔티티 생성
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<FirewallRulesResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    requestEntity,
                    FirewallRulesResponse.class);
            
            FirewallRulesResponse rulesResponse = response.getBody();
            
            if (rulesResponse == null || !rulesResponse.getSuccess()) {
                log.error("서버 {}에서 방화벽 정책 조회 실패: {}", 
                        server.getName(), 
                        rulesResponse != null ? rulesResponse.getMessage() : "응답이 null");
                return;
            }
            
            List<FirewallRulesResponse.FirewallRule> rules = 
                    rulesResponse.getData() != null ? rulesResponse.getData().getRules() : new ArrayList<>();
            
            log.info("서버 {}에서 {}개의 방화벽 정책을 조회했습니다.", server.getName(), rules.size());
            
            // 시간제한이 무제한인 기존 정책 조회
            List<Policy> unlimitedPolicies = policyRepository.findByServerObjectIdAndTimeLimitIsNull(server.getId());
            
            // 정책 비교 및 갱신
            updatePolicies(server, rules, unlimitedPolicies);
            
            // 작업 로그 기록 (성공)
            operationLogService.logOperation(
                    "SYSTEM",
                    "127.0.0.1",
                    true,
                    "서버: " + server.getName() + ", 정책 수: " + rules.size(),
                    "정책관리",
                    "서버 정책 수집 성공"
            );
            
            log.info("서버 {}에서 방화벽 정책 수집 완료", server.getName());
        } catch (Exception e) {
            log.error("서버 {}에서 방화벽 정책 수집 중 오류 발생: {}", server.getName(), e.getMessage(), e);
            throw e;
        }
    }
     /**
     * 출발지 IP와 bit를 기반으로 객체를 찾거나 생성합니다.
     * 
     * @param ipAddress IP 주소
     * @param bit 넷마스크 비트
     * @return 출발지 객체 정보 (ID와 타입)
     */
    private Map<String, Object> findOrCreateSourceObject(String ipAddress, Integer bit) {
        Map<String, Object> result = new HashMap<>();
        
        // 먼저 기존 객체 찾기
        // 1. 서버 객체 확인
        Optional<ServerObject> serverObject = serverObjectRepository.findByIpAddress(ipAddress);
        if (serverObject.isPresent()) {
            result.put("id", serverObject.get().getId());
            result.put("type", "SERVER");
            return result;
        }
        
        // bit에 따라 다른 처리
        if (bit == 32) {
            // 단일 IP (32bit)는 일반 객체
            Optional<GeneralObject> generalObject = generalObjectRepository.findByIpAddress(ipAddress);
            if (generalObject.isPresent()) {
                result.put("id", generalObject.get().getId());
                result.put("type", "GENERAL");
                return result;
            }
            
            // 없으면 자동 생성
            GeneralObject newGeneralObject = new GeneralObject();
            newGeneralObject.setName(ipAddress); // IP를 이름으로
            newGeneralObject.setIpAddress(ipAddress);
            newGeneralObject.setDescription("방화벽 정책 수집 중 자동 등록된 객체");
            // Zone은 null로 둠 (관리자가 나중에 설정)
            
            GeneralObject savedObject = generalObjectRepository.save(newGeneralObject);
            log.info("일반 객체 자동 생성: IP={}, ID={}", ipAddress, savedObject.getId());
            
            // 작업 로그 기록
            operationLogService.logOperation(
                    "SYSTEM",
                    "127.0.0.1",
                    true,
                    "IP: " + ipAddress,
                    "객체관리",
                    "정책 수집 중 일반 객체 자동 생성"
            );
            
            result.put("id", savedObject.getId());
            result.put("type", "GENERAL");
        } else {
            // 서브넷 (bit < 32)은 네트워크 객체
            String cidrIp = ipAddress + "/" + bit;
            Optional<NetworkObject> networkObject = networkObjectRepository.findByIpAddress(cidrIp);
            if (networkObject.isPresent()) {
                result.put("id", networkObject.get().getId());
                result.put("type", "NETWORK");
                return result;
            }
            
            // 없으면 자동 생성
            NetworkObject newNetworkObject = new NetworkObject();
            newNetworkObject.setName(cidrIp); // CIDR을 이름으로
            newNetworkObject.setIpAddress(cidrIp);
            newNetworkObject.setDescription("방화벽 정책 수집 중 자동 등록된 객체");
            // Zone은 빈 Set으로 초기화 (관리자가 나중에 설정)
            newNetworkObject.setZones(new HashSet<>());
            
            NetworkObject savedObject = networkObjectRepository.save(newNetworkObject);
            log.info("네트워크 객체 자동 생성: IP={}, ID={}", cidrIp, savedObject.getId());
            
            // 작업 로그 기록
            operationLogService.logOperation(
                    "SYSTEM",
                    "127.0.0.1",
                    true,
                    "IP: " + cidrIp,
                    "객체관리",
                    "정책 수집 중 네트워크 객체 자동 생성"
            );
            
            result.put("id", savedObject.getId());
            result.put("type", "NETWORK");
        }
        
        return result;
    }

    /**
     * 조회한 방화벽 정책과 DB의 정책을 비교하여 갱신합니다.
     * 
     * @param server 서버 객체
     * @param firewallRules 방화벽 정책 목록
     * @param dbPolicies DB에 저장된 정책 목록
     */
    private void updatePolicies(ServerObject server, 
                               List<FirewallRulesResponse.FirewallRule> firewallRules, 
                               List<Policy> dbPolicies) {
        log.info("서버 {}의 방화벽 정책 비교 및 갱신 시작", server.getName());
        
        // DB에 있는 정책 중 방화벽에 없는 정책 식별
        List<Policy> policiesToDelete = new ArrayList<>(dbPolicies);
        
        // 방화벽 정책 순회
        for (FirewallRulesResponse.FirewallRule rule : firewallRules) {
            // 출발지 IP와 bit
            String ipv4Ip = rule.getIp().getIpv4Ip();
            Integer bit = rule.getIp().getBit();
            
            // 포트 정보
            String portMode = rule.getPort().getMode();
            Integer startPort = null;
            Integer endPort = null;
            
            if ("single".equals(portMode)) {
                // 단일 포트
                if (rule.getPort().getPort() instanceof Integer) {
                    startPort = (Integer) rule.getPort().getPort();
                    endPort = startPort;
                } else if (rule.getPort().getPort() instanceof String) {
                    try {
                        startPort = Integer.parseInt((String) rule.getPort().getPort());
                        endPort = startPort;
                    } catch (NumberFormatException e) {
                        log.warn("포트 변환 실패: {}", rule.getPort().getPort());
                        continue;
                    }
                }
            } else if ("multi".equals(portMode)) {
                // 포트 범위
                if (rule.getPort().getPort() instanceof String) {
                    String portRange = (String) rule.getPort().getPort();
                    String[] parts = portRange.split("-");
                    if (parts.length == 2) {
                        try {
                            startPort = Integer.parseInt(parts[0]);
                            endPort = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            log.warn("포트 범위 변환 실패: {}", portRange);
                            continue;
                        }
                    }
                }
            }
            
            if (startPort == null || endPort == null) {
                log.warn("포트 정보 누락: {}", rule.getPort());
                continue;
            }
            
            // 프로토콜과 액션
            String protocol = rule.getProtocol();
            String action = rule.getRule();
  // 출발지 객체 찾거나 생성
            Map<String, Object> sourceObject = findOrCreateSourceObject(ipv4Ip, bit);
            Long sourceObjectId = (Long) sourceObject.get("id");
            String sourceObjectType = (String) sourceObject.get("type");
            
            // 일치하는 DB 정책 찾기 - sourceObjectId와 sourceObjectType으로 조회
            Policy matchingPolicy = null;
            for (Policy dbPolicy : dbPolicies) {
                if (dbPolicy.getSourceObjectId().equals(sourceObjectId) &&
                    dbPolicy.getSourceObjectType().equals(sourceObjectType) &&
                    dbPolicy.getProtocol().equals(protocol) &&
                    dbPolicy.getPortMode().equals(portMode) &&
                    dbPolicy.getStartPort().equals(startPort) &&
                    (dbPolicy.getEndPort() == null && endPort == null || 
                     dbPolicy.getEndPort() != null && dbPolicy.getEndPort().equals(endPort)) &&
                    dbPolicy.getAction().equals(action)) {
                    
                    matchingPolicy = dbPolicy;
                    break;
                }
            }
            
            if (matchingPolicy != null) {
                // 정책이 존재하면 삭제 목록에서 제거
                policiesToDelete.remove(matchingPolicy);
                
                // 우선순위 업데이트 (필요한 경우)
                if (!matchingPolicy.getPriority().equals(rule.getPriority())) {
                    log.info("서버 {}의 정책 ID {} 우선순위 업데이트: {} -> {}", 
                            server.getName(), matchingPolicy.getId(), 
                            matchingPolicy.getPriority(), rule.getPriority());
                    matchingPolicy.setPriority(rule.getPriority());
                    
                    // sourceObjectIp, sourceObjectBit 업데이트 (혹시 없는 경우)
                    if (matchingPolicy.getSourceObjectIp() == null || matchingPolicy.getSourceObjectBit() == null) {
                        matchingPolicy.setSourceObjectIp(ipv4Ip);
                        matchingPolicy.setSourceObjectBit(bit);
                    }
                    
                    policyRepository.save(matchingPolicy);
                }
            } else {
                // 정책이 존재하지 않으면 새로 생성
                Policy newPolicy = createPolicyFromFirewallRule(server, rule);
                log.info("서버 {}에 새 정책 추가: 우선순위={}, 출발지={}({}/{}) ID={}, 프로토콜={}", 
                        server.getName(), newPolicy.getPriority(), 
                        sourceObjectType, ipv4Ip, bit, 
                        sourceObjectId, newPolicy.getProtocol());
                
                // 새 정책 저장
                policyRepository.save(newPolicy);
            }
        }
        
        // 방화벽에 없는 정책 삭제
        if (!policiesToDelete.isEmpty()) {
            log.info("서버 {}에서 {}개의 정책 삭제", server.getName(), policiesToDelete.size());
            policyRepository.deleteAll(policiesToDelete);
        }
        
        log.info("서버 {}의 방화벽 정책 비교 및 갱신 완료", server.getName());
    }
    
    /**
     * 방화벽 정책으로부터 DB 정책 엔티티를 생성합니다.
     * 
     * @param server 서버 객체
     * @param rule 방화벽 정책
     * @return 생성된 정책 엔티티
     */
    private Policy createPolicyFromFirewallRule(ServerObject server, FirewallRulesResponse.FirewallRule rule) {
        Policy policy = new Policy();
        
        // 서버 설정
        policy.setServerObject(server);
        
        // 우선순위
        policy.setPriority(rule.getPriority());
        
        // 출발지 정보 - 단순 IP 정보 설정
        policy.setSourceObjectIp(rule.getIp().getIpv4Ip());
        policy.setSourceObjectBit(rule.getIp().getBit());
        policy.setSourceObjectType("DIRECT_IP");  // 직접 IP 방식으로 설정
        policy.setSourceObjectId(0L);  // 직접 IP 방식이므로 0으로 설정
        
        // 프로토콜
        policy.setProtocol(rule.getProtocol());
        
        // 포트 정보
        policy.setPortMode(rule.getPort().getMode());
        
        if ("single".equals(rule.getPort().getMode())) {
            // 단일 포트
            if (rule.getPort().getPort() instanceof Integer) {
                policy.setStartPort((Integer) rule.getPort().getPort());
                policy.setEndPort((Integer) rule.getPort().getPort());
            } else if (rule.getPort().getPort() instanceof String) {
                try {
                    int port = Integer.parseInt((String) rule.getPort().getPort());
                    policy.setStartPort(port);
                    policy.setEndPort(port);
                } catch (NumberFormatException e) {
                    log.warn("포트 변환 실패: {}", rule.getPort().getPort());
                }
            }
        } else if ("multi".equals(rule.getPort().getMode())) {
            // 포트 범위
            if (rule.getPort().getPort() instanceof String) {
                String portRange = (String) rule.getPort().getPort();
                String[] parts = portRange.split("-");
                if (parts.length == 2) {
                    try {
                        policy.setStartPort(Integer.parseInt(parts[0]));
                        policy.setEndPort(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        log.warn("포트 범위 변환 실패: {}", portRange);
                    }
                }
            }
        }
        
        // 동작 (accept/reject)
        policy.setAction(rule.getRule());
        
        // 기본값 설정
        policy.setLogging(true);
        policy.setTimeLimit(null);  // 무제한
        policy.setRegistrationDate(LocalDateTime.now());
        policy.setRegistrar("SYSTEM");  // 시스템에 의한 자동 등록
        policy.setDescription("방화벽에서 자동 수집된 정책");
        
        return policy;
    }
}