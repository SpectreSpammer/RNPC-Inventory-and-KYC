package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.GpuPartsDto;
import com.rnpc.inventory.entity.GpuParts;
import com.rnpc.inventory.repository.GpuPartsRepository;
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
public class GpuPartsService {
    private final GpuPartsRepository repo;

    @Autowired
    public GpuPartsService(GpuPartsRepository repo) {
        this.repo = repo;
    }

    public List<GpuParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "gpuId"));
    }

    public GpuParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid GPU part Id: " + id));
    }

    public List<String> getDistinctBrands() {
        return repo.findAll().stream().map(GpuParts::getBrand)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctMemoryTypes() {
        return repo.findAll().stream().map(GpuParts::getMemoryType)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public GpuParts saveComponent(GpuPartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        GpuParts part = new GpuParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public GpuParts updateComponent(int id, GpuPartsDto dto) {
        GpuParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        GpuParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        GpuParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(GpuParts part, GpuPartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setSeries(dto.getSeries());
        part.setModelName(dto.getModelName());
        part.setArchitecture(dto.getArchitecture());
        part.setVram(dto.getVram());
        part.setMemoryType(dto.getMemoryType());
        part.setRecommendedPsuW(dto.getRecommendedPsuW());
        part.setPowerConnector(dto.getPowerConnector());
        part.setTypicalAibPartners(dto.getTypicalAibPartners());
        part.setLaunchYear(dto.getLaunchYear());
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

        List<GpuParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/gpu.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load GPU catalog: " + ex.getMessage(), ex);
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

            GpuParts part = new GpuParts();
            part.setBrand(f.get(0));
            part.setSeries(f.get(1));
            part.setModelName(f.get(2));
            part.setArchitecture(f.get(3));
            part.setVram(f.get(4));
            part.setMemoryType(f.get(5));
            part.setRecommendedPsuW(Integer.parseInt(f.get(6)));
            part.setPowerConnector(f.get(7));
            part.setTypicalAibPartners(f.get(8));
            part.setLaunchYear(Integer.parseInt(f.get(9)));
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
        for (GpuParts part : repo.findAll()) {
            String specs = part.getArchitecture() + ", " + part.getVram() + " " + part.getMemoryType()
                    + ", Recommended PSU: " + part.getRecommendedPsuW() + "W, Connector: " + part.getPowerConnector()
                    + ", AIB: " + part.getTypicalAibPartners() + ", Launched " + part.getLaunchYear();
            BuildPartView view = new BuildPartView(part.getGpuId(), "GPU", part.getBrand(), part.getModelName(),
                    null, null, specs, part.getPrice(), part.getStocks());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Series", part.getSeries());
            fields.put("Model", part.getModelName());
            fields.put("Architecture", part.getArchitecture());
            fields.put("VRAM", part.getVram());
            fields.put("Memory Type", part.getMemoryType());
            fields.put("Recommended PSU", String.valueOf(part.getRecommendedPsuW()));
            fields.put("Power Connector", part.getPowerConnector());
            fields.put("Typical AIB Partners", part.getTypicalAibPartners());
            fields.put("Launch Year", String.valueOf(part.getLaunchYear()));
            view.setFields(fields);
            view.setImageFileName(part.getImageFileName());
            views.add(view);
        }
        return views;
    }
}
