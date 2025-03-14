package com.nicednb.svrgate.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.exception.IpAddressRestrictionException;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.OperationLogService;

import lombok.Setter;

public class CustomDaoAuthenticationProvider extends DaoAuthenticationProvider {
    
    private final Logger log = LoggerFactory.getLogger(CustomDaoAuthenticationProvider.class);
    
    @Setter
    private AccountService accountService;
    
    @Setter
    private OperationLogService operationLogService;
    
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        try {
            super.additionalAuthenticationChecks(userDetails, authentication);
            
            // 비밀번호 검증 후 IP 제한 검사 추가
            if (userDetails instanceof Account) {
                Account account = (Account) userDetails;
                String clientIp = account.getClientIp();
                String allowedIps = account.getAllowedLoginIps();
                
                if (!accountService.isAllowedIp(allowedIps, clientIp)) {
                    // 로그인 실패 로깅
                    operationLogService.logOperation(
                            account.getUsername(),
                            clientIp,
                            false,
                            "접근이 허용되지 않은 IP",
                            "로그인",
                            "로그인 실패"
                    );
                    log.warn("로그인 실패 - IP 불일치 username={}, clientIp={}", account.getUsername(), clientIp);
                    throw new IpAddressRestrictionException("접근이 허용되지 않은 IP 주소입니다: " + clientIp);
                }
            }
        } catch (BadCredentialsException ex) {
            // 비밀번호 불일치인 경우 구체적 메시지
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }
    }
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 인증 수행
        Authentication result = super.authenticate(authentication);
        
        // 인증 성공 후 로그인 성공 처리
        if (result.isAuthenticated() && result.getPrincipal() instanceof Account) {
            Account account = (Account) result.getPrincipal();
            accountService.updateLoginSuccess(account);
        }
        
        return result;
    }
}