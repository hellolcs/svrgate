package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalSettingDto {

    @NotEmpty(message = "ID를 입력해주세요.")
    private String username;

    // 비밀번호와 비밀번호 확인은 필수가 아님
    private String password;
    private String passwordConfirm;

    @NotEmpty(message = "이름을 입력해주세요.")
    private String name;

    private String department;

    @NotEmpty(message = "연락처를 입력해주세요.")
    @Pattern(regexp = "^\\d{1,3}-\\d{1,4}-\\d{1,4}$", message = "연락처 형식은 숫자 1~3자리-숫자 1~4자리-숫자 1~4자리여야 합니다.")
    private String phoneNumber;

    @NotEmpty(message = "이메일을 입력해주세요.")
    private String email;

    @NotEmpty(message = "접속 IP를 입력해주세요.")
    @Pattern(
            regexp = "^(?:(?:25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})(?:\\.(?:25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})){3})(?:\\s*,\\s*(?:(?:25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})(?:\\.(?:25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})){3}))*$",
            message = "유효한 IPv4 주소를 콤마(,)로 구분하여 입력해주세요."
    )
    private String allowedLoginIps;
}