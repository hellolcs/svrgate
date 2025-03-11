package com.nicednb.svrgate.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    @ModelAttribute
    public void addCurrentUri(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
    }
}
