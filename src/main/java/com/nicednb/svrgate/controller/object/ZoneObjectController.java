package com.nicednb.svrgate.controller.object;

import com.nicednb.svrgate.dto.ZoneObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.ZoneObjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zone 관련 요청을 처리하는 컨트롤러
 * Zone 조회, 추가, 수정, 삭제 등의 기능 처리
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/object/zone")
public class ZoneObjectController {

    private final Logger log = LoggerFactory.getLogger(ZoneObjectController.class);
    private final ZoneObjectService zoneService;
    private final AccountService accountService;

    // 기존 메서드는 유지하고 syncWithFirewall 메서드만 수정합니다...

    /**
     * 방화벽 동기화
     */
    @PostMapping("/sync/{id}")
    public String syncWithFirewall(@PathVariable("id") Long id, 
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        log.info("방화벽 동기화 요청: {}", id);

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            zoneService.syncWithFirewall(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "방화벽과 동기화가 성공적으로 완료되었습니다.");
        } catch (IllegalStateException e) {
            // 연동이 비활성화된 경우
            log.error("방화벽 동기화 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("방화벽 동기화 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "방화벽 동기화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/object/zone";
    }
}