package com.nicednb.svrgate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirewallRuleRequest {
    private RuleInfo rule;     // rule.priority 정보를 담는 객체
    private IpInfo ip;
    private PortInfo port;
    private String protocol;
    private Boolean log;
    
    // JSON 필드명 변경
    @JsonProperty("use_timeout")
    private Boolean useTimeout;
    
    private Integer timeout;
    private String description;
    private String action;     // 'accept' 또는 'reject' 값
    
    @Getter
    @Setter
    public static class RuleInfo {
        private Integer priority;
    }
    
    @Getter
    @Setter
    public static class IpInfo {
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