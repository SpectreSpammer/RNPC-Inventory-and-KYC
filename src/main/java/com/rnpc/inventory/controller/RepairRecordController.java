package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.util.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.groups.Default;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("repair")
public class RepairRecordController {

    private final RepairRecordService service;
    private final ClientService clientService;
    private final NotificationService notificationService;
    private final SmartValidator validator;

    @Autowired
    public RepairRecordController(RepairRecordService service, ClientService clientService,
                                   NotificationService notificationService, SmartValidator validator) {
        this.service = service;
        this.clientService = clientService;
        this.notificationService = notificationService;
        this.validator = validator;
    }

    // A cleared cost field posts "", which cannot be bound to the primitive double: Spring records a
    // generic typeMismatch ("Failed to convert property value ... empty String") and Bean Validation
    // never looks at the field. When the posted status is COMPLETED or RELEASED the person should see
    // the required-cost message instead, so a blank cost is bound as 0.0 for those two statuses and
    // the cost rule in RepairRecordDto rejects it like any other zero. Every other status keeps
    // Spring's default handling - the status is read from the request here because the binder has
    // not bound the DTO yet. Repairs batch 1b makes cost a nullable Double: this editor can then go,
    // since a blank binds to null and the @NotNull on the finished-repair rule rejects it.
    @InitBinder("repairRecordDto")
    public void initBinder(WebDataBinder binder, HttpServletRequest request) {
        if (RepairRecordDto.requiresFix(request.getParameter("status"))) {
            binder.registerCustomEditor(double.class, "cost", new PropertyEditorSupport() {
                @Override
                public void setAsText(String text) {
                    setValue(text == null || text.isBlank() ? 0.0d : Double.valueOf(text.trim()));
                }
            });
        }
    }

    // Default plus, only when the status makes the fix mandatory, the FixRequired group - see
    // RepairRecordDto. Run through the injected SmartValidator rather than @Valid because the group
    // depends on a posted field. Boot marks its defaultValidator bean primary, so this is unambiguous.
    private void validate(RepairRecordDto repairRecordDto, BindingResult result) {
        if (RepairRecordDto.requiresFix(repairRecordDto.getStatus())) {
            validator.validate(repairRecordDto, result, Default.class, RepairRecordDto.FixRequired.class);
        } else {
            validator.validate(repairRecordDto, result, Default.class, RepairRecordDto.NotFinished.class);
        }
    }

    @GetMapping({"", "/"})
    public String showRepairList(@RequestParam(value = "clientId", required = false) Long clientId,
                                  Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        List<RepairRecord> repairs;
        if (admin) {
            repairs = clientId != null
                    ? service.getRepairRecordsByClient(clientId)
                    : service.getAllRepairRecords();
        } else if (isSignedIn(authentication)) {
            clientId = null;
            repairs = service.getRepairRecordsForUser(authentication.getName());
        } else {
            clientId = null;
            repairs = List.of();
        }

        model.addAttribute("repairs", repairs);
        model.addAttribute("clientId", clientId);
        model.addAttribute("isAdmin", admin);
        // /repair keeps the admin's client-name drill-down link; /ticket/view (same template,
        // see TicketController.showTicketView) intentionally does not.
        model.addAttribute("linkClientName", admin);
        if (admin && clientId != null) {
            model.addAttribute("client", clientService.getClientById(clientId));
        }

        // Topbar chrome (username/role/notification bell+dropdown) - this page never set these
        // before the redesign, so the user-menu/bell silently never appeared (layout-app.html's
        // topbar guards currentUsername==null). Same admin/customer split notifyAdmin/notifyCustomer
        // already use elsewhere (NotificationService.java:51-57): admin sees the shared admin
        // inbox, a signed-in customer sees their own.
        if (isSignedIn(authentication)) {
            String username = authentication.getName();
            model.addAttribute("currentUsername", username);
            model.addAttribute("currentRole", admin ? "Admin" : "Customer");
            model.addAttribute("unreadNotifications",
                    admin ? notificationService.getUnreadCountForAdmin() : notificationService.getUnreadCountForUser(username));
            List<Notification> recent =
                    admin ? notificationService.getForAdmin() : notificationService.getForUser(username);
            model.addAttribute("recentNotifications", recent.stream().limit(15).collect(Collectors.toList()));
        }

        return "repairs/repairIndex";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    // Notifications (and any other list) fetch this fragment via JS and drop it into a modal
    // instead of navigating to a full page - only an admin or the repair's own linked account may
    // fetch it (mirrors the visibility rule in RepairRecordService.getRepairRecordsForUser).
    @GetMapping("/{id}/modal")
    public String showRepairModal(@PathVariable("id") Long id, Authentication authentication, Model model) {
        RepairRecord repair = service.getRepairRecordById(id);
        boolean owns = isSignedIn(authentication) && repair.getClient().getUser() != null
                && repair.getClient().getUser().getUsername().equals(authentication.getName())
                && repair.isVisible();
        if (!isAdmin(authentication) && !owns) {
            return "redirect:/repair";
        }
        model.addAttribute("repair", repair);
        return "repairs/repairDetail :: repairCard";
    }

    @GetMapping("/create")
    public String showCreatePage(@RequestParam(value = "clientId", required = false) Long clientId,
                                  Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        RepairRecordDto repairRecordDto = new RepairRecordDto();
        if (clientId != null) {
            repairRecordDto.setClientId(clientId);
        }
        model.addAttribute("repairRecordDto", repairRecordDto);
        model.addAttribute("clients", clientService.getAllClients());
        return "repairs/repairCreate";
    }

    @PostMapping("/create")
    public String createRepairRecord(@ModelAttribute RepairRecordDto repairRecordDto,
                                      BindingResult result, Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        validate(repairRecordDto, result);
        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.getAllClients());
            return "repairs/repairCreate";
        }

