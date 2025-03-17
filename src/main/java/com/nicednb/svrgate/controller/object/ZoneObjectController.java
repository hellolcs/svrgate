package com.nicednb.svrgate.controller.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Zone 관련 요청을 처리하는 컨트롤러
 * Zone 조회, 추가, 수정, 삭제 등의 기능 처리
 */
@Controller
@RequestMapping("/object/zone")
public class ZoneObjectController {

    private final Logger log = LoggerFactory.getLogger(ZoneObjectController.class);

    /**
     * Zone 페이지
     */
    @GetMapping
    public String zoneObjects(Model model) {
        log.info("Zone 페이지 접근");
        return "object/zone";
    }
}