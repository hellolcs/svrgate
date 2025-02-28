package com.nicednb.svrgate.controller.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", "/dashboard"})
public class DashboardController {

    private final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @GetMapping
    public String dashboard(Model model) {
        log.info("대시보드 페이지 접근");
        return "dashboard/dashboard";
    }
}