        service.saveRepairRecord(repairRecordDto);
        return "redirect:/repair?clientId=" + repairRecordDto.getClientId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        RepairRecord repairRecord = service.getRepairRecordById(id);

        RepairRecordDto repairRecordDto = new RepairRecordDto();
        repairRecordDto.setClientId(repairRecord.getClient().getClientId());
        repairRecordDto.setDeviceType(repairRecord.getDeviceType());
        repairRecordDto.setBrand(repairRecord.getBrand());
        repairRecordDto.setModelName(repairRecord.getModelName());
        repairRecordDto.setSerialNumber(repairRecord.getSerialNumber());
        repairRecordDto.setIssueDescription(repairRecord.getIssueDescription());
        repairRecordDto.setTechnician(repairRecord.getTechnician());
        repairRecordDto.setCost(repairRecord.getCost());
        repairRecordDto.setRepairDate(repairRecord.getRepairDate());
        repairRecordDto.setWarrantyEndDate(repairRecord.getWarrantyEndDate());
        repairRecordDto.setStatus(repairRecord.getStatus().name());
        repairRecordDto.setRemarks(repairRecord.getRemarks());
        repairRecordDto.setFix(repairRecord.getFix());
        repairRecordDto.setRecommendation(repairRecord.getRecommendation());

        model.addAttribute("repairRecordDto", repairRecordDto);
        model.addAttribute("repairId", id);
        model.addAttribute("jobOrderNumber", repairRecord.getJobOrderNumber());
        model.addAttribute("currentImage", repairRecord.getImageFileName());
        model.addAttribute("clients", clientService.getAllClients());
        return "repairs/repairEdit";
    }

    @PutMapping("/update/{id}")
    public String updateRepairRecord(@PathVariable("id") Long id,
                                      @ModelAttribute RepairRecordDto repairRecordDto,
                                      BindingResult result,
                                      Authentication authentication,
                                      Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        validate(repairRecordDto, result);
        if (result.hasErrors()) {
            RepairRecord existing = service.getRepairRecordById(id);
            model.addAttribute("repairId", id);
            model.addAttribute("jobOrderNumber", existing.getJobOrderNumber());
            model.addAttribute("currentImage", existing.getImageFileName());
            model.addAttribute("clients", clientService.getAllClients());
            return "repairs/repairEdit";
        }

        service.updateRepairRecord(id, repairRecordDto);
        return "redirect:/repair?clientId=" + repairRecordDto.getClientId();
    }

    // Called by fetch (see fragments/parts-form :: remove-photo-script), not a form, so both
    // branches need a 303 - a bare "redirect:" (302) would have fetch's DELETE re-sent as DELETE
    // on follow, 405ing against a GET-only route (/repair/edit/{id}, or /repair itself). See
    // Redirects.
    @DeleteMapping("/removePhoto/{id}")
    public ResponseEntity<Void> removePhoto(@PathVariable("id") Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return Redirects.seeOther("/repair");
        }
        service.removePhoto(id);
        return Redirects.seeOther("/repair/edit/" + id);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteRepairRecord(@PathVariable("id") Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        Long clientId = service.getRepairRecordById(id).getClient().getClientId();
        service.deleteRepairRecord(id);
        return "redirect:/repair?clientId=" + clientId;
    }
}
