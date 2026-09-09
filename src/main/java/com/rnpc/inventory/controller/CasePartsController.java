package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CasePartsDto;
import com.rnpc.inventory.entity.CaseParts;
import com.rnpc.inventory.service.CasePartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("case")
public class CasePartsController {

    private final CasePartsService service;

    @Autowired
    public CasePartsController(CasePartsService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/caseParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("casePartsDto", new CasePartsDto());
        addDropdownOptions(model);
        return "products/caseCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute CasePartsDto casePartsDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            return "products/caseCreateParts";
        }

        service.saveComponent(casePartsDto);
        return "redirect:/computer?category=Case";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        CaseParts part = service.getPartById(id);

        CasePartsDto dto = new CasePartsDto();
        dto.setBrand(part.getBrand());
        dto.setModelName(part.getModelName());
        dto.setTowerClass(part.getTowerClass());
        dto.setMotherboardSupport(part.getMotherboardSupport());
        dto.setMaxGpuLengthMm(part.getMaxGpuLengthMm());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("casePartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        return "products/caseEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute CasePartsDto casePartsDto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            return "products/caseEditParts";
        }

        service.updateComponent(id, casePartsDto);
        return "redirect:/computer?category=Case";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("towerClassOptions", service.getDistinctTowerClasses());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/case/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=Case";
    }
}
