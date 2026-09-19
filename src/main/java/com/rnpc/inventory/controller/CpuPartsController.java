package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CpuPartsDto;
import com.rnpc.inventory.entity.CpuParts;
import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("cpu")
public class CpuPartsController {

    private final CpuPartsService service;
    private final NotificationService notificationService;

    @Autowired
    public CpuPartsController(CpuPartsService service, NotificationService notificationService) {
        this.service = service;
        this.notificationService = notificationService;
    }

    // Topbar chrome for the shared layout-app shell, same shape as PartsController's /computer.
    // These routes are admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_PARTS_PATHS), so
    // the admin inbox is always the right one and no isAdmin check belongs here. Called from the
    // create and edit handlers, including their validation-error paths, which re-render the form.
    private void addShellAttributes(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/cpuParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("cpuPartsDto", new CpuPartsDto());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/cpuCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute CpuPartsDto cpuPartsDto, BindingResult result,
                              Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/cpuCreateParts";
        }

        service.saveComponent(cpuPartsDto);
        return "redirect:/computer?category=CPU";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Authentication authentication, Model model) {
        CpuParts part = service.getPartById(id);

        CpuPartsDto dto = new CpuPartsDto();
        dto.setBrand(part.getBrand());
        dto.setGeneration(part.getGeneration());
        dto.setModelName(part.getModelName());
        dto.setSocket(part.getSocket());
        dto.setCores(part.getCores());
        dto.setThread(part.getThread());
        dto.setBaseClockGhz(part.getBaseClockGhz());
        dto.setBoostClockGhz(part.getBoostClockGhz());
        dto.setIntegratedGraphics(part.getIntegratedGraphics());
        dto.setMemorySupport(part.getMemorySupport());
        dto.setLaunchYear(part.getLaunchYear());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("cpuPartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/cpuEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute CpuPartsDto cpuPartsDto,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/cpuEditParts";
        }

        service.updateComponent(id, cpuPartsDto);
        return "redirect:/computer?category=CPU";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("socketOptions", service.getDistinctSockets());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/cpu/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=CPU";
    }
}
