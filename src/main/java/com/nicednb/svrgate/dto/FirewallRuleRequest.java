package com.nicednb.svrgate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 방화벽 정책 API 요청 DTO
 * 방화벽 관리 Agent API 정의서에 맞게 정의됨
 */
@Getter
@Setter
public class FirewallRuleRequest {
    // 필드명 변경: rule -> ruleInfo (JSON 변환 시 이름 변경하지 않음)
    private RuleInfo ruleInfo;
    private IpInfo ip;
    private PortInfo port;
    private String protocol;
    private Boolean log;
    
    @JsonProperty("use_timeout")
    private Boolean useTimeout;
    
    private Integer timeout;
    private String description;
    
    // JSON 변환 시 "rule"로 출력
    @JsonProperty("rule")
    private String ruleType;     // 'accept' 또는 'reject' 값
    
    @Getter
    @Setter
    public static class RuleInfo {
        private Integer priority;
    }
    
    @Getter
    @Setter
    public static class IpInfo {
        @JsonProperty("ipv4_ip")
        private String ipv4Ip;
        private Integer bit;
    }
    
    @Getter
    @Setter
    public static class PortInfo {
        private String mode;
        private Object port;  // 단일 포트 또는 포트 범위 문자열
    }
}