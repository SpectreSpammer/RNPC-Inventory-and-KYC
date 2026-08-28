package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.RamPartsDto;
import com.rnpc.inventory.entity.RamParts;
import com.rnpc.inventory.repository.RamPartsRepository;
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

@Service
public class RamPartsService {
    private final RamPartsRepository repo;

    @Autowired
    public RamPartsService(RamPartsRepository repo) {
        this.repo = repo;
    }

    public List<RamParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "ramId"));
    }

    public RamParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid RAM part Id: " + id));
    }

    public RamParts saveComponent(RamPartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        RamParts part = new RamParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public RamParts updateComponent(int id, RamPartsDto dto) {
        RamParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        RamParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        RamParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(RamParts part, RamPartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setModelName(dto.getModelName());
        part.setType(dto.getType());
        part.setKitCapacity(dto.getKitCapacity());
        part.setModuleConfig(dto.getModuleConfig());
        part.setSpeedMts(dto.getSpeedMts());
        part.setCasLatency(dto.getCasLatency());
        part.setVoltage(dto.getVoltage());
        part.setRgb(dto.getRgb());
        part.setTypicalUse(dto.getTypicalUse());
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

        List<RamParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/ram.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load RAM catalog: " + ex.getMessage(), ex);
        }

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        List<List<String>> records = parseCsv(content);
        for (int i = 1; i < records.size(); i++) {
            List<String> f = records.get(i);
            if (f.size() < 12) {
                continue;
            }

            RamParts part = new RamParts();
            part.setBrand(f.get(0));
            part.setModelName(f.get(1));
            part.setType(f.get(2));
            part.setKitCapacity(f.get(3));
            part.setModuleConfig(f.get(4));
            part.setSpeedMts(Integer.parseInt(f.get(5)));
            part.setCasLatency(f.get(6));
            part.setVoltage(Double.parseDouble(f.get(7)));
            part.setRgb(f.get(8));
            part.setTypicalUse(f.get(9));
            part.setPrice(Double.parseDouble(f.get(10)));
            part.setStocks(Integer.parseInt(f.get(11)));
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
        for (RamParts part : repo.findAll()) {
            String specs = part.getKitCapacity() + " kit (" + part.getModuleConfig() + "), Speed: "
                    + part.getSpeedMts() + " MT/s, " + part.getCasLatency() + ", " + part.getVoltage()
                    + "V, RGB: " + part.getRgb() + ", Use: " + part.getTypicalUse();
            BuildPartView view = new BuildPartView(part.getRamId(), "RAM", part.getBrand(), part.getModelName(),
                    null, part.getType(), specs, part.getPrice(), part.getStocks());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Model", part.getModelName());
            fields.put("Type", part.getType());
            fields.put("Kit Capacity", part.getKitCapacity());
            fields.put("Module Config", part.getModuleConfig());
            fields.put("Speed(MT/s)", String.valueOf(part.getSpeedMts()));
            fields.put("CAS Latency", part.getCasLatency());
            fields.put("Voltage", String.valueOf(part.getVoltage()));
            fields.put("RGB", part.getRgb());
            fields.put("Typical Use", part.getTypicalUse());
            view.setFields(fields);
            view.setImageFileName(part.getImageFileName());
            views.add(view);
        }
        return views;
    }
}
