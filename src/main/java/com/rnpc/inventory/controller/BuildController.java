package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.CasePartsService;
import com.rnpc.inventory.service.CoolerPartsService;
import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.GpuPartsService;
import com.rnpc.inventory.service.MotherboardPartsService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.PsuPartsService;
import com.rnpc.inventory.service.RamPartsService;
import com.rnpc.inventory.service.StoragePartsService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BuildController {

    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public BuildController(CpuPartsService cpuService, GpuPartsService gpuService,
                            MotherboardPartsService motherboardService, RamPartsService ramService,
                            StoragePartsService storageService, PsuPartsService psuService,
                            CasePartsService caseService, CoolerPartsService coolerService,
                            UserService userService, NotificationService notificationService) {
        this.cpuService = cpuService;
        this.gpuService = gpuService;
        this.motherboardService = motherboardService;
        this.ramService = ramService;
        this.storageService = storageService;
        this.psuService = psuService;
        this.caseService = caseService;
        this.coolerService = coolerService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/build")
    public String showBuildPage(Authentication authentication, Model model) {
        List<BuildPartView> parts = new ArrayList<>();
        parts.addAll(cpuService.getAllAsBuildParts());
        parts.addAll(gpuService.getAllAsBuildParts());
        parts.addAll(motherboardService.getAllAsBuildParts());
        parts.addAll(ramService.getAllAsBuildParts());
        parts.addAll(storageService.getAllAsBuildParts());
        parts.addAll(psuService.getAllAsBuildParts());
        parts.addAll(caseService.getAllAsBuildParts());
        parts.addAll(coolerService.getAllAsBuildParts());

        model.addAttribute("components", parts);

        // Shared layout-app chrome (sidebar/topbar) needs these three - same resolution as
        // DashboardController.showHome, guarded for anonymous since this page (unlike the
        // dashboard) is open to guests too: SecurityConfig.java:49 permits /build, and Order Now
        // already prompts for sign-in only at checkout time (see buildPc.html's authModal flow).
        if (isSignedIn(authentication)) {
            String username = authentication.getName();
            model.addAttribute("currentUsername", resolveDisplayName(authentication, username));
            model.addAttribute("currentRole", isAdmin(authentication) ? "Admin" : "Customer");
            model.addAttribute("unreadNotifications", notificationService.getUnreadCountForUser(username));
            // Topbar notification dropdown - NotificationService.java:55-57 (getForUser) already
            // returns newest-first; just trims to a short list for the dropdown, no new query.
            model.addAttribute("recentNotifications",
                    notificationService.getForUser(username).stream().limit(15).collect(Collectors.toList()));
        }

        return "build/buildPc";
    }

    private String resolveDisplayName(Authentication authentication, String username) {
        String displayName = username;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String givenName = oAuth2User.getAttribute("given_name");
            if (givenName != null && !givenName.isBlank()) {
                displayName = givenName;
            }
        }
        String profileName = userService.findByUsername(username).map(User::getFullName).orElse(null);
        if (profileName != null && !profileName.isBlank()) {
            displayName = profileName;
        }
        return displayName;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
