package com.rnpc.inventory.controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.service.LaptopPartsService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("laptop")
public class LaptopPartsController {

	private final LaptopPartsService laptopPartsService;

	@Autowired
	public LaptopPartsController(LaptopPartsService laptopPartsService) {
		this.laptopPartsService = laptopPartsService;
	}

	@GetMapping
	public String showLaptopPartsList(Model model) {
		model.addAttribute("laptop", laptopPartsService.getAllLaptopParts());
		return "products/laptopParts";
	}

	@PostMapping("/create")
	public String createLaptopPart(@ModelAttribute LaptopPartsDto laptopPartsDto) {
		laptopPartsService.saveLaptopPart(laptopPartsDto);
		return "redirect:/laptop";
	}

	@GetMapping("/create")
	public String showCreateLaptopPartForm(Model model) {
		model.addAttribute("laptopPartsDto", new LaptopPartsDto());
		return "products/laptopCreateParts";
	}

	@DeleteMapping("/delete/{id}")
	public String deleteLaptopPart(@PathVariable("id") int id) {
		laptopPartsService.deleteLaptopPart(id);
		return "redirect:/laptop";
	}

	// Show edit page for a specific laptop
	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Model model) {
		LaptopParts product = laptopPartsService.getLaptopPartById(id);

		LaptopPartsDto laptopPartsDto = new LaptopPartsDto();
		laptopPartsDto.setBrand(product.getBrand());
		laptopPartsDto.setPartName(product.getPartName());
		laptopPartsDto.setCategory(product.getCategory());
		laptopPartsDto.setStorageSize(product.getStorageSize());
		laptopPartsDto.setStocks(product.getStocks());
		laptopPartsDto.setPrice(product.getPrice());
		laptopPartsDto.setDescription(product.getDescription());

		model.addAttribute("laptopPartsDto", laptopPartsDto); // Changed here
		model.addAttribute("productId", id);
		return "products/laptopEditParts";
	}


	// Update the laptop details
	@PostMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id,
								@Valid @ModelAttribute LaptopPartsDto laptopDto,
								BindingResult result,
								Model model) {
		if (result.hasErrors()) {
			model.addAttribute("productId", id);
			return "products/laptopEditParts";
		}

		laptopPartsService.updateLaptopPart(id, laptopDto);
		return "redirect:/laptop";
	}
}
