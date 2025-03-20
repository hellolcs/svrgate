package com.nicednb.svrgate.controller.object;

import com.nicednb.svrgate.dto.ServerObjectDto;
import com.nicednb.svrgate.dto.ZoneObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.ServerObjectService;
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
 * 연동서버 객체 관련 요청을 처리하는 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/object/server")
public class ServerObjectController {

    private final Logger log = LoggerFactory.getLogger(ServerObjectController.class);
    private final ServerObjectService serverObjectService;
    private final ZoneObjectService zoneObjectService;
    private final AccountService accountService;

    @GetMapping
    public String serverObjects(Model model,
                               @RequestParam(value = "searchText", required = false) String searchText,
                               @RequestParam(value = "active", required = false) Boolean active,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("연동서버 객체 페이지 접근");

        // 페이징 및 검색 처리
        Pageable pageable = PageRequest.of(page, size);
        Page<ServerObjectDto> serverObjects = serverObjectService.searchServerObjectsAsDto(searchText, active, pageable);

        // Zone 목록 (드롭다운 선택용)
        List<ZoneObjectDto> allZones = zoneObjectService.findAllZonesForDropdownAsDto();

        model.addAttribute("serverObjects", serverObjects);
        model.addAttribute("allZones", allZones);
        model.addAttribute("searchText", searchText);
        model.addAttribute("active", active);
        model.addAttribute("size", size);
        model.addAttribute("serverObjectDto", new ServerObjectDto());

        // 필터 값 설정
        List<Map<String, Object>> filterValues = new ArrayList<>();
        Map<String, Object> trueOption = new HashMap<>();
        trueOption.put("value", "true");
        trueOption.put("label", "연동");
        filterValues.add(trueOption);

        Map<String, Object> falseOption = new HashMap<>();
        falseOption.put("value", "false");
        falseOption.put("label", "미연동");
        filterValues.add(falseOption);

        model.addAttribute("filterValues", filterValues);

        return "object/server";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ServerObjectDto getServerObjectInfo(@PathVariable("id") Long id) {
        log.info("연동서버 객체 정보 요청: {}", id);
        return serverObjectService.findByIdAsDto(id);
    }

    @PostMapping("/add")
    public String addServerObject(@Valid @ModelAttribute("serverObjectDto") ServerObjectDto serverObjectDto,
                                 BindingResult bindingResult,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        log.info("연동서버 객체 추가 요청: {}", serverObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("연동서버 객체 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/server";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            serverObjectService.createServerObject(serverObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "연동서버 객체가 성공적으로 추가되었습니다.");
            return "redirect:/object/server";
        } catch (Exception e) {
            log.error("연동서버 객체 추가 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "연동서버 객체 추가 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/server";
        }
    }

    @PostMapping("/update")
    public String updateServerObject(@Valid @ModelAttribute("serverObjectDto") ServerObjectDto serverObjectDto,
                                    BindingResult bindingResult,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        log.info("연동서버 객체 수정 요청: {}", serverObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("연동서버 객체 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/server";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            serverObjectService.updateServerObject(serverObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "연동서버 객체가 성공적으로 수정되었습니다.");
            return "redirect:/object/server";
        } catch (Exception e) {
            log.error("연동서버 객체 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "연동서버 객체 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/server";
        }
    }

    @PostMapping("/delete")
    public String deleteServerObject(@RequestParam("id") Long id,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        log.info("연동서버 객체 삭제 요청: {}", id);

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            serverObjectService.deleteServerObject(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "연동서버 객체가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("연동서버 객체 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "연동서버 객체 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/object/server";
    }
    
    /**
     * 서버와 연동(동기화) 수행
     */
    @PostMapping("/sync/{id}")
    public String syncWithServer(@PathVariable("id") Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        log.info("서버 연동 요청: {}", id);

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            serverObjectService.syncWithServer(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "서버 연동이 성공적으로 완료되었습니다.");
        } catch (IllegalStateException e) {
            // 연동이 비활성화된 경우
            log.error("서버 연동 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("서버 연동 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "서버 연동 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/object/server";
    }
}