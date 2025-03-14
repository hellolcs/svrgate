package com.nicednb.svrgate.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeDto {

    @NotEmpty(message = "사용자 ID는 필수입니다.")
    private String username;
    
    @NotEmpty(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
    
    @NotEmpty(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;
    
    @NotEmpty(message = "새 비밀번호 확인을 입력해주세요.")
    private String newPasswordConfirm;
}