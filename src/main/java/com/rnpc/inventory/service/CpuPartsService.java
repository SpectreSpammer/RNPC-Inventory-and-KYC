package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.CpuPartsDto;
import com.rnpc.inventory.entity.CpuParts;
import com.rnpc.inventory.repository.CpuPartsRepository;
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
public class CpuPartsService {
    private final CpuPartsRepository repo;

    @Autowired
    public CpuPartsService(CpuPartsRepository repo) {
        this.repo = repo;
    }

    public List<CpuParts> getAllParts() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "cpuId"));
    }

    public CpuParts getPartById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid CPU part Id: " + id));
    }

    public List<String> getDistinctBrands() {
        return repo.findAll().stream().map(CpuParts::getBrand)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<String> getDistinctSockets() {
        return repo.findAll().stream().map(CpuParts::getSocket)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    public CpuParts saveComponent(CpuPartsDto dto) {
        String storageFileName = handleFileUpload(dto.getImageFile());
        CpuParts part = new CpuParts();
        applyDto(part, dto);
        part.setCreatedAt(new Date());
        part.setImageFileName(storageFileName);
        return repo.save(part);
    }

    public CpuParts updateComponent(int id, CpuPartsDto dto) {
        CpuParts part = getPartById(id);
        applyDto(part, dto);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            deleteImageFile(part.getImageFileName());
            part.setImageFileName(handleFileUpload(dto.getImageFile()));
        }

        return repo.save(part);
    }

    public void removePhoto(int id) {
        CpuParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        part.setImageFileName(null);
        repo.save(part);
    }

    public void deleteComponent(int id) {
        CpuParts part = getPartById(id);
        deleteImageFile(part.getImageFileName());
        repo.delete(part);
    }

    private void applyDto(CpuParts part, CpuPartsDto dto) {
        part.setBrand(dto.getBrand());
        part.setGeneration(dto.getGeneration());
        part.setModelName(dto.getModelName());
        part.setSocket(dto.getSocket());
        part.setCores(dto.getCores());
        part.setThread(dto.getThread());
        part.setBaseClockGhz(dto.getBaseClockGhz());
        part.setBoostClockGhz(dto.getBoostClockGhz());
        part.setIntegratedGraphics(dto.getIntegratedGraphics());
        part.setMemorySupport(dto.getMemorySupport());
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

        List<CpuParts> parts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("data/cpu.csv");

        String content;
        try (InputStream inputStream = resource.getInputStream()) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load CPU catalog: " + ex.getMessage(), ex);
        }

        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        List<List<String>> records = parseCsv(content);
        for (int i = 1; i < records.size(); i++) { // row 0 is the header
            List<String> f = records.get(i);
            if (f.size() < 13) {
                continue;
            }

            CpuParts part = new CpuParts();
            part.setBrand(f.get(0));
            part.setGeneration(f.get(1));
            part.setModelName(f.get(2));
            part.setSocket(f.get(3));
            part.setCores(Integer.parseInt(f.get(4)));
            part.setThread(Integer.parseInt(f.get(5)));
            part.setBaseClockGhz(Double.parseDouble(f.get(6)));
            part.setBoostClockGhz(Double.parseDouble(f.get(7)));
            part.setIntegratedGraphics(f.get(8));
            part.setMemorySupport(f.get(9));
            part.setLaunchYear(Integer.parseInt(f.get(10)));
            part.setPrice(Double.parseDouble(f.get(11)));
            part.setStocks(Integer.parseInt(f.get(12)));
            parts.add(part);
        }

        repo.saveAll(parts);
    }

    /**
     * Minimal RFC4180 parser over the whole file content (not line-by-line), since quoted fields
     * in this catalog contain embedded commas that a per-line reader would otherwise mishandle.
     * Handles quoted fields, embedded commas/newlines, and escaped ("") quotes; treats CRLF and
     * LF as record separators.
     */
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
        for (CpuParts part : repo.findAll()) {
            String specs = part.getCores() + " Cores / " + part.getThread() + " Threads, "
                    + part.getBaseClockGhz() + "GHz base / " + part.getBoostClockGhz() + "GHz boost, iGPU: "
                    + part.getIntegratedGraphics() + ", Memory: " + part.getMemorySupport() + ", "
                    + part.getGeneration() + ", Launched " + part.getLaunchYear();
            BuildPartView view = new BuildPartView(part.getCpuId(), "CPU", part.getBrand(), part.getModelName(),
                    part.getSocket(), null, specs, part.getPrice(), part.getStocks());
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            fields.put("Brand", part.getBrand());
            fields.put("Generation", part.getGeneration());
            fields.put("Model", part.getModelName());
            fields.put("Socket", part.getSocket());
            fields.put("Cores", String.valueOf(part.getCores()));
            fields.put("Thread", String.valueOf(part.getThread()));
            fields.put("Base Clock(Ghz)", String.valueOf(part.getBaseClockGhz()));
            fields.put("Boost Clock(Ghz)", String.valueOf(part.getBoostClockGhz()));
            fields.put("Integrated Graphics", part.getIntegratedGraphics());
            fields.put("Memory Support", part.getMemorySupport());
            fields.put("Launch Year", String.valueOf(part.getLaunchYear()));
            view.setFields(fields);
            view.setImageFileName(part.getImageFileName());
            views.add(view);
        }
        return views;
    }
}
