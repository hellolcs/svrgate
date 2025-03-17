package com.nicednb.svrgate.controller.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nicednb.svrgate.dto.AccountDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.AccountService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system/user")
public class UserController {

    private final Logger log = LoggerFactory.getLogger(UserController.class);
    private final AccountService accountService;

    // 사용자 관리 페이지: 사용자 목록 조회 및 신규 계정 추가 폼 바인딩
    @GetMapping
    public String userManagement(Model model, HttpSession session) {
        log.info("사용자 관리 페이지 접근");
        
        model.addAttribute("accountList", accountService.findAllAccounts());
        model.addAttribute("accountDto", new AccountDto());
        
        // 세션 속성 사용 후 제거
        session.removeAttribute("modalType");
        session.removeAttribute("modalError");
        session.removeAttribute("selectedUsername");
        
        return "system/user"; // 템플릿 파일: system/user.html
    }

    // 사용자 추가 처리: 모달 폼 전송 (POST /system/user/add)
    @PostMapping("/add")
    public String addAccount(@Valid @ModelAttribute("accountDto") AccountDto accountDto,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        log.info("사용자 추가 요청: {}", accountDto.getUsername());
        
        if (bindingResult.hasErrors()) {
            log.warn("사용자 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            model.addAttribute("accountList", accountService.findAllAccounts());
            // 모달 내 알림을 위한 세션 속성 설정
            session.setAttribute("modalType", "add");
            session.setAttribute("modalError", "입력 정보를 확인해주세요.");
            return "system/user";
        }
        
        if (!accountDto.getPassword().equals(accountDto.getPasswordConfirm())) {
            log.warn("사용자 추가 실패: 비밀번호 불일치 (사용자: {})", accountDto.getUsername());
            // 모달 내 알림을 위한 세션 속성 설정
            session.setAttribute("modalType", "add");
            session.setAttribute("modalError", "비밀번호가 일치하지 않습니다.");
            return "redirect:/system/user";
        }
        
        try {
            // DTO -> Account 엔티티 매핑
            Account account = Account.builder()
                    .username(accountDto.getUsername())
                    .password(accountDto.getPassword())
                    .name(accountDto.getName())
                    .department(accountDto.getDepartment())
                    .phoneNumber(accountDto.getPhoneNumber())
                    .email(accountDto.getEmail())
                    .allowedLoginIps(accountDto.getAllowedLoginIps())
                    .build();
            
            accountService.saveAccount(account);
            log.info("사용자 추가 성공: {}", accountDto.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "사용자가 성공적으로 추가되었습니다.");
            return "redirect:/system/user";
        } catch (IllegalArgumentException e) {
            log.error("사용자 추가 중 오류 발생: {}", e.getMessage(), e);
            // 모달 내 알림을 위한 세션 속성 설정
            session.setAttribute("modalType", "add");
            session.setAttribute("modalError", e.getMessage());
            return "redirect:/system/user";
        }
    }

    @PostMapping("/delete")
    public String deleteUser(@RequestParam("userId") String username, RedirectAttributes redirectAttributes) {
        log.info("사용자 삭제 요청: username={}", username);
        try {
            accountService.deleteAccountByUsername(username);
            log.info("사용자 삭제 성공: username={}", username);
            redirectAttributes.addFlashAttribute("successMessage", "사용자가 성공적으로 삭제되었습니다.");
            return "redirect:/system/user";
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "사용자 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/system/user";
        }
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute AccountDto accountDto, 
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        log.info("사용자 업데이트 요청: {}", accountDto.getUsername());
        try {
            // 비밀번호 확인 검증
            if (accountDto.getPassword() != null && !accountDto.getPassword().isEmpty() &&
                !accountDto.getPassword().equals(accountDto.getPasswordConfirm())) {
                log.warn("사용자 업데이트 실패: 비밀번호 불일치");
                // 모달 내 알림을 위한 세션 속성 설정
                session.setAttribute("modalType", "update");
                session.setAttribute("modalError", "비밀번호가 일치하지 않습니다.");
                session.setAttribute("selectedUsername", accountDto.getUsername());
                return "redirect:/system/user";
            }
            
            accountService.updateAccount(accountDto);
            log.info("사용자 업데이트 성공: {}", accountDto.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "사용자 정보가 성공적으로 업데이트되었습니다.");
            return "redirect:/system/user";
        } catch (IllegalArgumentException e) {
            log.error("사용자 업데이트 중 오류 발생: {}", e.getMessage(), e);
            // 모달 내 알림을 위한 세션 속성 설정
            session.setAttribute("modalType", "update");
            session.setAttribute("modalError", e.getMessage());
            session.setAttribute("selectedUsername", accountDto.getUsername());
            return "redirect:/system/user";
        }
    }
}