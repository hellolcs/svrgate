package com.nicednb.svrgate.service.account;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.repository.AccountRepository;
import com.nicednb.svrgate.service.log.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    @Override
    @Transactional
    public Account loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String clientIp = getClientIpAddress(request);

        // IP 허용 검사
        if (!isAllowedIp(account.getAllowedLoginIps(), clientIp)) {
            operationLogService.logOperation(username, clientIp, false, "접근이 허용되지 않은 IP 주소입니다.", "로그인", "IP 불일치");
            log.warn("로그인 실패 - IP 불일치 username={}, clientIp={}", username, clientIp);
            throw new BadCredentialsException("접근이 허용되지 않은 IP 주소입니다.");
        }

        // 로그인 성공 시각 업데이트
        account.setLastLoginTime(LocalDateTime.now());
        accountRepository.save(account);

        operationLogService.logOperation(username, clientIp, true, null, "로그인", "로그인 성공");
        log.info("로그인 성공 username={}, ip={}", username, clientIp);

        return account;
    }

    // 계정 생성/수정 시 비밀번호 인코딩 처리
    @Transactional
    public Account saveAccount(Account account) {
        if (!account.getPassword().startsWith("{bcrypt}")) {
            account.setPassword(passwordEncoder.encode(account.getPassword()));
        }
        return accountRepository.save(account);
    }

    // 계정 삭제
    @Transactional
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    private boolean isAllowedIp(String allowedLoginIps, String clientIp) {
        if (allowedLoginIps == null || allowedLoginIps.trim().isEmpty()) {
            return true;
        }
        List<String> allowedIpsList = Arrays.stream(allowedLoginIps.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        return allowedIpsList.contains(clientIp.trim());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIp(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isEmptyIp(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (isEmptyIp(ip)) ip = request.getHeader("HTTP_CLIENT_IP");
        if (isEmptyIp(ip)) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isEmptyIp(ip)) ip = request.getRemoteAddr();

        if (ip != null && ip.contains(":")) {
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
        }
        return ip;
    }

    private boolean isEmptyIp(String ip) {
        return (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip));
    }

    @Transactional(readOnly = true)
    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + id));
    }
}
