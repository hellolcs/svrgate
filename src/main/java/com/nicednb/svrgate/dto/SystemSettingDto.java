package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemSettingDto {
    
    // 기본 설정
    @NotNull(message = "최대 유휴시간을 입력해주세요.")
    @Min(value = 1, message = "최대 유휴시간은 1 이상이어야 합니다.")
    private Integer maxIdleTime;
    
    @NotNull(message = "패스워드 변경주기를 입력해주세요.")
    @Min(value = 1, message = "패스워드 변경주기는 1 이상이어야 합니다.")
    private Integer passwordChangeCycle;
    
    // 서버 연동 설정
    @NotNull(message = "서버정책 수집주기를 입력해주세요.")
    @Min(value = 1, message = "서버정책 수집주기는 1 이상이어야 합니다.")
    private Integer serverPolicyCycle;
    
    @NotNull(message = "동시 수집 서버 수를 입력해주세요.")
    @Min(value = 1, message = "동시 수집 서버 수는 1 이상이어야 합니다.")
    private Integer concurrentServers;
    
    @NotNull(message = "정책 만료 확인 주기를 입력해주세요.")
    @Min(value = 1, message = "정책 만료 확인 주기는 1 이상이어야 합니다.")
    private Integer policyExpiryCheckCycle;
    
    @NotNull(message = "서버 연동 포트를 입력해주세요.")
    @Min(value = 1, message = "서버 연동 포트는 1 이상이어야 합니다.")
    private Integer serverConnectionPort;
    
    // 방화벽 연동 설정
    @NotNull(message = "방화벽정책 수집주기를 입력해주세요.")
    @Min(value = 1, message = "방화벽정책 수집주기는 1 이상이어야 합니다.")
    private Integer firewallPolicyCycle;
    
    @NotNull(message = "동시 수집 방화벽 수를 입력해주세요.")
    @Min(value = 1, message = "동시 수집 방화벽 수는 1 이상이어야 합니다.")
    private Integer concurrentFirewalls;
}