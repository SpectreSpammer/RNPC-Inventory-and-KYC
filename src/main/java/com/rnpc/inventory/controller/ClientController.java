package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.dto.AdminRepairView;
import com.rnpc.inventory.dto.ClientView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.util.Redirects;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("client")
public class ClientController {

    private final ClientService service;
    private final RepairRecordService repairRecordService;
    private final NotificationService notificationService;

    @Autowired
    public ClientController(ClientService service, RepairRecordService repairRecordService,
                            NotificationService notificationService) {
        this.service = service;
        this.repairRecordService = repairRecordService;
        this.notificationService = notificationService;
    }

    // Topbar chrome for the shared layout-app shell, same shape as LaptopPartsController's. These
    // routes are admin-only via the filter chain (SecurityConfig.ADMIN_ONLY_CUSTOMER_PATHS), so the
    // admin inbox is always the right one and no isAdmin check belongs here.
    private void addShellAttributes(Authentication authentication, Model model) {
        model.addAttribute("currentUsername", authentication != null ? authentication.getName() : null);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream().limit(15).collect(Collectors.toList()));
    }

    @GetMapping({"", "/"})
    public String showClientList(Authentication authentication, Model model) {
        // Display-ready rows, serialized into the page as ALL_CLIENTS - same approach as /laptop.
        // Clients are read first, then every repair record in ONE query and grouped by client,
        // rather than a count and a list per client.
        List<Client> clients = service.getAllClients();
        Map<Long, List<RepairRecord>> repairsByClient = repairRecordService.getRepairRecordsGroupedByClient();
        model.addAttribute("clients", clients.stream()
                .map(c -> ClientView.from(c, repairsByClient.get(c.getClientId())))
                .collect(Collectors.toList()));
        addShellAttributes(authentication, model);
        return "clients/clientIndex";
    }

    /**
     * Read-only details for one client (customers batch C1). An unknown id is a 404 here only -
     * the edit route still throws on one. The job orders are the repairs Repair History shows
     * (RepairRecordService.getRepairRecordsByClient), as AdminRepairView rows for the status label
     * and cost text; the template never prints their remarks.
     */
    @GetMapping("/view/{id}")
    public String showClientDetails(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Client client;
        try {
            client = service.getClientById(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        List<RepairRecord> repairs = repairRecordService.getRepairRecordsByClient(id);

        model.addAttribute("view", ClientView.from(client, repairs));
        model.addAttribute("walkIn", client.getUser() == null);
        model.addAttribute("account", accountLabel(client.getUser()));
        // The same bare filename the edit form renders under /images/ - no route of its own.
        String photo = client.getImageFileName();
        model.addAttribute("photoFile", photo == null || photo.isBlank() ? null : photo);
        model.addAttribute("repairs", repairs.stream().map(AdminRepairView::from).collect(Collectors.toList()));
        addShellAttributes(authentication, model);
        return "clients/clientView";
    }

    /** "Walk-in client" without a linked sign-in account; otherwise their email, else their username. */
    private static String accountLabel(User user) {
        if (user == null) {
            return "Walk-in client";
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "Linked account";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        model.addAttribute("clientDto", new ClientDto());
        addShellAttributes(authentication, model);
        return "clients/clientCreate";
    }

    @PostMapping("/create")
    public String createClient(@Valid @ModelAttribute ClientDto clientDto, BindingResult result,
                                Authentication authentication, Model model) {
        rejectDuplicateNumber(clientDto, result, null);
        if (!result.hasErrors()) {
            try {
                service.saveClient(clientDto);
                return "redirect:/client";
            } catch (IllegalArgumentException ex) {
                result.rejectValue("imageFile", "invalid", ex.getMessage());
            }
        }
        addShellAttributes(authentication, model);
        return "clients/clientCreate";
    }

    @GetMapping("/edit/{id}")
    public String showEditClientForm(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Client client = service.getClientById(id);

        ClientDto clientDto = new ClientDto();
        clientDto.setFullName(client.getFullName());
        clientDto.setContactNumber(client.getContactNumber());
        clientDto.setEmail(client.getEmail());
        clientDto.setAddress(client.getAddress());

        model.addAttribute("clientDto", clientDto);
        model.addAttribute("clientId", id);
        model.addAttribute("currentImage", client.getImageFileName());
        addShellAttributes(authentication, model);
        return "clients/clientEdit";
    }

    @PutMapping("/update/{id}")
    public String updateClient(@PathVariable("id") Long id,
                                @Valid @ModelAttribute ClientDto clientDto,
                                BindingResult result,
                                Authentication authentication,
                                Model model) {
        rejectDuplicateNumber(clientDto, result, id);
        if (!result.hasErrors()) {
            try {
                service.updateClient(id, clientDto);
                return "redirect:/client";
            } catch (IllegalArgumentException ex) {
                result.rejectValue("imageFile", "invalid", ex.getMessage());
            }
        }
        model.addAttribute("clientId", id);
        model.addAttribute("currentImage", service.getClientById(id).getImageFileName());
        addShellAttributes(authentication, model);
        return "clients/clientEdit";
    }

    // A number already on another client is refused, naming that client so the admin can go to them
    // instead. Skipped when the field already failed validation, so one field never shows two errors.
    private void rejectDuplicateNumber(ClientDto clientDto, BindingResult result, Long excludeClientId) {
        if (result.hasFieldErrors("contactNumber")) {
            return;
        }
        service.findOtherWithContactNumber(clientDto.getContactNumber(), excludeClientId).ifPresent(other ->
                result.rejectValue("contactNumber", "duplicate",
                        "This number already belongs to " + other.getFullName() + " (client #" + other.getClientId()
                                + "). Open that client instead of adding another."));
    }

    // A bare "redirect:" is a 302, which fetch's DELETE would re-send as DELETE on follow, 405ing
    // against the GET-only edit route - see Redirects. This is called by fetch, not a form, so it
    // needs the 303.
    @DeleteMapping("/removePhoto/{id}")
    public ResponseEntity<Void> removePhoto(@PathVariable("id") Long id) {
        service.removePhoto(id);
        return Redirects.seeOther("/client/edit/" + id);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteClient(@PathVariable("id") Long id) {
        service.deleteClient(id);
        return "redirect:/client";
    }
}
