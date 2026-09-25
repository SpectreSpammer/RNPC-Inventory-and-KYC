package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.CellphonePartView;
import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.entity.CellphoneParts.PartType;
import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.CellphonePartsService;
import com.rnpc.inventory.service.NotificationService;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("cellphone")
public class CellphonePartsController {

    /*
     * Cellphone parts, mid-redesign (batch 3: type-based create/edit for Screen and Battery).
     * Two models live side by side, same coexistence as laptop batch 2:
     *
     *  - Type-based parts (partType set): created at /cellphone/{slug}/create, edited with the
     *    type's own template under products/cellphone/. Validated as Default + the type's group.
     *  - Pre-redesign parts (partType null): the old /cellphone/create form and old edit template,
     *    validated as Default + Legacy, exactly the rules they had before.
     *
     * Validation runs through the injected SmartValidator rather than @Valid, because the groups
     * depend on the type, which is only known inside the handler. Boot marks its defaultValidator
     * bean primary, so this injection is unambiguous.
     */

    /**
     * The types that have create/edit templates so far, and each one's template prefix
     * (products/cellphone/<prefix>Create and <prefix>Edit). A later batch adds the rest; until
     * then any other slug is a 404. Also the order of the Add cellphone part dropdown.
     */
    private static final Map<PartType, String> FORM_TEMPLATES = new EnumMap<>(PartType.class);
    static {
        FORM_TEMPLATES.put(PartType.SCREEN, "screen");
        FORM_TEMPLATES.put(PartType.BATTERY, "battery");
        FORM_TEMPLATES.put(PartType.CHARGING_BOARD, "chargingBoard");
        FORM_TEMPLATES.put(PartType.BACK_GLASS, "backGlass");
        FORM_TEMPLATES.put(PartType.HOUSING, "housing");
        FORM_TEMPLATES.put(PartType.FLEX_CABLE, "flexCable");
        FORM_TEMPLATES.put(PartType.CAMERA, "camera");
        FORM_TEMPLATES.put(PartType.FINGERPRINT, "fingerprint");
        FORM_TEMPLATES.put(PartType.SENSOR, "sensor");
        FORM_TEMPLATES.put(PartType.OTHER, "other");
    }

    private final CellphonePartsService cellphonePartsService;
    private final NotificationService notificationService;
    private final SmartValidator validator;


    @Autowired
    public CellphonePartsController(CellphonePartsService cellphonePartsService, NotificationService notificationService,
                                    SmartValidator validator) {
        this.cellphonePartsService = cellphonePartsService;
        this.notificationService = notificationService;
        this.validator = validator;
    }

    // Topbar chrome for the shared layout-app shell, same shape as LaptopPartsController's.
    // This route is admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_PARTS_PATHS), so
    // the admin inbox is always the right one and no isAdmin check belongs here.
    private void addShellAttributes(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
    }

    // Dropdown options shared by the old and the type-based forms. Passed in rather than read in
    // the template, because T(...) static calls are avoided in templates here.
    private void addFormOptions(Model model) {
        model.addAttribute("brandOptions", CellphonePartsDto.BRAND_OPTIONS);
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
        return "products/cellphone/" + FORM_TEMPLATES.get(type) + suffix;
    }

    /**
     * A stored part's type, with a stray null read as OTHER - same helper as
     * LaptopPartsController.storedType. Every cellphone row is null today (batch 1 added the
     * column without backfilling it), so this always returns OTHER for now.
     */
    private static PartType storedType(CellphoneParts part) {
        PartType type = part.getPartType();
        return type == null ? PartType.OTHER : type;
    }

