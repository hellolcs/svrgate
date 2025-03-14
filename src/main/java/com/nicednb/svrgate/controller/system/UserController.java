package com.nicednb.svrgate.controller.system;

import com.nicednb.svrgate.dto.AccountDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.AccountService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
// import jakarta.servlet.http.HttpServletRequest;

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
            // 기존 계정을 먼저 조회
            Account existingAccount = accountService.findByUsername(accountDto.getUsername());

            // 필드 업데이트
            existingAccount.setName(accountDto.getName());
            existingAccount.setDepartment(accountDto.getDepartment());
            existingAccount.setPhoneNumber(accountDto.getPhoneNumber());
            existingAccount.setEmail(accountDto.getEmail());
            existingAccount.setAllowedLoginIps(accountDto.getAllowedLoginIps());

            // 비밀번호가 제공된 경우에만 업데이트
            if (accountDto.getPassword() != null && !accountDto.getPassword().isEmpty()) {
                existingAccount.setPassword(accountDto.getPassword());
            }

            accountService.saveAccount(existingAccount);
            return "redirect:/system/user";
        } catch (UsernameNotFoundException e) {
            // 오류 처리
            return "redirect:/system/user?error=user_not_found";
        }
    }
}
