package com.nicednb.svrgate.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // 생성자에서 기본 실패 URL을 받도록 설정
    public CustomAuthFailureHandler(String defaultFailureUrl) {
        super(defaultFailureUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // 실제 예외 메시지를 쿼리 파라미터에 붙여서 보냄
        String errorMessage = exception.getMessage();  // IP 불일치, BadCredentialsException 메시지 등
        setDefaultFailureUrl("/account/login?error=true&loginError=" + errorMessage);

        super.onAuthenticationFailure(request, response, exception);
    }
}