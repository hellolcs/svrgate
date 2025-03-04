package com.nicednb.svrgate.controller.system;

import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system")
public class SystemController {

    private final AccountService accountService;

    // /system -> /system/user 로 리다이렉트 (Not Found 방지)
    @GetMapping
    public String systemRoot() {
        return "redirect:/system/user";
    }

    // 계정관리
    @GetMapping("/user")
    public String userManagement(Model model) {
        List<Account> accountList = accountService.findAllAccounts();
        model.addAttribute("accountList", accountList);
        return "system/user";
    }

    // 시스템 설정
    @GetMapping("/setting")
    public String systemSetting(Model model) {
        // ... 필요 시 로직
        return "system/setting";
    }
}
