package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.RepairRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("client")
public class ClientController {

    private final ClientService service;
    private final RepairRecordService repairRecordService;

    @Autowired
    public ClientController(ClientService service, RepairRecordService repairRecordService) {
        this.service = service;
        this.repairRecordService = repairRecordService;
    }

    @GetMapping({"", "/"})
    public String showClientList(Model model) {
        var clients = service.getAllClients();
        Map<Long, Long> repairCounts = new HashMap<>();
        Map<Long, String> ticketNumbers = new HashMap<>();
        for (Client client : clients) {
            repairCounts.put(client.getClientId(), repairRecordService.getRepairCountByClient(client.getClientId()));

            List<String> jobOrderNumbers = repairRecordService.getRepairRecordsByClient(client.getClientId())
                    .stream()
                    .map(RepairRecord::getJobOrderNumber)
                    .collect(Collectors.toList());
            ticketNumbers.put(client.getClientId(), String.join(", ", jobOrderNumbers));
        }

        model.addAttribute("client", clients);
        model.addAttribute("repairCounts", repairCounts);
        model.addAttribute("ticketNumbers", ticketNumbers);
        return "clients/clientIndex";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("clientDto", new ClientDto());
        return "clients/clientCreate";
    }

    @PostMapping("/create")
    public String createClient(
            @Valid @ModelAttribute ClientDto clientDto, BindingResult result) {

        if (result.hasErrors()) {
            return "clients/clientCreate";
        }

        service.saveClient(clientDto);
        return "redirect:/client";
    }

    @GetMapping("/edit/{id}")
    public String showEditClientForm(@PathVariable("id") Long id, Model model) {
        Client client = service.getClientById(id);

        ClientDto clientDto = new ClientDto();
        clientDto.setFullName(client.getFullName());
        clientDto.setContactNumber(client.getContactNumber());
        clientDto.setEmail(client.getEmail());
        clientDto.setAddress(client.getAddress());

        model.addAttribute("clientDto", clientDto);
        model.addAttribute("clientId", id);
        model.addAttribute("currentImage", client.getImageFileName());
        return "clients/clientEdit";
    }

    @PutMapping("/update/{id}")
    public String updateClient(@PathVariable("id") Long id,
                                @Valid @ModelAttribute ClientDto clientDto,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("clientId", id);
            model.addAttribute("currentImage", service.getClientById(id).getImageFileName());
            return "clients/clientEdit";
        }

        service.updateClient(id, clientDto);
        return "redirect:/client";
    }

    @DeleteMapping("/removePhoto/{id}")
    public String removePhoto(@PathVariable("id") Long id) {
        service.removePhoto(id);
        return "redirect:/client/edit/" + id;
    }

    @DeleteMapping("/delete/{id}")
    public String deleteClient(@PathVariable("id") Long id) {
        service.deleteClient(id);
        return "redirect:/client";
    }
}
