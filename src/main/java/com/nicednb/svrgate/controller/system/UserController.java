package com.nicednb.svrgate.controller.system;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system/user")
public class UserController {

    private final AccountService accountService;

    // 사용자 관리 페이지: 사용자 목록 조회
    @GetMapping
    public String userManagement(Model model) {
        model.addAttribute("accountList", accountService.findAllAccounts());
        return "system/user";
    }

    // 사용자 추가 처리 (Modal 폼 전송)
    @PostMapping("/new")
    public String addUser(@ModelAttribute Account account,
                          @RequestParam("passwordConfirm") String passwordConfirm,
                          Model model) {
        if (!account.getPassword().equals(passwordConfirm)) {
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("accountList", accountService.findAllAccounts());
            return "system/user";
        }
        accountService.saveAccount(account);
        return "redirect:/system/user";
    }

    // 사용자 삭제 처리 (예시)
    @PostMapping("/delete")
    public String deleteUser(@RequestParam("userId") Long userId) {
        accountService.deleteAccount(userId);
        return "redirect:/system/user";
    }

    // 사용자 변경 처리 (예시)
    @PostMapping("/update")
    public String updateUser(@ModelAttribute Account account) {
        accountService.saveAccount(account);
        return "redirect:/system/user";
    }
}
