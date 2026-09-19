package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.StoragePartsDto;
import com.rnpc.inventory.entity.StorageParts;
import com.rnpc.inventory.service.StoragePartsService;
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
@RequestMapping("storage")
public class StoragePartsController {

    private final StoragePartsService service;
    private final NotificationService notificationService;

    @Autowired
    public StoragePartsController(StoragePartsService service,
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
        return "products/storageParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("storagePartsDto", new StoragePartsDto());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/storageCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute StoragePartsDto storagePartsDto, BindingResult result,
                              Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/storageCreateParts";
        }

        service.saveComponent(storagePartsDto);
        return "redirect:/computer?category=Storage";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Authentication authentication, Model model) {
        StorageParts part = service.getPartById(id);

        StoragePartsDto dto = new StoragePartsDto();
        dto.setBrand(part.getBrand());
        dto.setModelName(part.getModelName());
        dto.setCategory(part.getCategory());
        dto.setFormFactor(part.getFormFactor());
        dto.setInterfaceType(part.getInterfaceType());
        dto.setCapacities(part.getCapacities());
        dto.setSeqReadMbs(part.getSeqReadMbs());
        dto.setSeqWriteMbs(part.getSeqWriteMbs());
        dto.setDramCache(part.getDramCache());
        dto.setWarranty(part.getWarranty());
        dto.setTypicalUse(part.getTypicalUse());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("storagePartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        addShellAttributes(authentication, model);
        return "products/storageEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute StoragePartsDto storagePartsDto,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            addShellAttributes(authentication, model);
            return "products/storageEditParts";
        }

        service.updateComponent(id, storagePartsDto);
        return "redirect:/computer?category=Storage";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("categoryOptions", service.getDistinctCategories());
        model.addAttribute("formFactorOptions", service.getDistinctFormFactors());
        model.addAttribute("dramCacheOptions", service.getDistinctDramCacheOptions());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/storage/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=Storage";
    }
}
