package com.nicednb.svrgate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirewallRuleRequest {
    // 기존 중복된 'rule' 필드를 'rulePriority'와 'ruleAction'으로 분리
    private RulePriority rulePriority;  // rule.priority 정보를 담는 객체
    private String ruleAction;          // accept/reject 값
    private FirewallIp ip;
    private FirewallPort port;
    private String protocol;
    private Boolean log;
    private Boolean useTimeout;
    private Integer timeout;
    private String description;
    
    @Getter
    @Setter
    public static class RulePriority {
        private Integer priority;
    }
    
    @Getter
    @Setter
    public static class FirewallIp {
        private String ipv4Ip;
        private Integer bit;
    }
    
    @Getter
    @Setter
    public static class FirewallPort {
        private String mode;
        private String port;  // "123" 또는 "123-456" 형식
    }
}