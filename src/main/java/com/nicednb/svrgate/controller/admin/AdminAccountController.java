package com.nicednb.svrgate.controller.admin;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;

    // 전체 계정 목록 조회
    @GetMapping
    public String listAccounts(Model model) {
        List<Account> accounts = accountService.findAllAccounts();
        model.addAttribute("accounts", accounts);
        return "admin/accounts/list";
    }

    // 새 계정 생성 폼 표시
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new Account());
        return "admin/accounts/form";
    }

    // 계정 생성 처리
    @PostMapping
    public String createAccount(@ModelAttribute Account account) {
        accountService.saveAccount(account);
        return "redirect:/admin/accounts";
    }

    // 계정 수정 폼 표시
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Account account = accountService.findAccountById(id);
        model.addAttribute("account", account);
        return "admin/accounts/form";
    }

    // 계정 수정 처리
    @PostMapping("/{id}")
    public String updateAccount(@PathVariable Long id, @ModelAttribute Account account) {
        account.setId(id);
        accountService.saveAccount(account);
        return "redirect:/admin/accounts";
    }

    // 계정 삭제 처리
    @PostMapping("/{id}/delete")
    public String deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return "redirect:/admin/accounts";
    }
}
