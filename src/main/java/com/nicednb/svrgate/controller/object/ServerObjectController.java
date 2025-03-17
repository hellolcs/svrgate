package com.nicednb.svrgate.controller.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 연동서버객체 관련 요청을 처리하는 컨트롤러
 * 연동서버객체 조회, 추가, 수정, 삭제 등의 기능 처리
 */
@Controller
@RequestMapping("/object/server")
public class ServerObjectController {

    private final Logger log = LoggerFactory.getLogger(ServerObjectController.class);

    /**
     * 연동서버객체 페이지
     */
    @GetMapping
    public String serverObjects(Model model) {
        log.info("연동서버객체 페이지 접근");
        return "object/server";
    }
}