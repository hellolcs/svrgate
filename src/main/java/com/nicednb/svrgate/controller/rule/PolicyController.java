package com.nicednb.svrgate.controller.rule;

import com.nicednb.svrgate.dto.PolicyDto;
import com.nicednb.svrgate.dto.SourceObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.PolicyService;
import com.nicednb.svrgate.service.PolicyService.PolicyOperationException;
import com.nicednb.svrgate.service.PolicyService.PolicyOperationResult;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<PolicyDto> getPoliciesByServerId(@PathVariable("serverId") Long serverId) {
        log.info("서버별 정책 목록 조회: serverId={}", serverId);
        return policyService.getPoliciesByServerId(serverId);
    }
    
    /**
     * 정책 상세 조회 (AJAX)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public PolicyDto getPolicyById(@PathVariable("id") Long id) {
        log.info("정책 상세 조회: id={}", id);
        return policyService.getPolicyById(id);
    }
    
    /**
     * 출발지 객체 검색 (AJAX)
     */
    @GetMapping("/source-objects")
    @ResponseBody
    public List<SourceObjectDto> searchSourceObjects(@RequestParam(value = "searchText", required = false) String searchText) {
        log.info("출발지 객체 검색: searchText={}", searchText);
        return policyService.searchSourceObjects(searchText);
    }
    
    /**
     * 정책 추가 (JSON 응답 추가)
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addPolicy(@Valid @ModelAttribute("policyDto") PolicyDto policyDto,
                           BindingResult bindingResult,
                           HttpServletRequest request) {
        log.info("정책 추가 요청: serverId={}, priority={}", policyDto.getServerObjectId(), policyDto.getPriority());
        
        Map<String, Object> response = new HashMap<>();
        
        // 유효성 검증 실패 시 오류 응답 반환
        if (bindingResult.hasErrors()) {
            log.warn("정책 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            response.put("success", false);
            response.put("message", "입력 값을 확인해주세요.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            
            // 정책 추가 (방화벽 API 호출 포함)
            PolicyOperationResult result = policyService.createPolicy(policyDto, ipAddress);
            
            // API 응답 결과에 따라 처리
            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("policy", result.getPolicyDto());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
                return ResponseEntity.ok(response);
            }
        } catch (PolicyOperationException e) {
            log.error("정책 추가 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("정책 추가 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "정책 추가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 정책 수정 (JSON 응답 추가)
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updatePolicy(@Valid @ModelAttribute("policyDto") PolicyDto policyDto,
                              BindingResult bindingResult,
                              HttpServletRequest request) {
        log.info("정책 수정 요청: id={}, serverId={}, priority={}", policyDto.getId(), policyDto.getServerObjectId(), policyDto.getPriority());
        
        Map<String, Object> response = new HashMap<>();
        
        // 유효성 검증 실패 시 오류 응답 반환
        if (bindingResult.hasErrors()) {
            log.warn("정책 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            response.put("success", false);
            response.put("message", "입력 값을 확인해주세요.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            
            // 정책 수정 (요청자와 설명만 업데이트)
            PolicyDto updatedPolicy = policyService.updatePolicy(policyDto, ipAddress);
            
            response.put("success", true);
            response.put("message", "정책이 성공적으로 수정되었습니다.");
            response.put("policy", updatedPolicy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 수정 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "정책 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 정책 삭제 (JSON 응답 추가)
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deletePolicy(@RequestParam("id") Long id,
                              HttpServletRequest request) {
        log.info("정책 삭제 요청: id={}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            
            // 정책 삭제 (방화벽 API 호출 포함)
            PolicyOperationResult result = policyService.deletePolicy(id, ipAddress);
            
            // API 응답 결과에 따라 처리
            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
                return ResponseEntity.ok(response);
            }
        } catch (PolicyOperationException e) {
            log.error("정책 삭제 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("정책 삭제 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "정책 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}