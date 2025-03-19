package com.nicednb.svrgate.controller.object;

import com.nicednb.svrgate.dto.GeneralObjectDto;
import com.nicednb.svrgate.dto.ZoneObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.GeneralObjectService;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/object/general")
public class GeneralObjectController {

    private final Logger log = LoggerFactory.getLogger(GeneralObjectController.class);
    private final GeneralObjectService generalObjectService;
    private final ZoneObjectService zoneObjectService;
    private final AccountService accountService;

    @GetMapping
    public String generalObjects(Model model,
                                @RequestParam(value = "searchText", required = false) String searchText,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("일반 객체 페이지 접근");

        // 페이징 및 검색 처리 - DTO 사용
        Pageable pageable = PageRequest.of(page, size);
        Page<GeneralObjectDto> generalObjects = generalObjectService.searchGeneralObjectsAsDto(searchText, pageable);

        // Zone 목록 (드롭다운 선택용) - DTO 사용
        List<ZoneObjectDto> allZones = zoneObjectService.findAllZonesForDropdownAsDto();

        model.addAttribute("generalObjects", generalObjects);
        model.addAttribute("allZones", allZones);
        model.addAttribute("searchText", searchText);
        model.addAttribute("size", size);
        model.addAttribute("generalObjectDto", new GeneralObjectDto());

        return "object/general";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public GeneralObjectDto getGeneralObjectInfo(@PathVariable("id") Long id) {
        log.info("일반 객체 정보 요청: {}", id);
        return generalObjectService.findByIdAsDto(id);
    }

    @PostMapping("/add")
    public String addGeneralObject(@Valid @ModelAttribute("generalObjectDto") GeneralObjectDto generalObjectDto,
                                  BindingResult bindingResult,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        log.info("일반 객체 추가 요청: {}", generalObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("일반 객체 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/general";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            generalObjectService.createGeneralObject(generalObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "일반 객체가 성공적으로 추가되었습니다.");
            return "redirect:/object/general";
        } catch (Exception e) {
            log.error("일반 객체 추가 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일반 객체 추가 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/general";
        }
    }

    @PostMapping("/update")
    public String updateGeneralObject(@Valid @ModelAttribute("generalObjectDto") GeneralObjectDto generalObjectDto,
                                     BindingResult bindingResult,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.info("일반 객체 수정 요청: {}", generalObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("일반 객체 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/general";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            generalObjectService.updateGeneralObject(generalObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "일반 객체가 성공적으로 수정되었습니다.");
            return "redirect:/object/general";
        } catch (Exception e) {
            log.error("일반 객체 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일반 객체 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/general";
        }
    }

    @PostMapping("/delete")
    public String deleteGeneralObject(@RequestParam("id") Long id,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.info("일반 객체 삭제 요청: {}", id);

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            generalObjectService.deleteGeneralObject(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "일반 객체가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("일반 객체 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "일반 객체 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/object/general";
    }
}