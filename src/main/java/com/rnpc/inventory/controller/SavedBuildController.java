package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.SavedBuildItemView;
import com.rnpc.inventory.dto.SavedBuildLoadResult;
import com.rnpc.inventory.entity.SavedBuild;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.SavedBuildService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("build/my-builds")
public class SavedBuildController {

    private final SavedBuildService savedBuildService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public SavedBuildController(SavedBuildService savedBuildService, UserService userService,
                                 NotificationService notificationService) {
        this.savedBuildService = savedBuildService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    // Saved Builds is a signed-in-only workspace (unlike /build itself, which stays open to
    // guests) - there's no Client-guest concept for it the way order checkout has one, since a
    // save always ties to a real User account. An anonymous visitor is bounced to /build rather
    // than shown an empty list.
    @GetMapping({"", "/"})
    public String showSavedBuilds(Authentication authentication, Model model) {
        if (!isSignedIn(authentication)) {
            return "redirect:/build";
        }
        String username = authentication.getName();
        List<SavedBuild> builds = savedBuildService.getSavedBuildsForUser(username);

        Map<Long, LinkedHashMap<String, SavedBuildItemView>> specRowsByBuild = new LinkedHashMap<>();
        Map<Long, String> caseImageByBuild = new LinkedHashMap<>();
        for (SavedBuild build : builds) {
            specRowsByBuild.put(build.getSavedBuildId(), savedBuildService.specRows(build));
            caseImageByBuild.put(build.getSavedBuildId(), savedBuildService.getCaseImageFileName(build));
        }

        model.addAttribute("builds", builds);
        model.addAttribute("specRowsByBuild", specRowsByBuild);
        model.addAttribute("caseImageByBuild", caseImageByBuild);
        model.addAttribute("currentUsername", username);
        model.addAttribute("currentRole", isAdmin(authentication) ? "Admin" : "Customer");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForUser(username));
        // Topbar notification dropdown - NotificationService.java:55-57 (getForUser) already
        // returns newest-first; just trims to a short list for the dropdown, no new query.
        model.addAttribute("recentNotifications",
                notificationService.getForUser(username).stream().limit(15).collect(Collectors.toList()));
        return "build/savedBuilds";
    }

    @PostMapping
    public String createSavedBuild(@RequestParam(value = "buildName", required = false) String buildName,
                                    @RequestParam(required = false) Integer cpuId,
                                    @RequestParam(required = false) Integer gpuId,
                                    @RequestParam(required = false) Integer motherboardId,
                                    @RequestParam(required = false) Integer ramId,
                                    @RequestParam(required = false) Integer storageSsdId,
                                    @RequestParam(required = false) Integer storageHddId,
                                    @RequestParam(required = false) Integer psuId,
                                    @RequestParam(required = false) Integer caseId,
                                    @RequestParam(required = false) Integer coolerId,
                                    Authentication authentication) {
        if (!isSignedIn(authentication)) {
            return "redirect:/build";
        }

        Map<String, Integer> selections = buildSelections(cpuId, gpuId, motherboardId, ramId,
                storageSsdId, storageHddId, psuId, caseId, coolerId);
        if (selections.isEmpty()) {
            return "redirect:/build";
        }

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Signed-in user not found: " + authentication.getName()));
        savedBuildService.createSavedBuild(user, buildName, selections);
        return "redirect:/build/my-builds";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteSavedBuild(@PathVariable("id") Long id, Authentication authentication) {
        if (!isSignedIn(authentication)) {
            return ResponseEntity.status(403).build();
        }
        try {
            savedBuildService.deleteSavedBuild(id, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(403).build();
        }
    }

    // Plain form POST (not fetch) - same convention as createSavedBuild - since it's a full-page
    // action triggered from a card button, not something the page needs to react to inline.
    @PostMapping("/{id}/duplicate")
    public String duplicateSavedBuild(@PathVariable("id") Long id, Authentication authentication) {
        if (!isSignedIn(authentication)) {
            return "redirect:/build";
        }
        try {
            savedBuildService.duplicateSavedBuild(id, authentication.getName());
        } catch (IllegalArgumentException ex) {
            // Not this user's build - silently ignored, same as deleteSavedBuild's 403 case but
            // without a JSON response to return since this is a plain form POST.
        }
        return "redirect:/build/my-builds";
    }

    // Fetched via JS from buildPc.html (?loadBuildId=X) to repopulate the picker's `selected`
    // object against the live catalog it already has loaded (ALL_PARTS) - see chat for why this
    // only hands back category/componentId pairs rather than full part objects.
    @GetMapping("/{id}/load")
    @ResponseBody
    public ResponseEntity<SavedBuildLoadResult> loadSavedBuild(@PathVariable("id") Long id, Authentication authentication) {
        if (!isSignedIn(authentication)) {
            return ResponseEntity.status(403).build();
        }
        SavedBuild build = savedBuildService.getSavedBuildById(id);
        if (!build.getUser().getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(savedBuildService.loadSelections(build));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Map<String, Integer> buildSelections(Integer cpuId, Integer gpuId, Integer motherboardId,
                                                  Integer ramId, Integer storageSsdId, Integer storageHddId,
                                                  Integer psuId, Integer caseId, Integer coolerId) {
        LinkedHashMap<String, Integer> selections = new LinkedHashMap<>();
        if (cpuId != null) selections.put("CPU", cpuId);
        if (gpuId != null) selections.put("GPU", gpuId);
        if (motherboardId != null) selections.put("MOTHERBOARD", motherboardId);
        if (ramId != null) selections.put("RAM", ramId);
        if (storageSsdId != null) selections.put("STORAGE_SSD", storageSsdId);
        if (storageHddId != null) selections.put("STORAGE_HDD", storageHddId);
        if (psuId != null) selections.put("PSU", psuId);
        if (caseId != null) selections.put("CASE", caseId);
        if (coolerId != null) selections.put("COOLER", coolerId);
        return selections;
    }
}