    @GetMapping({"", "/"})
    public String showCellphonePartsList(Authentication authentication, Model model){
        // Display-ready rows, serialized into the page as ALL_PARTS - same approach as /laptop.
        // Every label, key spec and unit is decided in CellphonePartView, not in the template.
        model.addAttribute("parts", cellphonePartsService.getAllCellphoneParts().stream()
                .map(CellphonePartView::from).collect(Collectors.toList()));

        // Chip list in enum order, every type shown even at 0 (as on /laptop and /computer). The
        // chip's key is the slug, not t.name() - see LaptopPartsController's identical comment.
        List<Map<String, String>> partTypes = new ArrayList<>();
        for (CellphoneParts.PartType t : CellphoneParts.PartType.values()) {
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
        return "products/cellphoneParts";
    }

    // ---- Type-based create ----------------------------------------------------------------

    @GetMapping("/{type}/create")
    public String showTypedCreateForm(@PathVariable("type") String slug, Authentication authentication, Model model) {
        PartType type = formTypeForSlug(slug);
        model.addAttribute("cellphonePartsDto", new CellphonePartsDto());
        model.addAttribute("partType", type);
        addFormOptions(model);
        addShellAttributes(authentication, model);
        return typedTemplate(type, "Create");
    }

    @PostMapping("/{type}/create")
    public String createTypedPart(@PathVariable("type") String slug,
                                  @ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result,
                                  Authentication authentication, Model model) {
        PartType type = formTypeForSlug(slug);
        validator.validate(cellphonePartsDto, result, Default.class, type.getGroup());
        if (result.hasErrors()) {
            model.addAttribute("partType", type);
            addFormOptions(model);
            addShellAttributes(authentication, model);
            return typedTemplate(type, "Create");
        }

        cellphonePartsService.saveTypedPart(type, cellphonePartsDto);
        return "redirect:/cellphone";
    }

    // ---- Pre-redesign create, kept working until every type has a form -----------------------

    @PostMapping("/create")
    public String createCellphonePart(@ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result){
        validator.validate(cellphonePartsDto, result, Default.class, PartType.Groups.Legacy.class);
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

    // ---- Edit: the template follows the part's own type ------------------------------------

    @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Authentication authentication, Model model){
        CellphoneParts product = cellphonePartsService.getCellphonePartById(id);
        PartType rawType = product.getPartType();
        boolean typed = rawType != null && FORM_TEMPLATES.containsKey(rawType);

        model.addAttribute("cellphonePartId", id);
        model.addAttribute("currentImage", product.getImageFileName());
        // So the Cancel link can return to the type the part belongs to - see updateProduct.
        model.addAttribute("partType", storedType(product));

        if (typed) {
            model.addAttribute("cellphonePartsDto", cellphonePartsService.toDto(product));
            addFormOptions(model);
            addShellAttributes(authentication, model);
            return typedTemplate(rawType, "Edit");
        }

        // Pre-redesign row (partType null): the old edit form, populated as before.
        CellphonePartsDto cellphonePartsDto = new CellphonePartsDto();
        cellphonePartsDto.setBrand(product.getBrand());
        cellphonePartsDto.setPartName(product.getPartName());
        cellphonePartsDto.setCategory(product.getCategory());
        cellphonePartsDto.setStorageSize(product.getStorageSize());
        cellphonePartsDto.setStocks(product.getStocks());
        cellphonePartsDto.setPrice(product.getPrice());
        cellphonePartsDto.setDescription(product.getDescription());
        model.addAttribute("cellphonePartsDto", cellphonePartsDto);

        return "products/cellphoneEditParts";
    }

    @PutMapping("/update/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @ModelAttribute CellphonePartsDto cellphonePartsDto, BindingResult result,
                                Authentication authentication, Model model){
        // Fetched up front (not just on error) because the redirect on success also needs the
        // part's type - it never changes here, since these forms don't touch partType at all.
        CellphoneParts product = cellphonePartsService.getCellphonePartById(id);
        PartType rawType = product.getPartType();
        boolean typed = rawType != null && FORM_TEMPLATES.containsKey(rawType);
        PartType type = storedType(product);

        validator.validate(cellphonePartsDto, result, Default.class,
                typed ? rawType.getGroup() : PartType.Groups.Legacy.class);
        if (result.hasErrors()){
            model.addAttribute("cellphonePartId", id);
            model.addAttribute("currentImage", product.getImageFileName());
            model.addAttribute("partType", type);
            if (typed) {
                addFormOptions(model);
                addShellAttributes(authentication, model);
                return typedTemplate(rawType, "Edit");
            }
            return "products/cellphoneEditParts";
        }
        cellphonePartsService.updateCellphonePart(id, cellphonePartsDto);
        // Return to the type the part belongs to, not always All - same mechanism as /computer's
        // ?category= (allParts.html) and /laptop's ?type=.
        return "redirect:/cellphone?type=" + type.getSlug();
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") Long id) {
        cellphonePartsService.removePhoto(id);
        return "redirect:/cellphone/edit/" + id;
    }
}
