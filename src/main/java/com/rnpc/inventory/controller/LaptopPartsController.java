package com.rnpc.inventory.controller;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.LaptopPartsService;
import com.rnpc.inventory.service.NotificationService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("laptop")
public class LaptopPartsController {

	private final LaptopPartsService laptopPartsService;
	private final NotificationService notificationService;

	@Autowired
	public LaptopPartsController(LaptopPartsService laptopPartsService, NotificationService notificationService) {
		this.laptopPartsService = laptopPartsService;
		this.notificationService = notificationService;
	}

	// Topbar chrome for the shared layout-app shell, same shape as CpuPartsController's.
	// These routes are admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_PARTS_PATHS), so
	// the admin inbox is always the right one and no isAdmin check belongs here. Called from the
	// list, create and edit handlers, including their validation-error paths.
	private void addShellAttributes(Authentication authentication, Model model) {
		model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
		model.addAttribute("currentRole", "Admin");
		model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
		model.addAttribute("recentNotifications",
				notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
	}

	@GetMapping({"","/"})
	public String showLaptopPartsList(Authentication authentication, Model model) {
		model.addAttribute("laptop", laptopPartsService.getAllLaptopParts());
		// Same low-stock rule as the admin dashboard and /computer, reused rather than restated.
		model.addAttribute("lowStockLimit", AdminDashboardService.LOW_STOCK_LIMIT);
		addShellAttributes(authentication, model);
		return "products/laptopParts";
	}

	@PostMapping("/create")
	public String createLaptopPart(@Valid @ModelAttribute LaptopPartsDto laptopPartsDto, BindingResult result,
								   Authentication authentication, Model model) {
		// The photo is optional, as it is for the PC categories: with no file,
		// LaptopPartsService.handleFileUpload returns null and the part is saved without one.
		if (result.hasErrors()) {
			addShellAttributes(authentication, model);
			return "products/laptopCreateParts";
		}

		laptopPartsService.saveLaptopPart(laptopPartsDto);
		return "redirect:/laptop";
	}

	@GetMapping("/create")
	public String showCreateLaptopPartForm(Authentication authentication, Model model) {
		model.addAttribute("laptopPartsDto", new LaptopPartsDto());
		addShellAttributes(authentication, model);
		return "products/laptopCreateParts";
	}

	@DeleteMapping("/delete/{id}")
	public String deleteLaptopPart(@PathVariable("id") int id) {
		laptopPartsService.deleteLaptopPart(id);
		return "redirect:/laptop";
	}

	// Show edit page for a specific laptop
	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Authentication authentication, Model model) {
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
		model.addAttribute("laptopPartId", id);
		model.addAttribute("currentImage", product.getImageFileName());
		addShellAttributes(authentication, model);
		return "products/laptopEditParts";
	}


	// Update the laptop details
	@PutMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id,
								@Valid @ModelAttribute LaptopPartsDto laptopDto,
								BindingResult result,
								Authentication authentication,
								Model model) {
		if (result.hasErrors()) {
			model.addAttribute("laptopPartId", id);
			model.addAttribute("currentImage", laptopPartsService.getLaptopPartById(id).getImageFileName());
			addShellAttributes(authentication, model);
			return "products/laptopEditParts";
		}

		laptopPartsService.updateLaptopPart(id, laptopDto);
		return "redirect:/laptop";
	}

	@DeleteMapping("/removePhoto/{id}")
	public String removePhoto(@PathVariable("id") int id) {
		laptopPartsService.removePhoto(id);
		return "redirect:/laptop/edit/" + id;
	}
}
