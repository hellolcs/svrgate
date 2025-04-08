package com.nicednb.svrgate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FirewallRulesResponse {
    private Boolean success;
    private String code;
    private String message;
    private ResponseData data;
    
    @Data
    public static class ResponseData {
        private Integer total;
        private Integer page;
        private Integer size;
        private List<FirewallRule> rules;
    }
    
    @Data
    public static class FirewallRule {
        private Integer priority;
        private IpInfo ip;
        private PortInfo port;
        private String protocol;
        private String rule;  // accept 또는 reject
    }
    
    @Data
    public static class IpInfo {
        @JsonProperty("ipv4_ip")
        private String ipv4Ip;
        private Integer bit;
    }
    
    @Data
    public static class PortInfo {
        private String mode;  // single 또는 multi
        private Object port;  // Integer 또는 String
    }
}