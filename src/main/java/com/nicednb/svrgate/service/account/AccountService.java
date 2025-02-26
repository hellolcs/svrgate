package com.nicednb.svrgate.service.account;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.entity.LoginHistory;
import com.nicednb.svrgate.repository.AccountRepository;
import com.nicednb.svrgate.repository.LoginHistoryRepository;
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
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    @Override
    @Transactional
    public Account loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다."));

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String clientIp = getClientIpAddress(request);

        // IP가 허용 목록에 있는지 검사
        if (!isAllowedIp(account.getAllowedLoginIps(), clientIp)) {
            // 로그인 실패 로그 기록
            saveLoginHistory(username, clientIp, false, "접근이 허용되지 않은 IP");
            log.warn("로그인 실패 - IP 불일치 username={}, clientIp={}", username, clientIp);
            throw new BadCredentialsException("접근이 허용되지 않은 IP 주소입니다.");
        }

        // 로그인 성공 시각 업데이트
        account.setLastLoginTime(LocalDateTime.now());
        accountRepository.save(account);

        // 로그인 성공 로그 기록
        saveLoginHistory(username, clientIp, true, null);
        log.info("로그인 성공 username={}, ip={}", username, clientIp);

        return account;
    }

    // 비밀번호 인코딩 후 계정 생성/수정
    @Transactional
    public Account saveAccount(Account account) {
        // 새로 등록/수정 시 비밀번호 해시 처리
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

    // (예시) ID로 계정 조회
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    // 로그인 이력 저장
    @Transactional
    public void saveLoginHistory(String username, String ipAddress, boolean success, String failReason) {
        LoginHistory history = LoginHistory.builder()
                .username(username)
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .success(success)
                .failReason(failReason)
                .build();
        loginHistoryRepository.save(history);
    }

    // 접속 IP가 허용된 IP 목록에 있는지 체크
    private boolean isAllowedIp(String allowedLoginIps, String clientIp) {
        if (allowedLoginIps == null || allowedLoginIps.trim().isEmpty()) {
            return true; // 허용 IP가 없으면 일단 모두 허용
        }
        List<String> allowedIpsList = Arrays.stream(allowedLoginIps.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        return allowedIpsList.contains(clientIp.trim());
    }

    // 실제 클라이언트 IP 추출
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIp(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isEmptyIp(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (isEmptyIp(ip)) ip = request.getHeader("HTTP_CLIENT_IP");
        if (isEmptyIp(ip)) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isEmptyIp(ip)) ip = request.getRemoteAddr();

        // IPv6 루프백 -> IPv4 변환
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
