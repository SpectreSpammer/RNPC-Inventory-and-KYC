package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.service.CellphonePartsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("cellphone")
public class CellphonePartsController {

    private final CellphonePartsService cellphonePartsService;

    @Autowired
    public CellphonePartsController(CellphonePartsService cellphonePartsService) {
        this.cellphonePartsService = cellphonePartsService;
    }

    @GetMapping({"", "/"})
    public String showLaptopPartsList(Model model){
        model.addAttribute("cellphone", cellphonePartsService.getAllCellphoneParts());
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

        return "products/cellphoneEditParts";
    }

    @PutMapping("/update/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @Valid @ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result, Model model){
        if (result.hasErrors()){
            model.addAttribute("cellphonePartId", id);
            return "products/cellphoneEditParts";
        }
        cellphonePartsService.updateCellphonePart(id, cellphonePartsDto);
        return "redirect:/cellphone";
    }
}
