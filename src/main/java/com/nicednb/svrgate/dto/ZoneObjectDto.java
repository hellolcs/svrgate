package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ZoneObjectDto {

    private Long id;

    @NotEmpty(message = "Zone명을 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Zone명은 영문자, 숫자, 대시(-), 언더바(_)만 허용됩니다.")
    private String name;

    @NotEmpty(message = "방화벽 IP를 입력해주세요.")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", 
             message = "유효한 IPv4 주소 형식이 아닙니다.")
    private String firewallIp;

    private List<Long> nonSecureZoneIds; // 비보안Zone ID 목록
    private List<Long> secureZoneIds; // 보안Zone ID 목록
    private boolean active; // 사용 여부
    private String description; // 설명

    // 화면 표시용 필드
    private String nonSecureZoneNames;
    private String secureZoneNames;
}