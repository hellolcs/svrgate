package com.nicednb.svrgate.util;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 비밀번호 규칙을 검증하는 유틸리티 클래스
 */
public class PasswordValidator {
    
    private static final Logger log = LoggerFactory.getLogger(PasswordValidator.class);
    
    // 비밀번호 규칙 정규식: 최소 8자, 최소 하나의 대문자, 하나의 소문자, 하나의 숫자, 하나의 특수문자
    private static final String PASSWORD_PATTERN = 
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*()_\\-{}\\[\\]|:;\"'<>,.?/~`])(?=\\S+$).{8,}$";
    
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    /**
     * 비밀번호가 규칙에 맞는지 검증
     * 
     * @param password 검증할 비밀번호
     * @return 유효한 비밀번호이면 true, 아니면 false
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        
        boolean isValid = pattern.matcher(password).matches();
        log.debug("비밀번호 유효성 검사: {}", isValid ? "통과" : "실패");
        
        return isValid;
    }
    
    /**
     * 비밀번호 규칙에 대한 설명 반환
     * 
     * @return 비밀번호 규칙 설명
     */
    public static String getPasswordRules() {
        return "비밀번호는 최소 8자 이상으로, 영문 대문자, 영문 소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다.";
    }
}