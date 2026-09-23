package com.rnpc.inventory.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;
import com.rnpc.inventory.dto.LaptopPartView;
import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.LaptopPartsService;
import com.rnpc.inventory.service.NotificationService;

import jakarta.validation.groups.Default;

@Controller
@RequestMapping("laptop")
public class LaptopPartsController {

	/*
	 * Laptop parts, one model: every part has a PartType. A part is created at
	 * /laptop/{slug}/create - the type comes from that path, never from the form - and edited with
	 * its own type's template under products/laptop/, chosen from the STORED type, so an edit can
	 * never change it. Both are validated as Default + the type's group.
	 *
	 * Validation runs through the injected SmartValidator rather than @Valid, because the group
	 * depends on the type, which is only known inside the handler. Boot marks its defaultValidator
	 * bean primary, so this injection is unambiguous.
	 *
	 * A row with a null part_type should not exist (the old untyped form is gone and part_type is
	 * NOT NULL once the retirement SQL has run). If one appears anyway it is treated as OTHER
	 * everywhere - listed, viewed and edited as Other, and saved back as OTHER - rather than
	 * throwing.
	 */

	/**
	 * Every part type's template prefix (products/laptop/<prefix>Create and <prefix>Edit) - all
	 * twelve types now have forms. A slug that is not a PartType is a 404. EnumMap iterates in
	 * enum order, which is also the order of the Add laptop part dropdowns.
	 */
	private static final Map<PartType, String> FORM_TEMPLATES = new EnumMap<>(PartType.class);
	static {
		FORM_TEMPLATES.put(PartType.LCD, "lcd");
		FORM_TEMPLATES.put(PartType.BATTERY, "battery");
		FORM_TEMPLATES.put(PartType.CHARGER, "charger");
		FORM_TEMPLATES.put(PartType.RAM, "ram");
		FORM_TEMPLATES.put(PartType.STORAGE, "storage");
		FORM_TEMPLATES.put(PartType.KEYBOARD, "keyboard");
		FORM_TEMPLATES.put(PartType.CASING, "casing");
		FORM_TEMPLATES.put(PartType.HINGES, "hinges");
		FORM_TEMPLATES.put(PartType.DC_JACK, "dcJack");
		FORM_TEMPLATES.put(PartType.WIFI_CARD, "wifiCard");
		FORM_TEMPLATES.put(PartType.TOUCHPAD, "touchpad");
		FORM_TEMPLATES.put(PartType.OTHER, "other");
	}

	private final LaptopPartsService laptopPartsService;
	private final NotificationService notificationService;
	private final SmartValidator validator;

