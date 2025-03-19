package com.nicednb.svrgate.controller.object;

import com.nicednb.svrgate.dto.NetworkObjectDto;
import com.nicednb.svrgate.dto.ZoneObjectDto;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.NetworkObjectService;
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
@RequestMapping("/object/network")
public class NetworkObjectController {

    private final Logger log = LoggerFactory.getLogger(NetworkObjectController.class);
    private final NetworkObjectService networkObjectService;
    private final ZoneObjectService zoneObjectService;
    private final AccountService accountService;

    @GetMapping
    public String networkObjects(Model model,
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("네트워크 객체 페이지 접근");

        // 페이징 및 검색 처리 - DTO 사용
        Pageable pageable = PageRequest.of(page, size);
        Page<NetworkObjectDto> networkObjects = networkObjectService.searchNetworkObjectsAsDto(searchText, pageable);

        // Zone 목록 (드롭다운 선택용) - DTO 사용
        List<ZoneObjectDto> allZones = zoneObjectService.findAllZonesForDropdownAsDto();

        model.addAttribute("networkObjects", networkObjects);
        model.addAttribute("allZones", allZones);
        model.addAttribute("searchText", searchText);
        model.addAttribute("size", size);
        model.addAttribute("networkObjectDto", new NetworkObjectDto());

        return "object/network";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public NetworkObjectDto getNetworkObjectInfo(@PathVariable("id") Long id) {
        log.info("네트워크 객체 정보 요청: {}", id);
        return networkObjectService.findByIdAsDto(id);
    }

    @PostMapping("/add")
    public String addNetworkObject(@Valid @ModelAttribute("networkObjectDto") NetworkObjectDto networkObjectDto,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.info("네트워크 객체 추가 요청: {}", networkObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("네트워크 객체 추가 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/network";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            networkObjectService.createNetworkObject(networkObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "네트워크 객체가 성공적으로 추가되었습니다.");
            return "redirect:/object/network";
        } catch (Exception e) {
            log.error("네트워크 객체 추가 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "네트워크 객체 추가 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/network";
        }
    }

    @PostMapping("/update")
    public String updateNetworkObject(@Valid @ModelAttribute("networkObjectDto") NetworkObjectDto networkObjectDto,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.info("네트워크 객체 수정 요청: {}", networkObjectDto.getName());

        if (bindingResult.hasErrors()) {
            log.warn("네트워크 객체 수정 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값을 확인해주세요.");
            return "redirect:/object/network";
        }

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            networkObjectService.updateNetworkObject(networkObjectDto, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "네트워크 객체가 성공적으로 수정되었습니다.");
            return "redirect:/object/network";
        } catch (Exception e) {
            log.error("네트워크 객체 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "네트워크 객체 수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/object/network";
        }
    }

    @PostMapping("/delete")
    public String deleteNetworkObject(@RequestParam("id") Long id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.info("네트워크 객체 삭제 요청: {}", id);

        try {
            String ipAddress = accountService.getClientIpAddress(request);
            networkObjectService.deleteNetworkObject(id, ipAddress);
            redirectAttributes.addFlashAttribute("successMessage", "네트워크 객체가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("네트워크 객체 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "네트워크 객체 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/object/network";
    }
}