package com.nicednb.svrgate.controller.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 객체관리 메인 페이지 접근을 위한 컨트롤러
 * 객체관리 메인 메뉴 클릭 시 서버객체 페이지로 리다이렉트
 */
@Controller
@RequestMapping("/object")
public class ObjectMenuController {

    private final Logger log = LoggerFactory.getLogger(ObjectMenuController.class);

    /**
     * 객체관리 메인 페이지 - 기본적으로 연동서버객체 페이지로 리다이렉트
     */
    @GetMapping
    public String index() {
        log.info("객체관리 메인 페이지 접근 - 연동서버객체로 리다이렉트");
        return "redirect:/object/server";
    }
}