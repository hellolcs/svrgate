package com.nicednb.svrgate.config.security;

import com.nicednb.svrgate.exception.IpAddressRestrictionException;
import com.nicednb.svrgate.service.OperationLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private OperationLogService operationLogService;
    
    private final Logger log = LoggerFactory.getLogger(CustomAuthFailureHandler.class);

    public CustomAuthFailureHandler() {
        // 기본 실패 URL 설정
        super.setDefaultFailureUrl("/account/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String errorMessage;
        
        // 예외 타입에 따른 메시지 처리
        if (exception instanceof BadCredentialsException) {
            errorMessage = exception.getMessage();
        } else if (exception instanceof IpAddressRestrictionException) {
            errorMessage = exception.getMessage();
        } else if (exception instanceof InternalAuthenticationServiceException) {
            // 내부에 원인이 다른 예외인 경우 확인
            Throwable cause = exception.getCause();
            if (cause instanceof IpAddressRestrictionException) {
                errorMessage = cause.getMessage();
            } else if (cause instanceof BadCredentialsException) {
                errorMessage = cause.getMessage();
            } else {
                errorMessage = "내부 시스템 문제로 로그인 요청을 처리할 수 없습니다.";
                log.error("인증 중 내부 오류 발생", exception);
            }
        } else {
            errorMessage = "로그인 중 오류가 발생했습니다. 관리자에게 문의하세요.";
            log.error("인증 실패: {}", exception.getMessage(), exception);
        }

        // 로그인 실패 정보 기록
        String username = request.getParameter("username");
        String clientIp = getClientIpAddress(request);
        operationLogService.logOperation(
                username,
                clientIp,
                false,
                errorMessage,
                "로그인",
                "로그인 실패"
        );

        // URL 인코딩을 통해 한글 메시지를 안전하게 전달
        String encodedMsg = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/account/login?error=true&loginError=" + encodedMsg);

        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIpAddress(HttpServletRequest request) {

                String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(":")) {
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
        }
        return ip;
    }
}