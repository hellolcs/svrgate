package com.nicednb.svrgate.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public String handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request, Model model) {
        log.warn("Authentication failure: {}", ex.getMessage());
        model.addAttribute("loginError", ex.getMessage());
        return "account/login"; // 로그인 뷰로 돌아가며 에러 메시지 전달
    }

    // 추가 예외 핸들러 작성 가능
}
