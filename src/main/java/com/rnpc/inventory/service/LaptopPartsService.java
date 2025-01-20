package com.rnpc.inventory.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import com.rnpc.inventory.entity.Computers;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.repository.LaptopPartsRepository;

@Service
public class LaptopPartsService {

    private final LaptopPartsRepository repo;

    @Autowired
    public LaptopPartsService(LaptopPartsRepository repo) {
        this.repo = repo;
    }

    public List<LaptopParts> getAllLaptopParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "laptopPartId"));
    }

    public LaptopParts getLaptopPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Laptop part with ID " + id + " not found"));
    }

    public LaptopParts saveLaptopPart(LaptopPartsDto laptopPartsDto) {
        String storageFileName = handleFileUpload(laptopPartsDto.getImageFile());
        LaptopParts laptopPart = mapToEntity(laptopPartsDto);
        laptopPart.setCreatedAt(new Date());
        laptopPart.setImageFileName(storageFileName);
        return repo.save(laptopPart);
    }

    public LaptopParts updateLaptopPart(int id, LaptopPartsDto laptopPartsDto) {
        LaptopParts laptopPart = getLaptopPartById(id);
        updateEntity(laptopPart, laptopPartsDto);

        if (laptopPartsDto.getImageFile() != null && !laptopPartsDto.getImageFile().isEmpty()) {
            String storageFileName = handleFileUpload(laptopPartsDto.getImageFile());
            laptopPart.setImageFileName(storageFileName);
        }

        return repo.save(laptopPart);
    }

    public void deleteLaptopPart(int id) {
        LaptopParts laptopPart = getLaptopPartById(id);
        deleteImageFile(laptopPart.getImageFileName());
        repo.delete(laptopPart);
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

    private LaptopParts mapToEntity(LaptopPartsDto laptopPartsDto) {
        LaptopParts laptopPart = new LaptopParts();
        laptopPart.setBrand(laptopPartsDto.getBrand());
        laptopPart.setPartName(laptopPartsDto.getPartName());
        laptopPart.setCategory(laptopPartsDto.getCategory());
        laptopPart.setStorageSize(laptopPartsDto.getStorageSize());
        laptopPart.setStocks(laptopPartsDto.getStocks());
        laptopPart.setPrice(laptopPartsDto.getPrice());
        laptopPart.setDescription(laptopPartsDto.getDescription());
        return laptopPart;
    }

    private void updateEntity(LaptopParts laptopPart, LaptopPartsDto laptopPartsDto) {
        laptopPart.setBrand(laptopPartsDto.getBrand());
        laptopPart.setPartName(laptopPartsDto.getPartName());
        laptopPart.setCategory(laptopPartsDto.getCategory());
        laptopPart.setStorageSize(laptopPartsDto.getStorageSize());
        laptopPart.setStocks(laptopPartsDto.getStocks());
        laptopPart.setPrice(laptopPartsDto.getPrice());
        laptopPart.setDescription(laptopPartsDto.getDescription());
    }
}