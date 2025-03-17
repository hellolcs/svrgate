package com.nicednb.svrgate.controller.account;

import com.nicednb.svrgate.dto.PersonalSettingDto;
import com.nicednb.svrgate.entity.Account;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.OperationLogService;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account/personal")
public class PersonalSettingController {

    private final Logger log = LoggerFactory.getLogger(PersonalSettingController.class);
    private final AccountService accountService;
    private final OperationLogService operationLogService;

    @GetMapping
    public String personalSettingForm(Model model) {
        log.info("개인설정 페이지 접근");
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.debug("현재 로그인한 사용자: {}", username);
        
        Account account = accountService.findByUsername(username);
        
        // PersonalSettingDto로 변환
        PersonalSettingDto personalSettingDto = new PersonalSettingDto();
        personalSettingDto.setUsername(account.getUsername());
        personalSettingDto.setName(account.getName());
        personalSettingDto.setDepartment(account.getDepartment());
        personalSettingDto.setPhoneNumber(account.getPhoneNumber());
        personalSettingDto.setEmail(account.getEmail());
        personalSettingDto.setAllowedLoginIps(account.getAllowedLoginIps());
        // 비밀번호 필드는 비어있는 상태로 유지
        
        model.addAttribute("personalSettingDto", personalSettingDto);
        return "account/personal";
    }

    @PostMapping
    public String updatePersonalSetting(@Valid @ModelAttribute("personalSettingDto") PersonalSettingDto personalSettingDto,
                                        BindingResult bindingResult,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        log.info("개인설정 업데이트 요청: {}", personalSettingDto.getUsername());
        
        if (bindingResult.hasErrors()) {
            log.warn("개인설정 업데이트 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "account/personal";
        }
        
        // 현재 인증된 사용자와 수정 요청한 사용자가 일치하는지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        
        if (!currentUsername.equals(personalSettingDto.getUsername())) {
            log.warn("개인설정 업데이트 실패: 인증된 사용자({})와 요청 사용자({})가 일치하지 않음", 
                    currentUsername, personalSettingDto.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "자신의 계정 정보만 수정할 수 있습니다.");
            return "redirect:/account/personal";
        }
        
        // 비밀번호가 입력된 경우에만 확인
        if (personalSettingDto.getPassword() != null && !personalSettingDto.getPassword().isEmpty()) {
            if (!personalSettingDto.getPassword().equals(personalSettingDto.getPasswordConfirm())) {
                log.warn("개인설정 업데이트 실패: 비밀번호 불일치");
                redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
                return "redirect:/account/personal";
            }
        }
        
        try {
            // 계정 업데이트
            accountService.updatePersonalSetting(personalSettingDto);
            
            // 성공 메시지 설정
            log.info("개인설정 업데이트 성공: {}", personalSettingDto.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "개인설정이 성공적으로 업데이트되었습니다.");
            
            return "redirect:/account/personal";
        } catch (IllegalArgumentException e) {
            log.error("개인설정 업데이트 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/account/personal";
        }
    }
}