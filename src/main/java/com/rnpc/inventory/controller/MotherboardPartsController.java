package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.MotherboardPartsDto;
import com.rnpc.inventory.entity.MotherboardParts;
import com.rnpc.inventory.service.MotherboardPartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("motherboard")
public class MotherboardPartsController {

    private final MotherboardPartsService service;

    @Autowired
    public MotherboardPartsController(MotherboardPartsService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/motherboardParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("motherboardPartsDto", new MotherboardPartsDto());
        addDropdownOptions(model);
        return "products/motherboardCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute MotherboardPartsDto motherboardPartsDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            return "products/motherboardCreateParts";
        }

        service.saveComponent(motherboardPartsDto);
        return "redirect:/computer?category=Motherboard";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        MotherboardParts part = service.getPartById(id);

        MotherboardPartsDto dto = new MotherboardPartsDto();
        dto.setBrand(part.getBrand());
        dto.setModelName(part.getModelName());
        dto.setChipset(part.getChipset());
        dto.setSocket(part.getSocket());
        dto.setCpuPlatform(part.getCpuPlatform());
        dto.setFormFactor(part.getFormFactor());
        dto.setMemoryType(part.getMemoryType());
        dto.setRamSlots(part.getRamSlots());
        dto.setMaxRam(part.getMaxRam());
        dto.setM2Slots(part.getM2Slots());
        dto.setPcieVersion(part.getPcieVersion());
        dto.setNotableFeatures(part.getNotableFeatures());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("motherboardPartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        return "products/motherboardEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute MotherboardPartsDto motherboardPartsDto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            return "products/motherboardEditParts";
        }

        service.updateComponent(id, motherboardPartsDto);
        return "redirect:/computer?category=Motherboard";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("socketOptions", service.getDistinctSockets());
        model.addAttribute("formFactorOptions", service.getDistinctFormFactors());
        model.addAttribute("memoryTypeOptions", service.getDistinctMemoryTypes());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/motherboard/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=Motherboard";
    }
}
