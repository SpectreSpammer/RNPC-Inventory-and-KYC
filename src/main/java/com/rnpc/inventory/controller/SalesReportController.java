package com.rnpc.inventory.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnpc.inventory.dto.SalesReportData;
import com.rnpc.inventory.service.SalesReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("sales")
public class SalesReportController {

    private final SalesReportService salesReportService;
    private final ObjectMapper objectMapper;

    @Autowired
    public SalesReportController(SalesReportService salesReportService, ObjectMapper objectMapper) {
        this.salesReportService = salesReportService;
        this.objectMapper = objectMapper;
    }

    // Admin-only, same as every other admin-facing list (see AppointmentController,
    // OrderController) - revenue figures aren't something a customer account should see. The nav
    // link is already hidden for non-admins (see fragments/nav.html); this is the actual guard.
    @GetMapping({"", "/"})
    public String showReport(Authentication authentication, Model model) throws JsonProcessingException {
        if (!isAdmin(authentication)) {
            return "redirect:/";
        }
        SalesReportData data = salesReportService.buildReportData();
        model.addAttribute("salesDataJson", objectMapper.writeValueAsString(data));
        return "sales/salesReport";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
