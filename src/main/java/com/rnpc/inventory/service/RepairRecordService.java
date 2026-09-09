package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.AppointmentDto;
import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.dto.TicketDto;
import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.repository.ClientRepository;
import com.rnpc.inventory.repository.RepairRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepairRecordService {
    private final RepairRecordRepository repo;
    private final ClientRepository clientRepo;

    @Autowired
    public RepairRecordService(RepairRecordRepository repo, ClientRepository clientRepo) {
        this.repo = repo;
        this.clientRepo = clientRepo;
    }

    // Excludes an appointment-sourced record until an admin confirms that appointment - see
    // RepairRecord.isVisible - for admin views as well as the customer's own, per the request
    // that a ticket only shows up in Repair History "once the ticket is confirmed or accepted."
    public List<RepairRecord> getAllRepairRecords() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "repairDate")).stream()
                .filter(RepairRecord::isVisible)
                .collect(Collectors.toList());
    }

    public List<RepairRecord> getRepairRecordsByClient(Long clientId) {
        return repo.findByClient_ClientIdOrderByRepairDateDesc(clientId).stream()
                .filter(RepairRecord::isVisible)
                .collect(Collectors.toList());
    }

    public long getRepairCountByClient(Long clientId) {
        return repo.countByClient_ClientId(clientId);
    }

    public RepairRecord getRepairRecordById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid repair record Id: " + id));
    }

    public RepairRecord saveRepairRecord(RepairRecordDto repairRecordDto) {
        RepairRecord repairRecord = new RepairRecord();
        applyDto(repairRecord, repairRecordDto);
        repairRecord.setCreatedAt(new Date());
        repairRecord.setJobOrderNumber("PENDING");
        repairRecord.setImageFileName(handleFileUpload(repairRecordDto.getImageFile()));
        repairRecord = repo.save(repairRecord);

        repairRecord.setJobOrderNumber(generateJobOrderNumber(repairRecord.getRepairId()));
        return repo.save(repairRecord);
    }

    private String generateJobOrderNumber(Long repairId) {
        return String.format("JO-%05d", repairId);
    }

    public RepairRecord createFromTicket(Client client, TicketDto ticketDto) {
        RepairRecord repairRecord = new RepairRecord();
        repairRecord.setClient(client);
        repairRecord.setDeviceType(ticketDto.getDeviceType());
        repairRecord.setBrand(ticketDto.getBrand());
        repairRecord.setModelName(ticketDto.getModelName());
        repairRecord.setSerialNumber(ticketDto.getSerialNumber());
        repairRecord.setIssueDescription(ticketDto.getIssueDescription());
        repairRecord.setCost(ticketDto.getEstimatedCost());
        repairRecord.setRepairDate(new Date());
        repairRecord.setStatus(RepairRecord.RepairStatus.PENDING);
        repairRecord.setCreatedAt(new Date());
        repairRecord.setJobOrderNumber("PENDING");
        repairRecord = repo.save(repairRecord);

        repairRecord.setJobOrderNumber(generateJobOrderNumber(repairRecord.getRepairId()));
        return repo.save(repairRecord);
    }

    // A scheduled appointment (see AppointmentController) is the customer's own intake form for a
    // walk-in repair - it should show up in Repair History right away so admins can see it's
    // coming, with only the fields the customer actually provided. Everything else (brand, model,
    // serial number, technician, cost, fix, recommendation, final status) is filled in later by
    // an admin via the normal /repair/edit form once the device is physically received.
    public RepairRecord createFromAppointment(Client client, AppointmentDto appointmentDto, Appointment appointment) {
        RepairRecord repairRecord = new RepairRecord();
        repairRecord.setClient(client);
        repairRecord.setAppointment(appointment);
        repairRecord.setDeviceType(appointmentDto.getDeviceType());
        repairRecord.setIssueDescription(appointmentDto.getIssueDescription());
        repairRecord.setRepairDate(Date.from(appointmentDto.getPreferredDate()
                .atTime(appointmentDto.getPreferredTime())
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        repairRecord.setStatus(RepairRecord.RepairStatus.PENDING);
        repairRecord.setCreatedAt(new Date());
        repairRecord.setJobOrderNumber("PENDING");
        repairRecord = repo.save(repairRecord);

        repairRecord.setJobOrderNumber(generateJobOrderNumber(repairRecord.getRepairId()));
        return repo.save(repairRecord);
    }

    // A customer's own Repair History: everything tied to a Client their account is linked to,
    // minus any appointment-sourced record whose appointment isn't CONFIRMED yet (see
    // RepairRecord.isVisible).
    public List<RepairRecord> getRepairRecordsForUser(String username) {
        return repo.findByClient_User_UsernameOrderByRepairDateDesc(username).stream()
                .filter(RepairRecord::isVisible)
                .collect(Collectors.toList());
    }

    public RepairRecord updateRepairRecord(Long id, RepairRecordDto repairRecordDto) {
        RepairRecord repairRecord = getRepairRecordById(id);
        applyDto(repairRecord, repairRecordDto);

        if (repairRecordDto.getImageFile() != null && !repairRecordDto.getImageFile().isEmpty()) {
            deleteImageFile(repairRecord.getImageFileName());
            repairRecord.setImageFileName(handleFileUpload(repairRecordDto.getImageFile()));
        }

        return repo.save(repairRecord);
    }

    public void removePhoto(Long id) {
        RepairRecord repairRecord = getRepairRecordById(id);
        deleteImageFile(repairRecord.getImageFileName());
        repairRecord.setImageFileName(null);
        repo.save(repairRecord);
    }

    public void deleteRepairRecord(Long id) {
        RepairRecord repairRecord = getRepairRecordById(id);
        deleteImageFile(repairRecord.getImageFileName());
        repo.delete(repairRecord);
    }

    private Client getClient(Long clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client Id: " + clientId));
    }

    private void applyDto(RepairRecord repairRecord, RepairRecordDto repairRecordDto) {
        repairRecord.setClient(getClient(repairRecordDto.getClientId()));
        repairRecord.setDeviceType(repairRecordDto.getDeviceType());
        repairRecord.setBrand(repairRecordDto.getBrand());
        repairRecord.setModelName(repairRecordDto.getModelName());
        repairRecord.setSerialNumber(repairRecordDto.getSerialNumber());
        repairRecord.setIssueDescription(repairRecordDto.getIssueDescription());
        repairRecord.setTechnician(repairRecordDto.getTechnician());
        repairRecord.setCost(repairRecordDto.getCost());
        repairRecord.setRepairDate(repairRecordDto.getRepairDate());
        repairRecord.setWarrantyEndDate(repairRecordDto.getWarrantyEndDate());
        repairRecord.setStatus(RepairRecord.RepairStatus.valueOf(repairRecordDto.getStatus()));
        repairRecord.setRemarks(repairRecordDto.getRemarks());
        repairRecord.setFix(repairRecordDto.getFix());
        repairRecord.setRecommendation(repairRecordDto.getRecommendation());
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
}
