package com.nicednb.svrgate.controller.system;

import com.nicednb.svrgate.dto.SystemSettingDto;
import com.nicednb.svrgate.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nicednb.svrgate.service.AccountService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/system/setting")
public class SystemSettingController {

    private final Logger log = LoggerFactory.getLogger(SystemSettingController.class);
    private final SystemSettingService systemSettingService;
    private final AccountService accountService;

    @GetMapping
    public String settingForm(Model model) {
        log.info("시스템 설정 페이지 접근");
        
        // 현재 설정값 조회
        SystemSettingDto settingDto = systemSettingService.getSettings();
        model.addAttribute("systemSettingDto", settingDto);
        
        return "system/setting";
    }
    
    @PostMapping
    public String updateSetting(@Valid @ModelAttribute("systemSettingDto") SystemSettingDto systemSettingDto,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        log.info("시스템 설정 업데이트 요청");
        
        if (bindingResult.hasErrors()) {
            log.warn("시스템 설정 업데이트 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "system/setting";
        }
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // 클라이언트 IP 주소 가져오기
            String ipAddress = accountService.getClientIpAddress(request);
            
            // 설정 저장
            systemSettingService.saveSettings(systemSettingDto, username, ipAddress);
            
            // 성공 메시지
            redirectAttributes.addFlashAttribute("successMessage", "시스템 설정이 성공적으로 업데이트되었습니다.");
            
            return "redirect:/system/setting";
        } catch (Exception e) {
            log.error("시스템 설정 업데이트 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "시스템 설정 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/system/setting";
        }
    }
}