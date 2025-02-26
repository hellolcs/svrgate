package com.nicednb.svrgate.controller.account;

import com.nicednb.svrgate.dto.LoginDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    private final Logger log = LoggerFactory.getLogger(AccountController.class);

    @GetMapping("/login")
    public String loginForm(Model model,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "loginError", required = false) String loginError) {
        log.info("로그인 폼 요청");
        model.addAttribute("loginDto", new LoginDto());
        if (error != null) {
            model.addAttribute("error", true);
            model.addAttribute("loginError", loginError);
        }
        return "account/login"; // templates/account/login.html
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("로그아웃 처리");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/account/login";
    }
}
