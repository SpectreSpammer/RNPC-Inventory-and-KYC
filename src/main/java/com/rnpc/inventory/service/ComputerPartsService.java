package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.ComputerDto;
import com.rnpc.inventory.entity.Computers;
import com.rnpc.inventory.repository.ComputerPartsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;


@Service
public class ComputerPartsService {
    private final ComputerPartsRepository repo;

    @Autowired
    public ComputerPartsService(ComputerPartsRepository repo) {
        this.repo = repo;
    }

    public List<Computers> getAllComputers() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "productId"));
    }

    public Computers getComputerById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id: " + id));
    }

    public Computers saveComputer(ComputerDto computerDto) {
        String storageFileName = handleFileUpload(computerDto.getImageFile());
        Computers computer = mapToEntity(computerDto);
        computer.setCreatedAt(new Date());
        computer.setImageFileName(storageFileName);
        return repo.save(computer);
    }

    public Computers updateComputer(int id, ComputerDto computerDto) {
        Computers computer = getComputerById(id);
        updateEntity(computer, computerDto);

        if (computerDto.getImageFile() != null && !computerDto.getImageFile().isEmpty()) {
            String storageFileName = handleFileUpload(computerDto.getImageFile());
            computer.setImageFileName(storageFileName);
        }

        return repo.save(computer);
    }

    public void deleteComputer(int id) {
        Computers computer = getComputerById(id);
        deleteImageFile(computer.getImageFileName());
        repo.delete(computer);
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

    private Computers mapToEntity(ComputerDto computerDto) {
        Computers computer = new Computers();
        computer.setBrand(computerDto.getBrand());
        computer.setModelName(computerDto.getModelName());
        computer.setCategory(computerDto.getCategory());
        computer.setStorageSize(computerDto.getStorageSize());
        computer.setStocks(computerDto.getStocks());
        computer.setPrice(computerDto.getPrice());
        computer.setDescription(computerDto.getDescription());
        return computer;
    }

    private void updateEntity(Computers computer, ComputerDto computerDto) {
        computer.setBrand(computerDto.getBrand());
        computer.setModelName(computerDto.getModelName());
        computer.setCategory(computerDto.getCategory());
        computer.setStorageSize(computerDto.getStorageSize());
        computer.setStocks(computerDto.getStocks());
        computer.setPrice(computerDto.getPrice());
        computer.setDescription(computerDto.getDescription());
    }
}
