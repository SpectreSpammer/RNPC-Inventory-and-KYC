package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CoolerPartsDto;
import com.rnpc.inventory.entity.CoolerParts;
import com.rnpc.inventory.service.CoolerPartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("cooler")
public class CoolerPartsController {

    private final CoolerPartsService service;

    @Autowired
    public CoolerPartsController(CoolerPartsService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public String showPartsList(Model model) {
        model.addAttribute("parts", service.getAllParts());
        return "products/coolerParts";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("coolerPartsDto", new CoolerPartsDto());
        return "products/coolerCreateParts";
    }

    @PostMapping("/create")
    public String createPart(@Valid @ModelAttribute CoolerPartsDto coolerPartsDto, BindingResult result) {
        if (result.hasErrors()) {
            return "products/coolerCreateParts";
        }

        service.saveComponent(coolerPartsDto);
        return "redirect:/computer";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
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
        return "products/coolerEditParts";
    }

    @PutMapping("/update/{id}")
    public String updatePart(@PathVariable("id") int id,
                              @Valid @ModelAttribute CoolerPartsDto coolerPartsDto,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partId", id);
            model.addAttribute("currentImage", service.getPartById(id).getImageFileName());
            return "products/coolerEditParts";
        }

        service.updateComponent(id, coolerPartsDto);
        return "redirect:/computer";
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") int id) {
        service.removePhoto(id);
        return "redirect:/cooler/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deletePart(@PathVariable("id") int id) {
        service.deleteComponent(id);
        return "redirect:/computer";
    }
}
