package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CoolerPartsDto;
import com.rnpc.inventory.entity.CoolerParts;
import com.rnpc.inventory.service.CoolerPartsService;
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
@RequestMapping("cooler")
public class CoolerPartsController {

    private final CoolerPartsService service;
    private final NotificationService notificationService;

    @Autowired
    public CoolerPartsController(CoolerPartsService service,
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
        return "products/coolerParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("coolerPartsDto", new CoolerPartsDto());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/coolerCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute CoolerPartsDto coolerPartsDto, BindingResult result,
                              Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/coolerCreateParts";
        }

        service.saveComponent(coolerPartsDto);
        return "redirect:/computer?category=CPU%20Cooler";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Authentication authentication, Model model) {
        CoolerParts part = service.getPartById(id);

        CoolerPartsDto dto = new CoolerPartsDto();
        dto.setBrand(part.getBrand());
        dto.setModelName(part.getModelName());
        dto.setType(part.getType());
        dto.setRadiatorHeight(part.getRadiatorHeight());
        dto.setSocketSupport(part.getSocketSupport());
        dto.setRgb(part.getRgb());
        dto.setNotes(part.getNotes());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("coolerPartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/coolerEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute CoolerPartsDto coolerPartsDto,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/coolerEditParts";
        }

        service.updateComponent(id, coolerPartsDto);
        return "redirect:/computer?category=CPU%20Cooler";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("typeOptions", service.getDistinctTypes());
        model.addAttribute("rgbOptions", service.getDistinctRgbOptions());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/cooler/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=CPU%20Cooler";
    }
}
