package com.nicednb.svrgate.dto;

import lombok.Data;

/**
 * 방화벽 정책 API 응답 DTO
 * 방화벽 관리 Agent API 정의서에 맞게 정의됨
 */
@Data
public class FirewallRuleResponse {
    private Boolean success;
    private String code;
    private String message;
    private ResponseData data;
    
    /**
     * 응답 데이터 내부 클래스
     */
    @Data
    public static class ResponseData {
        private String deletedAt;
    }
}