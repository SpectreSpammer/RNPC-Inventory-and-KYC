package com.rnpc.inventory.controller.ComputerPartsController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import com.rnpc.inventory.dto.ComputerDto;
import com.rnpc.inventory.repository.ComputerPartsRepository;
import com.rnpc.inventory.service.ComputerPartsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.entity.Computers;

import jakarta.validation.Valid;

@Controller
@RequestMapping("computer")
public class ComputerPartsController {

	private final ComputerPartsService service;

	@Autowired
	public ComputerPartsController(ComputerPartsService service) {
		this.service = service;
	}

	@GetMapping({"", "/"})
	public String showProductList(Model model) {
		model.addAttribute("computer", service.getAllComputers());
		return "products/computerParts";
	}

	@GetMapping("/create")
	public String showCreatePage(Model model) {
		model.addAttribute("computerDto", new ComputerDto());
		return "products/createComputerParts";
	}

	@PostMapping("/create")
	public String createProduct(
			@Valid @ModelAttribute ComputerDto computerDto,
			BindingResult result) {

		if (computerDto.getImageFile().isEmpty()) {
			result.addError(new FieldError("computerDto", "imageFile", "The image file is required!"));
		}

		if (result.hasErrors()) {
			return "products/createComputerParts";
		}

		service.saveComputer(computerDto);
		return "redirect:/computer";
	}

	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Model model) {
		Computers product = service.getComputerById(id);

		ComputerDto computerDto = new ComputerDto();
		computerDto.setBrand(product.getBrand());
		computerDto.setModelName(product.getModelName());
		computerDto.setCategory(product.getCategory());
		computerDto.setStorageSize(product.getStorageSize());
		computerDto.setStocks(product.getStocks());
		computerDto.setPrice(product.getPrice());
		computerDto.setDescription(product.getDescription());

		model.addAttribute("computerDto", computerDto);
		model.addAttribute("productId", id);
		return "products/editComputerParts";
	}

	@PostMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id,
								@Valid @ModelAttribute ComputerDto computerDto,
								BindingResult result,
								Model model) {
		if (result.hasErrors()) {
			model.addAttribute("productId", id);
			return "products/editComputerParts";
		}

		service.updateComputer(id, computerDto);
		return "redirect:/computer";
	}

	@GetMapping("/delete/{id}")
	public String deleteProduct(@PathVariable("id") int id) {
		service.deleteComputer(id);
		return "redirect:/computer";
	}
}
