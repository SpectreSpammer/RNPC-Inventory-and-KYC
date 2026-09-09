package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.RepairRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("repair")
public class RepairRecordController {

    private final RepairRecordService service;
    private final ClientService clientService;

    @Autowired
    public RepairRecordController(RepairRecordService service, ClientService clientService) {
        this.service = service;
        this.clientService = clientService;
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
    public String createRepairRecord(@Valid @ModelAttribute RepairRecordDto repairRecordDto,
                                      BindingResult result, Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
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
                                      @Valid @ModelAttribute RepairRecordDto repairRecordDto,
                                      BindingResult result,
                                      Authentication authentication,
                                      Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
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

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/repair";
        }
        service.removePhoto(id);
        return "redirect:/repair/edit/" + id;
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
