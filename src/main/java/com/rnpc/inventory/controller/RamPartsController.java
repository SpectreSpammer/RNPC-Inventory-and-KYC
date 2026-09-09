package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RamPartsDto;
import com.rnpc.inventory.entity.RamParts;
import com.rnpc.inventory.service.RamPartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("ram")
public class RamPartsController {

    private final RamPartsService service;

    @Autowired
    public RamPartsController(RamPartsService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/ramParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("ramPartsDto", new RamPartsDto());
        addDropdownOptions(model);
        return "products/ramCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute RamPartsDto ramPartsDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            addDropdownOptions(model);
            return "products/ramCreateParts";
        }

        service.saveComponent(ramPartsDto);
        return "redirect:/computer?category=RAM";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        RamParts part = service.getPartById(id);

        RamPartsDto dto = new RamPartsDto();
        dto.setBrand(part.getBrand());
        dto.setModelName(part.getModelName());
        dto.setType(part.getType());
        dto.setKitCapacity(part.getKitCapacity());
        dto.setModuleConfig(part.getModuleConfig());
        dto.setSpeedMts(part.getSpeedMts());
        dto.setCasLatency(part.getCasLatency());
        dto.setVoltage(part.getVoltage());
        dto.setRgb(part.getRgb());
        dto.setTypicalUse(part.getTypicalUse());
        dto.setPrice(part.getPrice());
        dto.setStocks(part.getStocks());

        model.addAttribute("ramPartsDto", dto);
        model.addAttribute("partId", id);
        model.addAttribute("currentImage", part.getImageFileName());
        addDropdownOptions(model);
        return "products/ramEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute RamPartsDto ramPartsDto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            addDropdownOptions(model);
            return "products/ramEditParts";
        }

        service.updateComponent(id, ramPartsDto);
        return "redirect:/computer?category=RAM";
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("brandOptions", service.getDistinctBrands());
        model.addAttribute("typeOptions", service.getDistinctTypes());
        model.addAttribute("rgbOptions", service.getDistinctRgbOptions());
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/ram/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer?category=RAM";
    }
}
