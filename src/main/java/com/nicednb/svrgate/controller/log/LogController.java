package com.nicednb.svrgate.controller.log;

import com.nicednb.svrgate.entity.OperationHistory;
import com.nicednb.svrgate.repository.OperationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/logs", method = {RequestMethod.GET, RequestMethod.POST})
public class LogController {

    private final OperationHistoryRepository operationHistoryRepository;

    @RequestMapping
    public String listLogs(Model model,
                           @RequestParam(value = "searchText", required = false) String searchText,
                           @RequestParam(value = "logType", required = false) String logType,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "30") int size,
                           @RequestParam(value = "startDate", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                           @RequestParam(value = "endDate", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime now = LocalDateTime.now();
        if (startDate == null) {
            startDate = now.minusDays(1);
        }
        if (endDate == null) {
            endDate = now;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationHistory> logs = operationHistoryRepository.searchOperations(
                searchText, logType, startDate, endDate, pageable
        );

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        String startDateValue = startDate.format(dtf);
        String endDateValue = endDate.format(dtf);

        model.addAttribute("logs", logs);
        model.addAttribute("searchText", searchText);
        model.addAttribute("logType", logType);
        model.addAttribute("startDateValue", startDateValue);
        model.addAttribute("endDateValue", endDateValue);
        model.addAttribute("size", size);
        return "log/list";
    }
}
