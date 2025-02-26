package com.nicednb.svrgate.controller.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 로그인 성공 후 이동할 대시보드 컨트롤러
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @GetMapping
    public String dashboard(Model model) {
        log.info("대시보드 페이지 접근");
        // 대시보드에 표시할 데이터 세팅
        return "dashboard/dashboard";
    }
}
