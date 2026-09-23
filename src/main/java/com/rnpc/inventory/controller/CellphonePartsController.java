package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CellphonePartView;
import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.CellphonePartsService;
import com.rnpc.inventory.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("cellphone")
public class CellphonePartsController {

    /*
     * Cellphone parts, mid-redesign (batch 2: list page and view model only). The list page is
     * driven by CellphoneParts.PartType, the same way /laptop is, but Add and Edit still point at
     * the existing untyped forms (cellphoneCreateParts.html, cellphoneEditParts.html) - the
     * per-type forms come in the next batch. Every row today has a null part_type (batch 1 added
     * the column without backfilling it), so every part shows as Other until then; see
     * CellphonePartView.typeOf.
     */

    private final CellphonePartsService cellphonePartsService;
    private final NotificationService notificationService;


    @Autowired
    public CellphonePartsController(CellphonePartsService cellphonePartsService, NotificationService notificationService) {
        this.cellphonePartsService = cellphonePartsService;
        this.notificationService = notificationService;
    }

    // Topbar chrome for the shared layout-app shell, same shape as LaptopPartsController's.
    // This route is admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_PARTS_PATHS), so
    // the admin inbox is always the right one and no isAdmin check belongs here.
    private void addShellAttributes(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
    }

    @GetMapping({"", "/"})
    public String showCellphonePartsList(Authentication authentication, Model model){
        // Display-ready rows, serialized into the page as ALL_PARTS - same approach as /laptop.
        // Every label, key spec and unit is decided in CellphonePartView, not in the template.
        model.addAttribute("parts", cellphonePartsService.getAllCellphoneParts().stream()
                .map(CellphonePartView::from).collect(Collectors.toList()));

        // Chip list in enum order, every type shown even at 0 (as on /laptop and /computer).
        List<Map<String, String>> partTypes = new ArrayList<>();
        for (CellphoneParts.PartType t : CellphoneParts.PartType.values()) {
            Map<String, String> chip = new LinkedHashMap<>();
            chip.put("key", t.name());
            chip.put("label", t.getLabel());
            partTypes.add(chip);
        }
        model.addAttribute("partTypes", partTypes);
        // Same low-stock rule as the admin dashboard and /computer, reused rather than restated.
        model.addAttribute("lowStockLimit", AdminDashboardService.LOW_STOCK_LIMIT);
        addShellAttributes(authentication, model);
        return "products/cellphoneParts";
    }

    @PostMapping("/create")
    public String createCellphonePart(@Valid @ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result){
        if (cellphonePartsDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("cellphonePartsDto", "imageFile", "The image file is required!"));
        }

        if (result.hasErrors()) {
            return "products/cellphoneCreateParts";
        }
        cellphonePartsService.saveCellphonePart(cellphonePartsDto);
        return "redirect:/cellphone";
    }

    @GetMapping("/create")
    public String showCreateCellphonePartForm(Model model){
        model.addAttribute("cellphonePartsDto", new CellphonePartsDto());
        return "products/cellphoneCreateParts";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteCellphonePart(@PathVariable ("id") Long id){
        cellphonePartsService.deleteCellphonePart(id);
        return "redirect:/cellphone";
    }

    @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model){
        CellphoneParts product = cellphonePartsService.getCellphonePartById(id);

        CellphonePartsDto cellphonePartsDto = new CellphonePartsDto();
        cellphonePartsDto.setBrand(product.getBrand());
        cellphonePartsDto.setPartName(product.getPartName());
        cellphonePartsDto.setCategory(product.getCategory());
        cellphonePartsDto.setStorageSize(product.getStorageSize());
        cellphonePartsDto.setStocks(product.getStocks());
        cellphonePartsDto.setPrice(product.getPrice());
        cellphonePartsDto.setDescription(product.getDescription());

        model.addAttribute("cellphonePartsDto", cellphonePartsDto);
        model.addAttribute("cellphonePartId", id);
        model.addAttribute("currentImage", product.getImageFileName());

        return "products/cellphoneEditParts";
    }

    @PutMapping("/update/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @Valid @ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result, Model model){
        if (result.hasErrors()){
            model.addAttribute("cellphonePartId", id);
            model.addAttribute("currentImage", cellphonePartsService.getCellphonePartById(id).getImageFileName());
            return "products/cellphoneEditParts";
        }
        cellphonePartsService.updateCellphonePart(id, cellphonePartsDto);
        return "redirect:/cellphone";
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") Long id) {
        cellphonePartsService.removePhoto(id);
        return "redirect:/cellphone/edit/" + id;
    }
}
