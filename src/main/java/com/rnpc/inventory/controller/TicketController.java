package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.dto.TicketDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.UserService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("ticket")
public class TicketController {

    private final ClientService clientService;
    private final RepairRecordService repairRecordService;
    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public TicketController(ClientService clientService, RepairRecordService repairRecordService,
                             NotificationService notificationService, UserService userService) {
        this.clientService = clientService;
        this.repairRecordService = repairRecordService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Topbar chrome for the shared layout-app shell, same shape as ClientController's. /ticket/create
    // is admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_CUSTOMER_PATHS), so the admin
    // inbox is always the right one and no isAdmin check belongs here.
    private void addShellAttributes(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("ticketDto", new TicketDto());
        addShellAttributes(authentication, model);
        return "tickets/ticketCreate";
    }

    @PostMapping("/create")
    public String createTicket(@Valid @ModelAttribute TicketDto ticketDto, BindingResult result,
                                Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addShellAttributes(authentication, model);
            return "tickets/ticketCreate";
        }

        // A matched client keeps whatever it already has - only a blank name/email/address is
        // filled in from what was typed, so a walk-in typo can never overwrite a client's real
        // details (see ClientService.fillMissingDetails).
        Optional<Client> existingClient = clientService.findByContactNumber(ticketDto.getContactNumber());
        Client client;
        if (existingClient.isPresent()) {
            client = existingClient.get();
            clientService.fillMissingDetails(client, ticketDto.getFullName(), ticketDto.getEmail(), ticketDto.getAddress());
        } else {
            ClientDto clientDto = new ClientDto();
            clientDto.setFullName(ticketDto.getFullName());
            clientDto.setContactNumber(ticketDto.getContactNumber());
            clientDto.setEmail(ticketDto.getEmail());
            clientDto.setAddress(ticketDto.getAddress());
            client = clientService.saveClient(clientDto);
        }

        // The acting admin's employee label (see RepairRecord.receivedByEmployeeId), the same
        // pattern OrderController.markPaid uses for verifiedByEmployeeId. authentication is
        // never null in practice (the filter chain requires an admin on this route), but every
        // other handler here still checks before dereferencing it - see addShellAttributes.
        String receivedByEmployeeId = authentication == null ? null
                : userService.findByUsername(authentication.getName()).map(User::getEmployeeLabel).orElse(null);
        RepairRecord repairRecord = repairRecordService.createFromTicket(client, ticketDto, receivedByEmployeeId);
        notificationService.notifyAdmin(
                "New ticket " + repairRecord.getJobOrderNumber() + " created for " + client.getFullName()
                        + " (" + ticketDto.getDeviceType() + ")",
                Notification.EntityType.REPAIR, repairRecord.getRepairId());
        return "redirect:/ticket/" + repairRecord.getRepairId();
    }

    @GetMapping("/{id}")
    public String showTicket(@PathVariable("id") Long id, Authentication authentication, Model model) {
        RepairRecord repairRecord = repairRecordService.getRepairRecordById(id);
        model.addAttribute("repair", repairRecord);
        model.addAttribute("client", repairRecord.getClient());
        // Precomputed rather than built in the template - avoids a ternary split across multiple
        // ${...} blocks (a past SpEL parse-error source here) and keeps the middle dot a real
        // UTF-8 character rather than an HTML entity, which th:text would render literally.
        model.addAttribute("ticketSubtitle", ticketSubtitle(repairRecord));
        addShellAttributes(authentication, model);
        return "tickets/ticketPrint";
    }

    private static String ticketSubtitle(RepairRecord repairRecord) {
        String device = repairRecord.deviceSummary(", ");
        return "Job order " + repairRecord.getJobOrderNumber() + " for " + repairRecord.getClient().getFullName()
                + (device.isEmpty() ? "" : " · " + device);
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