	@Autowired
	public LaptopPartsController(LaptopPartsService laptopPartsService, NotificationService notificationService,
								 SmartValidator validator) {
		this.laptopPartsService = laptopPartsService;
		this.notificationService = notificationService;
		this.validator = validator;
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

	// Dropdown options shared by every type's form. Passed in rather than read in the template,
	// because T(...) static calls are avoided in templates here.
	private void addFormOptions(Model model) {
		model.addAttribute("brandOptions", LaptopPartsDto.BRAND_OPTIONS);
		model.addAttribute("conditionOptions", Arrays.asList(PartCondition.values()));
	}

	private static PartType formTypeForSlug(String slug) {
		PartType type = PartType.fromSlug(slug);
		if (type == null || !FORM_TEMPLATES.containsKey(type)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return type;
	}

	private static String typedTemplate(PartType type, String suffix) {
		return "products/laptop/" + FORM_TEMPLATES.get(type) + suffix;
	}

	/** A stored part's type, with a stray null read as OTHER (see the class comment). */
	private static PartType storedType(LaptopParts part) {
		PartType type = part.getPartType();
		return type == null ? PartType.OTHER : type;
	}

	@GetMapping({"","/"})
	public String showLaptopPartsList(Authentication authentication, Model model) {
		// Display-ready rows, serialized into the page as ALL_PARTS - same approach as /computer.
		// Every label, key spec and unit is decided in LaptopPartView, not in the template.
		model.addAttribute("parts", laptopPartsService.getAllLaptopParts().stream()
				.map(LaptopPartView::from).collect(Collectors.toList()));

		// Chip list in enum order, every type shown even at 0 (as on /computer). The chip's key is
		// the slug, not t.name() - it's the same identifier the ?type= restore below has to match
		// (see LaptopPartView.from), which is also what every /laptop/{slug}/... URL already uses.
		List<Map<String, String>> partTypes = new ArrayList<>();
		for (PartType t : PartType.values()) {
			Map<String, String> chip = new LinkedHashMap<>();
			chip.put("key", t.getSlug());
			chip.put("label", t.getLabel());
			partTypes.add(chip);
		}
		model.addAttribute("partTypes", partTypes);
		// Same low-stock rule as the admin dashboard and /computer, reused rather than restated.
		model.addAttribute("lowStockLimit", AdminDashboardService.LOW_STOCK_LIMIT);
		model.addAttribute("addPartTypes", new ArrayList<>(FORM_TEMPLATES.keySet()));
		addShellAttributes(authentication, model);
		return "products/laptopParts";
	}

	// ---- Type-based create ----------------------------------------------------------------

	@GetMapping("/{type}/create")
	public String showTypedCreateForm(@PathVariable("type") String slug, Authentication authentication, Model model) {
		PartType type = formTypeForSlug(slug);
		model.addAttribute("laptopPartsDto", new LaptopPartsDto());
		model.addAttribute("partType", type);
		addFormOptions(model);
		addShellAttributes(authentication, model);
		return typedTemplate(type, "Create");
	}

	@PostMapping("/{type}/create")
	public String createTypedPart(@PathVariable("type") String slug,
								  @ModelAttribute LaptopPartsDto laptopPartsDto, BindingResult result,
								  Authentication authentication, Model model) {
		PartType type = formTypeForSlug(slug);
		validator.validate(laptopPartsDto, result, Default.class, type.getGroup());
		if (result.hasErrors()) {
			model.addAttribute("partType", type);
			addFormOptions(model);
			addShellAttributes(authentication, model);
			return typedTemplate(type, "Create");
		}

		laptopPartsService.saveTypedPart(type, laptopPartsDto);
		return "redirect:/laptop";
	}

	@DeleteMapping("/delete/{id}")
	public String deleteLaptopPart(@PathVariable("id") int id) {
		laptopPartsService.deleteLaptopPart(id);
		return "redirect:/laptop";
	}

	// ---- Edit: the template follows the part's own type ------------------------------------

	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Authentication authentication, Model model) {
		LaptopParts product = laptopPartsService.getLaptopPartById(id);
		PartType type = storedType(product);

		model.addAttribute("laptopPartId", id);
		model.addAttribute("currentImage", product.getImageFileName());
		model.addAttribute("laptopPartsDto", laptopPartsService.toDto(product));
		model.addAttribute("partType", type);
		addFormOptions(model);
		addShellAttributes(authentication, model);
		return typedTemplate(type, "Edit");
	}

	@PutMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id,
								@ModelAttribute LaptopPartsDto laptopPartsDto,
								BindingResult result,
								Authentication authentication,
								Model model) {
		LaptopParts product = laptopPartsService.getLaptopPartById(id);
		// The type is the stored one - an edit can never change it.
		PartType type = storedType(product);

		validator.validate(laptopPartsDto, result, Default.class, type.getGroup());
		if (result.hasErrors()) {
			model.addAttribute("laptopPartId", id);
			model.addAttribute("currentImage", product.getImageFileName());
			model.addAttribute("partType", type);
			addFormOptions(model);
			addShellAttributes(authentication, model);
			return typedTemplate(type, "Edit");
		}

		laptopPartsService.updateLaptopPart(id, laptopPartsDto);
		// Return to the type the part belongs to, not always All - same mechanism as /computer's
		// ?category= (allParts.html), just computed here since one controller serves every type.
		return "redirect:/laptop?type=" + type.getSlug();
	}

	@DeleteMapping("/removePhoto/{id}")
	public String removePhoto(@PathVariable("id") int id) {
		laptopPartsService.removePhoto(id);
		return "redirect:/laptop/edit/" + id;
	}
}
