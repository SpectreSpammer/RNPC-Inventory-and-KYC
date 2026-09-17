package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.PartsListView;
import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.CasePartsService;
import com.rnpc.inventory.service.CoolerPartsService;
import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.GpuPartsService;
import com.rnpc.inventory.service.MotherboardPartsService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.PsuPartsService;
import com.rnpc.inventory.service.RamPartsService;
import com.rnpc.inventory.service.StoragePartsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PartsController {

    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = Map.of(
            "CPU", "CPU",
            "GPU", "GPU",
            "MOTHERBOARD", "Motherboard",
            "RAM", "RAM",
            "STORAGE", "Storage",
            "PSU", "PSU",
            "CASE", "Case",
            "COOLER", "CPU Cooler"
    );

    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;
    private final NotificationService notificationService;

    @Autowired
    public PartsController(CpuPartsService cpuService, GpuPartsService gpuService,
                            MotherboardPartsService motherboardService, RamPartsService ramService,
                            StoragePartsService storageService, PsuPartsService psuService,
                            CasePartsService caseService, CoolerPartsService coolerService,
                            NotificationService notificationService) {
        this.cpuService = cpuService;
        this.gpuService = gpuService;
        this.motherboardService = motherboardService;
        this.ramService = ramService;
        this.storageService = storageService;
        this.psuService = psuService;
        this.caseService = caseService;
        this.coolerService = coolerService;
        this.notificationService = notificationService;
    }

    // Admin-only, enforced in the filter chain rather than here - see
    // SecurityConfig.ADMIN_ONLY_PARTS_PATHS. There is deliberately no isAdmin() check in this
    // class; by the time this method runs the request has already been authorised, so the topbar
    // can be populated from the admin inbox unconditionally.
    @GetMapping("/computer")
    public String showPartsList(Authentication authentication, Model model) {
        List<BuildPartView> allParts = new ArrayList<>();
        allParts.addAll(cpuService.getAllAsBuildParts());
        allParts.addAll(gpuService.getAllAsBuildParts());
        allParts.addAll(motherboardService.getAllAsBuildParts());
        allParts.addAll(ramService.getAllAsBuildParts());
        allParts.addAll(storageService.getAllAsBuildParts());
        allParts.addAll(psuService.getAllAsBuildParts());
        allParts.addAll(caseService.getAllAsBuildParts());
        allParts.addAll(coolerService.getAllAsBuildParts());

        List<PartsListView> parts = new ArrayList<>();
        for (BuildPartView p : allParts) {
            parts.add(toListView(p));
        }

        model.addAttribute("parts", parts);
        model.addAttribute("categories", CATEGORY_DISPLAY_NAMES.values());

        // Same low-stock rule the admin dashboard already applies, reused rather than restated so
        // "5 or fewer" can never mean two different things in two places.
        model.addAttribute("lowStockLimit", AdminDashboardService.LOW_STOCK_LIMIT);

        // Topbar chrome for the shared layout-app shell. This route is admin-only, so the admin
        // inbox is always the right one - same shape as OrderController/SupportController.
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));

        return "products/allParts";
    }

    private PartsListView toListView(BuildPartView p) {
        String routePrefix = p.getCategory().toLowerCase();
        String displayName = CATEGORY_DISPLAY_NAMES.getOrDefault(p.getCategory(), p.getCategory());
        return new PartsListView(p.getComponentId(), displayName, routePrefix, p.getBrand(), p.getModelName(),
                p.getPrice(), p.getStocks(), p.getImageFileName(), p.getFields());
    }
}
