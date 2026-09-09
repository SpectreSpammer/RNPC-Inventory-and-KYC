package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.StoragePartsDto;
import com.rnpc.inventory.entity.StorageParts;
import com.rnpc.inventory.service.StoragePartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("storage")
public class StoragePartsController {

    private final StoragePartsService service;

    @Autowired
    public StoragePartsController(StoragePartsService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/storageParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("storagePartsDto", new StoragePartsDto());
        addDropdownOptions(model);
        return "products/storageCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute StoragePartsDto storagePartsDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            return "products/storageCreateParts";
        }

        service.saveComponent(storagePartsDto);
        return "redirect:/computer?category=Storage";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
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
        return "products/storageEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute StoragePartsDto storagePartsDto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
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
