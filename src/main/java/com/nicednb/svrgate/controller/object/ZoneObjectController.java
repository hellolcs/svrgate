package com.nicednb.svrgate.controller.object;

import com.nicednb.svrgate.dto.ZoneDto;
import com.nicednb.svrgate.entity.Zone;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.ZoneService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Zone 관련 요청을 처리하는 컨트롤러
 * Zone 조회, 추가, 수정, 삭제 등의 기능 처리
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/object/zone")
public class ZoneObjectController {

    private final Logger log = LoggerFactory.getLogger(ZoneObjectController.class);
    private final ZoneService zoneService;
    private final AccountService accountService;

    /**
     * Zone 목록 페이지
     */
    @GetMapping
    public String zoneObjects(Model model,
                            @RequestParam(value = "searchText", required = false) String searchText,
                            @RequestParam(value = "active", required = false) Boolean active,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("Zone 페이지 접근");
        
        // 페이징 및 검색 처리
        Pageable pageable = PageRequest.of(page, size);
        Page<Zone> zones = zoneService.searchZones(searchText, active, pageable);
        
        // 활성화된 Zone 목록 (드롭다운 선택용)
        List<Zone> activeZones = zoneService.findActiveZones();
        
        model.addAttribute("zones", zones);
        model.addAttribute("activeZones", activeZones);
        model.addAttribute("searchText", searchText);
        model.addAttribute("active", active);
        model.addAttribute("size", size);
        model.addAttribute("zoneDto", new ZoneDto());
        
        return "object/zone";
    }
    
    /**
     * Zone 정보 가져오기 (Ajax)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ZoneDto getZoneInfo(@PathVariable("id") Long id) {
        log.info("Zone 정보 요청: {}", id);
        Zone zone = zoneService.findById(id);
        ZoneDto dto = zoneService.convertToDto(zone);
        return dto;
    }
    
    /**
     * Zone 추가
     */
    @PostMapping("/add")
    public String addZone(@Valid @ModelAttribute("zoneDto") ZoneDto zoneDto,
                         BindingResult bindingResult,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        log.info("Zone 추가 요청: {}", zoneDto.getName());
        
        if (bindingResult.hasErrors()) {
            log.warn("Zone 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/zone";
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            Zone zone = zoneService.createZone(zoneDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "Zone이 성공적으로 추가되었습니다.");
            return "redirect:/object/zone";
        } catch (Exception e) {
            log.error("Zone 추가 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Zone 추가 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/zone";
        }
    }
    
    /**
     * Zone 수정
     */
    @PostMapping("/update")
    public String updateZone(@Valid @ModelAttribute("zoneDto") ZoneDto zoneDto,
                            BindingResult bindingResult,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        log.info("Zone 수정 요청: {}", zoneDto.getName());
        
        if (bindingResult.hasErrors()) {
            log.warn("Zone 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/zone";
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            Zone zone = zoneService.updateZone(zoneDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "Zone이 성공적으로 수정되었습니다.");
            return "redirect:/object/zone";
        } catch (Exception e) {
            log.error("Zone 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Zone 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/zone";
        }
    }
    
    /**
     * Zone 삭제
     */
    @PostMapping("/delete")
    public String deleteZone(@RequestParam("id") Long id,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        log.info("Zone 삭제 요청: {}", id);
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            zoneService.deleteZone(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "Zone이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Zone 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Zone 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/object/zone";
    }
    
    /**
     * Excel 파일 업로드 (TODO: 실제 구현 필요)
     */
    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        log.info("Excel 파일 업로드 요청");
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "파일을 선택해주세요.");
            return "redirect:/object/zone";
        }
        
        try {
            String ipAddress = accountService.getClientIpAddress(request);
            zoneService.processExcelUpload(file.getBytes(), ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "Excel 파일이 성공적으로 처리되었습니다.");
        } catch (Exception e) {
            log.error("Excel 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/object/zone";
    }
    
    /**
     * 방화벽 동기화 (TODO: 실제 구현 필요)
     */
    @PostMapping("/sync/{id}")
    public String syncWithFirewall(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("방화벽 동기화 요청: {}", id);
        
        try {
            zoneService.syncWithFirewall(id);
            redirectAttributes.addFlashAttribute("successMessage", "방화벽과 동기화가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("방화벽 동기화 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "방화벽 동기화 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/object/zone";
    }
}