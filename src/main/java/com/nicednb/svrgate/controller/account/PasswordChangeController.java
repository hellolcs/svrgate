package com.nicednb.svrgate.controller.account;

import com.nicednb.svrgate.dto.PasswordChangeDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account/password-change")
public class PasswordChangeController {

    private final Logger log = LoggerFactory.getLogger(PasswordChangeController.class);
    private final AccountService accountService;
    private final OperationLogService operationLogService;

    @GetMapping
    public String passwordChangeForm(Model model) {
        log.info("비밀번호 변경 페이지 접근");
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/account/login";
        }
        
        Account account = (Account) auth.getPrincipal();
        
        // DTO 준비
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setUsername(account.getUsername());
        
        model.addAttribute("passwordChangeDto", passwordChangeDto);
        return "account/password-change";
    }
    
    @PostMapping
    public String changePassword(@Valid @ModelAttribute("passwordChangeDto") PasswordChangeDto passwordChangeDto,
                                 BindingResult bindingResult,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        log.info("비밀번호 변경 요청: {}", passwordChangeDto.getUsername());
        
        if (bindingResult.hasErrors()) {
            log.warn("비밀번호 변경 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "account/password-change";
        }
        
        // 현재 인증된 사용자 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        
        if (!currentUsername.equals(passwordChangeDto.getUsername())) {
            log.warn("비밀번호 변경 실패: 인증된 사용자({})와 요청 사용자({})가 일치하지 않음", 
                    currentUsername, passwordChangeDto.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "자신의 비밀번호만 변경할 수 있습니다.");
            return "redirect:/account/password-change";
        }
        
        // 비밀번호 일치 여부 확인
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getNewPasswordConfirm())) {
            log.warn("비밀번호 변경 실패: 새 비밀번호 불일치");
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/account/password-change";
        }
        
        try {
            // 현재 비밀번호 확인 및 비밀번호 변경
            Account account = accountService.findByUsername(currentUsername);
            
            if (!accountService.checkPassword(account, passwordChangeDto.getCurrentPassword())) {
                log.warn("비밀번호 변경 실패: 현재 비밀번호 불일치");
                redirectAttributes.addFlashAttribute("errorMessage", "현재 비밀번호가 일치하지 않습니다.");
                return "redirect:/account/password-change";
            }
            
            // 비밀번호 업데이트
            accountService.updatePassword(account, passwordChangeDto.getNewPassword());
            
            // 비밀번호 변경 시간 업데이트
            account.setLastPasswordChangeTime(LocalDateTime.now());
            accountService.saveAccount(account);
            
            // 성공 로그 남기기
            String ipAddress = accountService.getClientIpAddress(request);
            operationLogService.logOperation(
                    currentUsername,
                    ipAddress,
                    true,
                    null,
                    "계정관리",
                    "비밀번호 변경"
            );
            
            log.info("비밀번호 변경 성공: {}", currentUsername);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("비밀번호 변경 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/account/password-change";
        }
    }
}