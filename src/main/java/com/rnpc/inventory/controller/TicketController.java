package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.dto.TicketDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("ticket")
public class TicketController {

    private final ClientService clientService;
    private final RepairRecordService repairRecordService;
    private final NotificationService notificationService;

    @Autowired
    public TicketController(ClientService clientService, RepairRecordService repairRecordService,
                             NotificationService notificationService) {
        this.clientService = clientService;
        this.repairRecordService = repairRecordService;
        this.notificationService = notificationService;
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("ticketDto", new TicketDto());
        return "tickets/ticketCreate";
    }

    @PostMapping("/create")
    public String createTicket(@Valid @ModelAttribute TicketDto ticketDto, BindingResult result) {
        if (result.hasErrors()) {
            return "tickets/ticketCreate";
        }

        Client client = clientService.findByContactNumber(ticketDto.getContactNumber())
                .orElseGet(() -> {
                    ClientDto clientDto = new ClientDto();
                    clientDto.setFullName(ticketDto.getFullName());
                    clientDto.setContactNumber(ticketDto.getContactNumber());
                    clientDto.setEmail(ticketDto.getEmail());
                    clientDto.setAddress(ticketDto.getAddress());
                    return clientService.saveClient(clientDto);
                });

        RepairRecord repairRecord = repairRecordService.createFromTicket(client, ticketDto);
        notificationService.notifyAdmin(
                "New ticket " + repairRecord.getJobOrderNumber() + " created for " + client.getFullName()
                        + " (" + ticketDto.getDeviceType() + ")",
                Notification.EntityType.REPAIR, repairRecord.getRepairId());
        return "redirect:/ticket/" + repairRecord.getRepairId();
    }

    @GetMapping("/{id}")
    public String showTicket(@PathVariable("id") Long id, Model model) {
        RepairRecord repairRecord = repairRecordService.getRepairRecordById(id);
        model.addAttribute("repair", repairRecord);
        model.addAttribute("client", repairRecord.getClient());
        return "tickets/ticketPrint";
    }

    // The repair list's "Print" button fetches just the slip markup via JS and drops it into a
    // modal instead of navigating to the full /ticket/{id} page - reuses the exact same fragment.
    @GetMapping("/{id}/modal")
    public String showTicketModal(@PathVariable("id") Long id, Model model) {
        RepairRecord repairRecord = repairRecordService.getRepairRecordById(id);
        model.addAttribute("repair", repairRecord);
        model.addAttribute("client", repairRecord.getClient());
        return "tickets/ticketPrint :: ticketSlip";
    }

    // "View Ticket" in the nav's Ticket dropdown links here - same Repair History listing/template
    // as RepairRecordController's /repair, just reached from a /ticket/... path instead.
    @GetMapping("/view")
    public String showTicketView(@RequestParam(value = "clientId", required = false) Long clientId,
                                  Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        List<RepairRecord> repairs;
        if (admin) {
            repairs = clientId != null
                    ? repairRecordService.getRepairRecordsByClient(clientId)
                    : repairRecordService.getAllRepairRecords();
        } else if (isSignedIn(authentication)) {
            clientId = null;
            repairs = repairRecordService.getRepairRecordsForUser(authentication.getName());
        } else {
            clientId = null;
            repairs = List.of();
        }

        model.addAttribute("repairs", repairs);
        model.addAttribute("clientId", clientId);
        model.addAttribute("isAdmin", admin);
        // Unlike /repair, this view never links the client name off to the KYC drill-down.
        model.addAttribute("linkClientName", false);
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
}
