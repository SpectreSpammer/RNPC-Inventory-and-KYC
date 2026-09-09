package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.MotherboardPartsDto;
import com.rnpc.inventory.entity.MotherboardParts;
import com.rnpc.inventory.repository.MotherboardPartsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class MotherboardPartsService {
    private final MotherboardPartsRepository repo;

    @Autowired
    public MotherboardPartsService(MotherboardPartsRepository repo) {
        this.repo = repo;
    }

    public List<MotherboardParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "motherboardId"));
    }

    public MotherboardParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Motherboard part Id: " + id));
    }

    public List<String> getDistinctBrands() {
        return repo.findAll().stream().map(MotherboardParts::getBrand)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctSockets() {
        return repo.findAll().stream().map(MotherboardParts::getSocket)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctFormFactors() {
        return repo.findAll().stream().map(MotherboardParts::getFormFactor)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctMemoryTypes() {
        return repo.findAll().stream().map(MotherboardParts::getMemoryType)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public MotherboardParts saveComponent(MotherboardPartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        MotherboardParts part = new MotherboardParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public MotherboardParts updateComponent(int id, MotherboardPartsDto dto) {
        MotherboardParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        MotherboardParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        MotherboardParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(MotherboardParts part, MotherboardPartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setModelName(dto.getModelName());
        part.setChipset(dto.getChipset());
        part.setSocket(dto.getSocket());
        part.setCpuPlatform(dto.getCpuPlatform());
        part.setFormFactor(dto.getFormFactor());
        part.setMemoryType(dto.getMemoryType());
        part.setRamSlots(dto.getRamSlots());
        part.setMaxRam(dto.getMaxRam());
        part.setM2Slots(dto.getM2Slots());
        part.setPcieVersion(dto.getPcieVersion());
        part.setNotableFeatures(dto.getNotableFeatures());
        part.setPrice(dto.getPrice());
        part.setStocks(dto.getStocks());
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

    public void seedReferenceCatalogIfEmpty() {
        if (repo.count() > 0) {
            return;
        }

        List<MotherboardParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/motherboard.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load Motherboard catalog: " + ex.getMessage(), ex);
        }

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        List<List<String>> records = parseCsv(content);
        for (int i = 1; i < records.size(); i++) {
            List<String> f = records.get(i);
            if (f.size() < 14) {
                continue;
            }

            MotherboardParts part = new MotherboardParts();
            part.setBrand(f.get(0));
            part.setModelName(f.get(1));
            part.setChipset(f.get(2));
            part.setSocket(f.get(3));
            part.setCpuPlatform(f.get(4));
            part.setFormFactor(f.get(5));
            part.setMemoryType(f.get(6));
            part.setRamSlots(Integer.parseInt(f.get(7)));
            part.setMaxRam(f.get(8));
            part.setM2Slots(Integer.parseInt(f.get(9)));
            part.setPcieVersion(f.get(10));
            part.setNotableFeatures(f.get(11));
            part.setPrice(Double.parseDouble(f.get(12)));
            part.setStocks(Integer.parseInt(f.get(13)));
            parts.add(part);
        }

        repo.saveAll(parts);
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int len = content.length();

        int i = 0;
        while (i < len) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    currentField.append(c);
                    i++;
                }
                continue;
            }

            if (c == '"') {
                inQuotes = true;
                i++;
            } else if (c == ',') {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
                i++;
            } else if (c == '\r') {
                i++;
            } else if (c == '\n') {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
                records.add(currentRecord);
                currentRecord = new ArrayList<>();
                i++;
            } else {
                currentField.append(c);
                i++;
            }
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString());
            records.add(currentRecord);
        }

        return records;
    }

    public List<BuildPartView> getAllAsBuildParts() {
        List<BuildPartView> views = new ArrayList<>();
        for (MotherboardParts part : repo.findAll()) {
            String specs = part.getChipset() + " chipset, " + part.getFormFactor() + ", "
                    + part.getMemoryType() + ", " + part.getRamSlots() + " RAM slots (max " + part.getMaxRam()
                    + "), " + part.getM2Slots() + " M.2 slots, " + part.getPcieVersion() + ", "
                    + part.getNotableFeatures();
            BuildPartView view = new BuildPartView(part.getMotherboardId(), "MOTHERBOARD", part.getBrand(), part.getModelName(),
                    part.getSocket(), part.getMemoryType(), specs, part.getPrice(), part.getStocks());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Model", part.getModelName());
            fields.put("Chipset", part.getChipset());
            fields.put("Socket", part.getSocket());
            fields.put("CPU Platform", part.getCpuPlatform());
            fields.put("Form Factor", part.getFormFactor());
            fields.put("Memory Type", part.getMemoryType());
            fields.put("RAM Slots", String.valueOf(part.getRamSlots()));
            fields.put("Max Ram", part.getMaxRam());
            fields.put("M.2 Slots", String.valueOf(part.getM2Slots()));
            fields.put("PCIe Version", part.getPcieVersion());
            fields.put("Notable Features", part.getNotableFeatures());
            view.setFields(fields);
            view.setImageFileName(part.getImageFileName());
            views.add(view);
        }
        return views;
    }
}
