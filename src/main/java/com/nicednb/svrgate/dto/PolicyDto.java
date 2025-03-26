package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PolicyDto {

    private Long id;

    @NotNull(message = "우선순위를 입력해주세요.")
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다.")
    private Integer priority; // 우선순위

    @NotNull(message = "출발지 객체를 선택해주세요.")
    private Long sourceObjectId; // 출발지 객체 ID
    
    @NotEmpty(message = "출발지 객체 유형을 선택해주세요.")
    private String sourceObjectType; // 출발지 객체 유형 (SERVER, GENERAL, NETWORK)
    
    // 화면 표시용 출발지 객체 이름
    private String sourceObjectName;

    @NotEmpty(message = "프로토콜을 선택해주세요.")
    private String protocol; // 프로토콜 (tcp, udp)

    @NotEmpty(message = "포트 모드를 선택해주세요.")
    private String portMode; // 포트 모드 (single, multi)

    @NotNull(message = "시작 포트를 입력해주세요.")
    @Min(value = 1, message = "포트는 1 이상이어야 합니다.")
    @Max(value = 65535, message = "포트는 65535 이하여야 합니다.")
    private Integer startPort; // 시작 포트
    
    @Min(value = 1, message = "포트는 1 이상이어야 합니다.")
    @Max(value = 65535, message = "포트는 65535 이하여야 합니다.")
    private Integer endPort; // 끝 포트 (portMode가 multi인 경우에만 필요)

    @NotEmpty(message = "동작을 선택해주세요.")
    private String action; // 동작 (accept, reject)

    private Integer timeLimit; // 시간제한(h)

    @NotNull(message = "로깅 여부를 선택해주세요.")
    private Boolean logging; // 로깅 (true: 사용, false: 미사용)

    private LocalDateTime registrationDate; // 등록일
    private String registrationDateFormatted; // 등록일 포맷팅된 문자열
    
    private LocalDateTime expiresAt; // 정책 만료 시간 (추가됨)
    private String expiresAtFormatted; // 만료 시간 포맷팅된 문자열 (추가됨)

    private String requester; // 요청자

    private String registrar; // 등록자 (계정 Name)

    private String description; // 설명

    // 서버 관련 정보
    @NotNull(message = "서버를 선택해주세요.")
    private Long serverObjectId; // 서버 ID
    private String serverObjectName; // 서버 이름
}