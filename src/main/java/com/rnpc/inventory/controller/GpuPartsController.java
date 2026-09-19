package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.GpuPartsDto;
import com.rnpc.inventory.entity.GpuParts;
import com.rnpc.inventory.service.GpuPartsService;
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
@RequestMapping("gpu")
public class GpuPartsController {

    private final GpuPartsService service;
    private final NotificationService notificationService;

    @Autowired
    public GpuPartsController(GpuPartsService service,
                                       NotificationService notificationService) {
        this.service = service;
        this.notificationService = notificationService;
    }

    // Topbar chrome for the shared layout-app shell, the same helper CpuPartsController uses.
    // These routes are admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_PARTS_PATHS), so
    // the admin inbox is always the right one and no isAdmin check belongs here. Called from all
    // four paths that render a form, including the two validation-error paths, which re-render the
    // form and would otherwise lose the shell the moment a submit failed.
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
        return "products/gpuParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("gpuPartsDto", new GpuPartsDto());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/gpuCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute GpuPartsDto gpuPartsDto, BindingResult result,
                              Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/gpuCreateParts";
        }

        service.saveComponent(gpuPartsDto);
        return "redirect:/computer?category=GPU";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Authentication authentication, Model model) {
        GpuParts part = service.getPartById(id);

        GpuPartsDto dto = new GpuPartsDto();
        dto.setBrand(part.getBrand());
        dto.setSeries(part.getSeries());
        dto.setModelName(part.getModelName());
        dto.setArchitecture(part.getArchitecture());
        dto.setVram(part.getVram());
        dto.setMemoryType(part.getMemoryType());
        dto.setRecommendedPsuW(part.getRecommendedPsuW());
        dto.setPowerConnector(part.getPowerConnector());
        dto.setTypicalAibPartners(part.getTypicalAibPartners());
        dto.setLaunchYear(part.getLaunchYear());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("gpuPartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/gpuEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute GpuPartsDto gpuPartsDto,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/gpuEditParts";
        }

        service.updateComponent(id, gpuPartsDto);
        return "redirect:/computer?category=GPU";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("memoryTypeOptions", service.getDistinctMemoryTypes());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/gpu/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=GPU";
    }
}
