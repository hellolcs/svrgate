package com.nicednb.svrgate.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nicednb.svrgate.dto.AccountDto;
import com.nicednb.svrgate.dto.PersonalSettingDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.repository.AccountRepository;
import com.nicednb.svrgate.util.PasswordValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

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

        // 로그인 로직은 유지하되, IP 검사는 CustomDaoAuthenticationProvider로 이동
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String clientIp = getClientIpAddress(request);

        // IP 체크는 유지하지만, 추가 인증 단계에서 확인하도록 정보만 저장
        account.setClientIp(clientIp); // Account 클래스에 @Transient 필드 추가 필요

        // 로그인 성공 시각은 인증 성공 후 업데이트될 예정
        return account;
    }

    /**
     * 지정된 IP가 계정의 허용 IP 목록에 포함되어 있는지 확인합니다.
     * 이 메서드는 다른 컴포넌트(예: CustomDaoAuthenticationProvider)에서 호출할 수 있도록 public으로
     * 설정되었습니다.
     */
    public boolean isAllowedIp(String allowedLoginIps, String clientIp) {
        if (allowedLoginIps == null || allowedLoginIps.trim().isEmpty()) {
            return true;
        }
        List<String> allowedIpsList = Arrays.stream(allowedLoginIps.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        return allowedIpsList.contains(clientIp.trim());
    }

    /**
     * 로그인 성공 후 계정 정보를 업데이트합니다.
     */
    @Transactional
    public void updateLoginSuccess(Account account) {
        account.setLastLoginTime(LocalDateTime.now());
        accountRepository.save(account);

        String clientIp = account.getClientIp();
        operationLogService.logOperation(account.getUsername(), clientIp, true,
                null, // 성공이므로 실패 사유 없음
                "로그인", // 작업 유형
                "로그인 성공" // 설명
        );
        log.info("로그인 성공 username={}, ip={}", account.getUsername(), clientIp);
    }

    @Transactional(readOnly = true)
    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다: " + username));
    }

    /**
     * 사용자명으로 계정을 삭제합니다.
     */
    @Transactional
    public void deleteAccountByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("삭제할 계정을 찾을 수 없습니다: " + username));

        accountRepository.delete(account);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String clientIp = getCurrentClientIp();

        // 로그 메시지 형식 통일: failReason에 대상 정보를 포함
        operationLogService.logOperation(
                actor, // 작업 수행자
                clientIp, // IP 주소
                true, // 성공 여부
                "대상: " + username, // 대상 정보를 failReason 필드에 기록
                "계정관리", // 작업 유형
                "계정 삭제" // 간결한 설명
        );

        log.info("계정 삭제: actor={}, target={}", actor, username);
    }

    /**
     * 비밀번호 유효성 검사를 수행
     * 
     * @param password 검사할 비밀번호
     * @return 비밀번호가 규칙에 맞으면 true, 아니면 false
     */
    public boolean validatePassword(String password) {
        return PasswordValidator.isValid(password);
    }

    /**
     * 비밀번호 규칙 설명 반환
     */
    public String getPasswordRules() {
        return PasswordValidator.getPasswordRules();
    }

    /**
     * 계정 정보 업데이트 - 비밀번호 유효성 검사 추가
     * exception 대신 boolean 반환으로 변경
     */
    @Transactional
    public boolean updateAccount(AccountDto accountDto, StringBuilder errorMessage) {
        log.debug("계정 업데이트 시작: username={}", accountDto.getUsername());

        Account account;
        try {
            account = accountRepository.findByUsername(accountDto.getUsername())
                    .orElseThrow(() -> {
                        log.warn("계정 업데이트 실패: 사용자명 없음 - {}", accountDto.getUsername());
                        return new IllegalArgumentException("존재하지 않는 사용자명: " + accountDto.getUsername());
                    });
        } catch (Exception e) {
            errorMessage.append(e.getMessage());
            return false;
        }

        // 기존 계정 정보 업데이트
        account.setName(accountDto.getName());
        account.setDepartment(accountDto.getDepartment());
        account.setPhoneNumber(accountDto.getPhoneNumber());
        account.setEmail(accountDto.getEmail());
        account.setAllowedLoginIps(accountDto.getAllowedLoginIps());

        // 비밀번호가 입력된 경우에만 업데이트
        if (StringUtils.hasText(accountDto.getPassword())) {
            log.debug("계정 비밀번호 변경: username={}", accountDto.getUsername());

            // 비밀번호 규칙 검증
            if (!validatePassword(accountDto.getPassword())) {
                log.warn("계정 업데이트 실패: 비밀번호가 규칙에 맞지 않음 - {}", accountDto.getUsername());
                errorMessage.append(getPasswordRules());
                return false;
            }

            account.setPassword(passwordEncoder.encode(accountDto.getPassword()));
            account.setLastPasswordChangeTime(LocalDateTime.now());
        } else {
            log.debug("계정 비밀번호 유지: username={}", accountDto.getUsername());
        }

        accountRepository.save(account);

        // 변경 로깅 - 형식 통일
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String clientIp = getCurrentClientIp();
        operationLogService.logOperation(
                actor, // 작업 수행자
                clientIp, // IP 주소
                true, // 성공 여부
                "대상: " + account.getUsername(), // 대상 정보를 failReason 필드에 기록
                "계정관리", // 작업 유형
                "계정 정보 변경" // 간결한 설명
        );

        log.info("계정 업데이트 완료: username={}", accountDto.getUsername());
        return true;
    }

    /**
     * 사용자 계정 저장 - exception 대신 boolean 반환으로 변경
     */
    @Transactional
    public boolean saveAccount(Account account, StringBuilder errorMessage) {
        // 새 계정 또는 비밀번호 변경 시 검증
        if (account.getId() == null
                || (account.getPassword() != null && !account.getPassword().startsWith("{bcrypt}"))) {
            // 비밀번호 규칙 검증
            if (!validatePassword(account.getPassword())) {
                log.warn("계정 저장 실패: 비밀번호가 규칙에 맞지 않음 - {}", account.getUsername());
                errorMessage.append(getPasswordRules());
                return false;
            }

            account.setPassword(passwordEncoder.encode(account.getPassword()));
            account.setLastPasswordChangeTime(LocalDateTime.now());
        }

        boolean isNew = (account.getId() == null);
        Account savedAccount = accountRepository.save(account);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String clientIp = getCurrentClientIp();

        if (isNew) {
            // 계정 생성 로깅 - 형식 통일
            operationLogService.logOperation(
                    actor, // 작업 수행자
                    clientIp, // IP 주소
                    true, // 성공 여부
                    "대상: " + savedAccount.getUsername(), // 대상 정보를 failReason 필드에 기록
                    "계정관리", // 작업 유형
                    "계정 생성" // 간결한 설명
            );
            log.info("계정 추가: actor={}, target={}", actor, savedAccount.getUsername());
        } else {
            // 계정 변경 로깅 - 형식 통일
            operationLogService.logOperation(
                    actor, // 작업 수행자
                    clientIp, // IP 주소
                    true, // 성공 여부
                    "대상: " + savedAccount.getUsername(), // 대상 정보를 failReason 필드에 기록
                    "계정관리", // 작업 유형
                    "계정 정보 변경" // 간결한 설명
            );
            log.info("계정 변경: actor={}, target={}", actor, savedAccount.getUsername());
        }

        return true;
    }

    public String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIp(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (isEmptyIp(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (isEmptyIp(ip))
            ip = request.getHeader("HTTP_CLIENT_IP");
        if (isEmptyIp(ip))
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isEmptyIp(ip))
            ip = request.getRemoteAddr();
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

    private String getCurrentClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            return getClientIpAddress(request);
        } catch (Exception e) {
            log.warn("클라이언트 IP 주소 획득 실패: {}", e.getMessage());
            return "unknown";
        }
    }

    @Transactional(readOnly = true)
    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다: " + id));
    }

    /**
     * 개인 설정 업데이트 - exception 대신 boolean 반환으로 변경
     */
    @Transactional
    public boolean updatePersonalSetting(PersonalSettingDto personalSettingDto, StringBuilder errorMessage) {
        log.debug("개인설정 업데이트 시작: username={}", personalSettingDto.getUsername());

        Account account;
        try {
            account = accountRepository.findByUsername(personalSettingDto.getUsername())
                    .orElseThrow(() -> {
                        log.warn("계정 업데이트 실패: 사용자명 없음 - {}", personalSettingDto.getUsername());
                        return new IllegalArgumentException("존재하지 않는 사용자명: " + personalSettingDto.getUsername());
                    });
        } catch (Exception e) {
            errorMessage.append(e.getMessage());
            return false;
        }

        // 기존 계정 정보 업데이트
        account.setName(personalSettingDto.getName());
        account.setDepartment(personalSettingDto.getDepartment());
        account.setPhoneNumber(personalSettingDto.getPhoneNumber());
        account.setEmail(personalSettingDto.getEmail());
        account.setAllowedLoginIps(personalSettingDto.getAllowedLoginIps());

        // 비밀번호가 입력된 경우에만 업데이트
        if (StringUtils.hasText(personalSettingDto.getPassword())) {
            log.debug("계정 비밀번호 변경: username={}", personalSettingDto.getUsername());

            // 비밀번호 규칙 검증
            if (!validatePassword(personalSettingDto.getPassword())) {
                log.warn("개인설정 업데이트 실패: 비밀번호가 규칙에 맞지 않음 - {}", personalSettingDto.getUsername());
                errorMessage.append(getPasswordRules());
                return false;
            }

            account.setPassword(passwordEncoder.encode(personalSettingDto.getPassword()));
            account.setLastPasswordChangeTime(LocalDateTime.now());
        } else {
            log.debug("계정 비밀번호 유지: username={}", personalSettingDto.getUsername());
        }

        accountRepository.save(account);

        // 변경 로깅 - 형식 통일
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String clientIp = getCurrentClientIp();
        operationLogService.logOperation(
                actor, // 작업 수행자
                clientIp, // IP 주소
                true, // 성공 여부
                "대상: " + account.getUsername(), // 대상 정보를 failReason 필드에 기록
                "계정관리", // 작업 유형
                "개인설정 변경" // 간결한 설명
        );

        log.info("개인설정 업데이트 완료: username={}", personalSettingDto.getUsername());
        return true;
    }

    /**
     * 계정의 비밀번호 일치 여부를 확인합니다.
     */
    public boolean checkPassword(Account account, String rawPassword) {
        return passwordEncoder.matches(rawPassword, account.getPassword());
    }

    /**
     * 계정의 비밀번호를 업데이트합니다. - exception 대신 boolean 반환으로 변경
     */
    @Transactional
    public boolean updatePassword(Account account, String newPassword, StringBuilder errorMessage) {
        // 비밀번호 규칙 검증
        if (!validatePassword(newPassword)) {
            log.warn("비밀번호 업데이트 실패: 비밀번호가 규칙에 맞지 않음 - {}", account.getUsername());
            errorMessage.append(getPasswordRules());
            return false;
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setLastPasswordChangeTime(LocalDateTime.now());
        accountRepository.save(account);

        log.info("비밀번호 업데이트 완료: username={}", account.getUsername());
        return true;
    }

    /**
     * 검색어를 기반으로 계정 검색
     * 
     * @param searchText 검색어
     * @return 검색 결과 계정 목록
     */
    public List<Account> searchAccounts(String searchText) {
        log.debug("계정 검색: searchText={}", searchText);

        if (searchText == null || searchText.trim().isEmpty()) {
            // 검색어가 없으면 전체 계정 목록 반환
            return accountRepository.findAll();
        }

        // 검색어가 있으면 Repository에 검색 쿼리 요청
        return accountRepository.searchByKeyword(searchText.trim());
    }
}