package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.ClientRepository;
import com.rnpc.inventory.util.PhoneNumbers;
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

    /**
     * Every client whose number is the same phone as this one (see PhoneNumbers.matchKey), oldest
     * first. Compared on digits alone, in Java, because the stored form is not guaranteed to be
     * canonical (a landline keeps what was typed, and rows from before the format rule are raw) -
     * fine at shop scale; a stored digits column would be the next step if the table gets large.
     */
    public List<Client> findAllByContactNumber(String contactNumber) {
        String key = PhoneNumbers.matchKey(contactNumber);
        if (key.isEmpty()) {
            return List.of();
        }
        return repo.findAll(Sort.by(Sort.Direction.ASC, "clientId")).stream()
                .filter(c -> key.equals(PhoneNumbers.matchKey(c.getContactNumber())))
                .toList();
    }

    /**
     * The client a ticket, appointment or checkout should attach to. If old data still holds two
     * clients with the same number, this returns the oldest instead of throwing, so one bad pair
     * cannot take those flows down while the duplicates are merged by hand.
     */
    public Optional<Client> findByContactNumber(String contactNumber) {
        return findAllByContactNumber(contactNumber).stream().findFirst();
    }

    /**
     * A client other than {@code excludeClientId} (null when creating) who already has this number,
     * for rejecting a new or edited client. Empty means the number is free to use.
     */
    public Optional<Client> findOtherWithContactNumber(String contactNumber, Long excludeClientId) {
        return findAllByContactNumber(contactNumber).stream()
                .filter(c -> !c.getClientId().equals(excludeClientId))
                .findFirst();
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
        client.setContactNumber(PhoneNumbers.format(clientDto.getContactNumber()));
        client.setEmail(clientDto.getEmail());
        client.setAddress(clientDto.getAddress());
        return client;
    }

    private void updateEntity(Client client, ClientDto clientDto) {
        client.setFullName(clientDto.getFullName());
        client.setContactNumber(PhoneNumbers.format(clientDto.getContactNumber()));
        client.setEmail(clientDto.getEmail());
        client.setAddress(clientDto.getAddress());
    }
}
