package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.StoragePartsDto;
import com.rnpc.inventory.entity.StorageParts;
import com.rnpc.inventory.repository.StoragePartsRepository;
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
public class StoragePartsService {
    private final StoragePartsRepository repo;

    @Autowired
    public StoragePartsService(StoragePartsRepository repo) {
        this.repo = repo;
    }

    public List<StorageParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "storageId"));
    }

    public StorageParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Storage part Id: " + id));
    }

    public List<String> getDistinctBrands() {
        return repo.findAll().stream().map(StorageParts::getBrand)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctCategories() {
        return repo.findAll().stream().map(StorageParts::getCategory)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctFormFactors() {
        return repo.findAll().stream().map(StorageParts::getFormFactor)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctDramCacheOptions() {
        return repo.findAll().stream().map(StorageParts::getDramCache)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public StorageParts saveComponent(StoragePartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        StorageParts part = new StorageParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public StorageParts updateComponent(int id, StoragePartsDto dto) {
        StorageParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        StorageParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        StorageParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(StorageParts part, StoragePartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setModelName(dto.getModelName());
        part.setCategory(dto.getCategory());
        part.setFormFactor(dto.getFormFactor());
        part.setInterfaceType(dto.getInterfaceType());
        part.setCapacities(dto.getCapacities());
        part.setSeqReadMbs(dto.getSeqReadMbs());
        part.setSeqWriteMbs(dto.getSeqWriteMbs());
        part.setDramCache(dto.getDramCache());
        part.setWarranty(dto.getWarranty());
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

        List<StorageParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/storage.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load Storage catalog: " + ex.getMessage(), ex);
        }

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        List<List<String>> records = parseCsv(content);
        for (int i = 1; i < records.size(); i++) {
            List<String> f = records.get(i);
            if (f.size() < 13) {
                continue;
            }

            StorageParts part = new StorageParts();
            part.setBrand(f.get(0));
            part.setModelName(f.get(1));
            part.setCategory(f.get(2));
            part.setFormFactor(f.get(3));
            part.setInterfaceType(f.get(4));
            part.setCapacities(f.get(5));
            part.setSeqReadMbs(Integer.parseInt(f.get(6)));
            part.setSeqWriteMbs(Integer.parseInt(f.get(7)));
            part.setDramCache(f.get(8));
            part.setWarranty(f.get(9));
            part.setTypicalUse(f.get(10));
            part.setPrice(Double.parseDouble(f.get(11)));
            part.setStocks(Integer.parseInt(f.get(12)));
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
        for (StorageParts part : repo.findAll()) {
            String specs = part.getCategory() + ", " + part.getFormFactor() + ", " + part.getInterfaceType()
                    + ", Capacities: " + part.getCapacities() + ", Seq R/W: " + part.getSeqReadMbs() + "/"
                    + part.getSeqWriteMbs() + " MB/s, DRAM Cache: " + part.getDramCache() + ", Warranty: "
                    + part.getWarranty() + ", Use: " + part.getTypicalUse();
            BuildPartView view = new BuildPartView(part.getStorageId(), "STORAGE", part.getBrand(), part.getModelName(),
                    null, null, specs, part.getPrice(), part.getStocks());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Model", part.getModelName());
            fields.put("Category", part.getCategory());
            fields.put("Form Factor", part.getFormFactor());
            fields.put("Interface", part.getInterfaceType());
            fields.put("Capacities", part.getCapacities());
            fields.put("Seq. Read(MB/s)", String.valueOf(part.getSeqReadMbs()));
            fields.put("Seq. Write(MB/s)", String.valueOf(part.getSeqWriteMbs()));
            fields.put("DRAM Cache", part.getDramCache());
            fields.put("Warranty", part.getWarranty());
            fields.put("Typical Use", part.getTypicalUse());
            view.setFields(fields);
            view.setImageFileName(part.getImageFileName());
            views.add(view);
        }
        return views;
    }
}
