package com.rnpc.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// TEMPORARY - renders theme.css + layout-app + page-header/card/stat-card/progress-card/
// table-card/status-badge/empty-state/money together with mock data, so the new shell can be
// reviewed in a browser before any real page is converted. Delete this controller and
// templates/preview.html once reviewed. Touches no existing controller/entity/template.
//
// The sidebar's customer-vs-staff menu is gated by the real ${navIsAdmin} global attribute
// (GlobalNavAttributes.java:22-27), same as every other page - this controller does not fake or
// override that. To see the staff menu (3 accordions: Parts/Ticket/KYC - the better test of "one
// section open at a time"), log in as admin/12345 via /login first, then visit /preview. Signed
// out (or signed in as a customer), you'll see the customer menu (1 accordion).
@Controller
public class PreviewController {

    @GetMapping("/preview")
    public String showPreview(Model model) {
        model.addAttribute("currentUsername", "admin");
        model.addAttribute("currentRole", "ADMIN");
        model.addAttribute("unreadNotifications", 3);
        return "preview";
    }
}
