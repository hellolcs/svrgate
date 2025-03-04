package com.nicednb.svrgate.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUri")
    public String populateCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
