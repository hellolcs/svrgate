package com.nicednb.svrgate.controller.rule;

import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.dto.SourceObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.PolicyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/rule")
public class PolicyController {

    private final Logger log = LoggerFactory.getLogger(PolicyController.class);
    private final PolicyService policyService;
    private final AccountService accountService;

    /**
     * 정책 목록 페이지
     */
    @GetMapping
    public String policies(Model model) {
        log.info("정책 목록 페이지 접근");
        
        // 서버별 정책 요약 정보 조회
        List<PolicyService.ServerPolicySummary> serverPolicySummaries = policyService.getAllServerPolicySummaries();
        model.addAttribute("serverPolicySummaries", serverPolicySummaries);
        
        return "rule/list";
    }
    
    /**
     * 서버별 정책 목록 조회 (AJAX)
     */
    @GetMapping("/server/{serverId}")
    @ResponseBody
    public List<PolicyDto> getPoliciesByServerId(@PathVariable Long serverId) {
        log.info("서버별 정책 목록 조회: serverId={}", serverId);
        return policyService.getPoliciesByServerId(serverId);
    }
    
    /**
     * 정책 상세 조회 (AJAX)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public PolicyDto getPolicyById(@PathVariable Long id) {
        log.info("정책 상세 조회: id={}", id);
        return policyService.getPolicyById(id);
    }
    
    /**
     * 출발지 객체 검색 (AJAX)
     */
    @GetMapping("/source-objects")
    @ResponseBody
    public List<SourceObjectDto> searchSourceObjects(@RequestParam(required = false) String searchText) {
        log.info("출발지 객체 검색: searchText={}", searchText);
        return policyService.searchSourceObjects(searchText);
    }
    
    /**
     * 정책 추가
     */
    @PostMapping("/add")
    public String addPolicy(@Valid @ModelAttribute("policyDto") PolicyDto policyDto,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        log.info("정책 추가 요청: serverId={}, priority={}", policyDto.getServerObjectId(), policyDto.getPriority());
        
        if (bindingResult.hasErrors()) {
            log.warn("정책 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/rule";
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            policyService.createPolicy(policyDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "정책이 성공적으로 추가되었습니다.");
        } catch (Exception e) {
            log.error("정책 추가 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "정책 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/rule";
    }
    
    /**
     * 정책 수정
     */
    @PostMapping("/update")
    public String updatePolicy(@Valid @ModelAttribute("policyDto") PolicyDto policyDto,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        log.info("정책 수정 요청: id={}, serverId={}, priority={}", policyDto.getId(), policyDto.getServerObjectId(), policyDto.getPriority());
        
        if (bindingResult.hasErrors()) {
            log.warn("정책 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/rule";
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            policyService.updatePolicy(policyDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "정책이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            log.error("정책 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "정책 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/rule";
    }
    
    /**
     * 정책 삭제
     */
    @PostMapping("/delete")
    public String deletePolicy(@RequestParam("id") Long id,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        log.info("정책 삭제 요청: id={}", id);
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            policyService.deletePolicy(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "정책이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("정책 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "정책 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/rule";
    }
}