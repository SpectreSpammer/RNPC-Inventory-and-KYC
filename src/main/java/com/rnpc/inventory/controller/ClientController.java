package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.dto.ClientView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.util.Redirects;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
