package com.nicednb.svrgate.config;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private final Logger log = LoggerFactory.getLogger(PasswordChangeInterceptor.class);
    private final SystemSettingService systemSettingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 로그인한 사용자가 없거나 특정 경로 접근 시 통과
        if (auth == null || auth.getPrincipal().equals("anonymousUser") || 
                request.getRequestURI().startsWith("/account/password-change") ||
                request.getRequestURI().startsWith("/account/login")) {
            return true;
        }
        
        // 로그인한 사용자가 없거나 이미 비밀번호 변경 페이지로 이동하는 경우 통과
        if (auth == null || auth.getPrincipal().equals("anonymousUser") || 
                request.getRequestURI().startsWith("/account/password-change")) {
            return true;
        }
        
        // 비밀번호 변경 주기 확인이 필요한 인증된 사용자
        if (auth.getPrincipal() instanceof Account) {
            Account account = (Account) auth.getPrincipal();
            int passwordChangeCycle = systemSettingService.getPasswordChangeCycle();
            
            if (account.isPasswordChangeRequired(passwordChangeCycle)) {
                log.info("비밀번호 변경 필요: username={}, 마지막 변경일={}", 
                        account.getUsername(), account.getLastPasswordChangeTime());
                
                // 비밀번호 변경 페이지로 리다이렉트
                response.sendRedirect("/account/password-change");
                return false;
            }
        }
        
        return true;
    }
}