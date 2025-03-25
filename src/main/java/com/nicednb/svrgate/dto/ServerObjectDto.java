package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServerObjectDto {

    private Long id;

    @NotEmpty(message = "서버 이름을 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "서버 이름은 영문자, 숫자, 대시(-), 언더바(_)만 허용됩니다.")
    private String name;

    @NotEmpty(message = "IP 주소를 입력해주세요.")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", 
             message = "유효한 IPv4 주소 형식이 아닙니다.")
    private String ipAddress;

    private boolean active; // 연동여부
    
    private String apiKey; // API Key

    private String description;

    // 선택된 Zone ID
    @NotNull(message = "Zone을 선택해주세요.")
    private Long zoneId;

    // 화면 표시용 필드
    private String zoneName;
    
    // 마지막 연동 시각
    private LocalDateTime lastSyncTime;
    
    // 마지막 연동 시각 표시용
    private String lastSyncTimeFormatted;
}