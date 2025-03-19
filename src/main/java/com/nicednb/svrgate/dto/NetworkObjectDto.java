package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NetworkObjectDto {

    private Long id;

    @NotEmpty(message = "네트워크 이름을 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "네트워크 이름은 영문자, 숫자, 대시(-), 언더바(_)만 허용됩니다.")
    private String name;

    @NotEmpty(message = "IP 주소를 입력해주세요.")
    // 정규식 수정
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/)(8|16|24)$", 
             message = "유효한 A, B, C 클래스 IPv4 주소 형식이 아닙니다. 예: 10.0.0.0/8, 172.16.0.0/16, 192.168.1.0/24")
    private String ipAddress;

    private String description;

    // 선택된 Zone ID 목록
    private List<Long> zoneIds;

    // 화면 표시용 필드
    private String zoneNames;
}