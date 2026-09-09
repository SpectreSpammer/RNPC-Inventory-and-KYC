package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.CasePartsDto;
import com.rnpc.inventory.entity.CaseParts;
import com.rnpc.inventory.repository.CasePartsRepository;
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
public class CasePartsService {
    private final CasePartsRepository repo;

    @Autowired
    public CasePartsService(CasePartsRepository repo) {
        this.repo = repo;
    }

    public List<CaseParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "caseId"));
    }

    public CaseParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Case part Id: " + id));
    }

    public List<String> getDistinctBrands() {
        return repo.findAll().stream().map(CaseParts::getBrand)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctTowerClasses() {
        return repo.findAll().stream().map(CaseParts::getTowerClass)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public CaseParts saveComponent(CasePartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        CaseParts part = new CaseParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public CaseParts updateComponent(int id, CasePartsDto dto) {
        CaseParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        CaseParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        CaseParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(CaseParts part, CasePartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setModelName(dto.getModelName());
        part.setTowerClass(dto.getTowerClass());
        part.setMotherboardSupport(dto.getMotherboardSupport());
        part.setMaxGpuLengthMm(dto.getMaxGpuLengthMm());
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

        List<CaseParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/case.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load Case catalog: " + ex.getMessage(), ex);
        }

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        List<List<String>> records = parseCsv(content);
        for (int i = 1; i < records.size(); i++) {
            List<String> f = records.get(i);
            if (f.size() < 7) {
                continue;
            }

            CaseParts part = new CaseParts();
            part.setBrand(f.get(0));
            part.setModelName(f.get(1));
            part.setTowerClass(f.get(2));
            part.setMotherboardSupport(f.get(3));
            part.setMaxGpuLengthMm(Integer.parseInt(f.get(4)));
            part.setPrice(Double.parseDouble(f.get(5)));
            part.setStocks(Integer.parseInt(f.get(6)));
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
        for (CaseParts part : repo.findAll()) {
            String specs = part.getTowerClass() + ", Supports: " + part.getMotherboardSupport()
                    + ", Max GPU: " + part.getMaxGpuLengthMm() + "mm";
            BuildPartView view = new BuildPartView(part.getCaseId(), "CASE", part.getBrand(), part.getModelName(),
                    null, null, specs, part.getPrice(), part.getStocks());
            view.setImageFileName(part.getImageFileName());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Model", part.getModelName());
            fields.put("Tower Class", part.getTowerClass());
            fields.put("Motherboard Support", part.getMotherboardSupport());
            fields.put("Max GPU Length(mm)", String.valueOf(part.getMaxGpuLengthMm()));
            view.setFields(fields);
            views.add(view);
        }
        return views;
    }
}
