package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {
    private final ClientRepository repo;
    private final RepairRecordService repairRecordService;

    @Autowired
    public ClientService(ClientRepository repo, RepairRecordService repairRecordService) {
        this.repo = repo;
        this.repairRecordService = repairRecordService;
    }

    public List<Client> getAllClients() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "clientId"));
    }

    public Client getClientById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client Id: " + id));
    }

    public Optional<Client> findByContactNumber(String contactNumber) {
        return repo.findByContactNumber(contactNumber);
    }

    // Attributes a client record to the logged-in account that created/reused it, so their own
    // appointments and repair history can be found later - never overwrites an existing owner.
    public void linkToUser(Client client, User user) {
        if (client.getUser() == null) {
            client.setUser(user);
            repo.save(client);
        }
    }

    public Client saveClient(ClientDto clientDto) {
        String storageFileName = handleFileUpload(clientDto.getImageFile());
        Client client = mapToEntity(clientDto);
        client.setCreatedAt(new Date());
        client.setImageFileName(storageFileName);
        return repo.save(client);
    }

    public Client updateClient(Long id, ClientDto clientDto) {
        Client client = getClientById(id);
        updateEntity(client, clientDto);

        if (clientDto.getImageFile() != null && !clientDto.getImageFile().isEmpty()) {
            deleteImageFile(client.getImageFileName());
            String storageFileName = handleFileUpload(clientDto.getImageFile());
            client.setImageFileName(storageFileName);
        }

        return repo.save(client);
    }

    public void removePhoto(Long id) {
        Client client = getClientById(id);
        deleteImageFile(client.getImageFileName());
        client.setImageFileName(null);
        repo.save(client);
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = getClientById(id);
        for (RepairRecord repairRecord : repairRecordService.getRepairRecordsByClient(id)) {
            repairRecordService.deleteRepairRecord(repairRecord.getRepairId());
        }
        deleteImageFile(client.getImageFileName());
        repo.delete(client);
    }

    private String handleFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = new Date().getTime() + "_" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file: " + ex.getMessage());
        }
    }

    private void deleteImageFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            try {
                String uploadDir = "public/images/";
                Path filePath = Paths.get(uploadDir + fileName);
                Files.deleteIfExists(filePath);
            } catch (Exception ex) {
                System.out.println("Error deleting file: " + ex.getMessage());
            }
        }
    }

    private Client mapToEntity(ClientDto clientDto) {
        Client client = new Client();
        client.setFullName(clientDto.getFullName());
        client.setContactNumber(clientDto.getContactNumber());
        client.setEmail(clientDto.getEmail());
        client.setAddress(clientDto.getAddress());
        return client;
    }

    private void updateEntity(Client client, ClientDto clientDto) {
        client.setFullName(clientDto.getFullName());
        client.setContactNumber(clientDto.getContactNumber());
        client.setEmail(clientDto.getEmail());
        client.setAddress(clientDto.getAddress());
    }
}
