package com.nicednb.svrgate.controller.system;

// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nicednb.svrgate.dto.AccountDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.AccountService;

import jakarta.validation.Valid;
// import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system/user")
public class UserController {

    private final AccountService accountService;

    // 사용자 관리 페이지: 사용자 목록 조회 및 신규 계정 추가 폼 바인딩
    @GetMapping
    public String userManagement(Model model) {
        model.addAttribute("accountList", accountService.findAllAccounts());
        model.addAttribute("accountDto", new AccountDto());
        return "system/user"; // 템플릿 파일: system/user.html
    }

    // 사용자 추가 처리: 모달 폼 전송 (POST /system/user/add)
    @PostMapping("/add")
    public String addAccount(@Valid @ModelAttribute("accountDto") AccountDto accountDto,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accountList", accountService.findAllAccounts());
            return "system/user";
        }
        if (!accountDto.getPassword().equals(accountDto.getPasswordConfirm())) {
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("accountList", accountService.findAllAccounts());
            return "system/user";
        }
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
        return "redirect:/system/user";
    }

    @PostMapping("/delete")
    public String deleteUser(@RequestParam("userId") Long userId) {
        accountService.deleteAccount(userId);
        return "redirect:/system/user";
    }

    @PostMapping("/update")
    public String updateUser(@ModelAttribute AccountDto accountDto) {
        try {
            // 기존 updateAccount 함수 활용
            accountService.updateAccount(accountDto);
            return "redirect:/system/user";
        } catch (Exception e) {
            // 오류 로깅
            log.error("사용자 업데이트 중 오류 발생: {}", e.getMessage());
            return "redirect:/system/user?error=update_failed";
        }
    }
}
