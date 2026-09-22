package com.rnpc.inventory.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.LaptopParts.PartType;
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

    /**
     * Type-based create (/laptop/{type}/create). The type comes from the route, never from the
     * form. Spec fields belonging to other types are nulled before saving.
     */
    public LaptopParts saveTypedPart(PartType type, LaptopPartsDto laptopPartsDto) {
        LaptopParts laptopPart = new LaptopParts();
        laptopPart.setPartType(type);
        copyTypedFields(laptopPart, laptopPartsDto);
        clearOtherTypesSpecs(laptopPart);
        laptopPart.setCreatedAt(new Date());
        laptopPart.setImageFileName(handleFileUpload(laptopPartsDto.getImageFile()));
        return repo.save(laptopPart);
    }

    /**
     * Update for both models. A pre-redesign row (partType null) is updated from the old form's
     * fields exactly as before; a typed row takes the new fields, keeps its type, and has other
     * types' spec fields nulled.
     */
    public LaptopParts updateLaptopPart(int id, LaptopPartsDto laptopPartsDto) {
        LaptopParts laptopPart = getLaptopPartById(id);
        if (laptopPart.getPartType() == null) {
            updateEntity(laptopPart, laptopPartsDto);
        } else {
            copyTypedFields(laptopPart, laptopPartsDto);
            clearOtherTypesSpecs(laptopPart);
        }

        if (laptopPartsDto.getImageFile() != null && !laptopPartsDto.getImageFile().isEmpty()) {
            deleteImageFile(laptopPart.getImageFileName());
            String storageFileName = handleFileUpload(laptopPartsDto.getImageFile());
            laptopPart.setImageFileName(storageFileName);
        }

        return repo.save(laptopPart);
    }

    public void removePhoto(int id) {
        LaptopParts laptopPart = getLaptopPartById(id);
        deleteImageFile(laptopPart.getImageFileName());
        laptopPart.setImageFileName(null);
        repo.save(laptopPart);
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

    /** New-model fields, DTO to entity. Strings are trimmed and blank ones stored as null. */
    private void copyTypedFields(LaptopParts laptopPart, LaptopPartsDto dto) {
        laptopPart.setBrand(blankToNull(dto.getBrand()));
        laptopPart.setPartName(blankToNull(dto.getPartName()));
        laptopPart.setStocks(dto.getStocks());
        laptopPart.setPrice(dto.getPrice());
        laptopPart.setCompatibleModels(blankToNull(dto.getCompatibleModels()));
        laptopPart.setPartNumber(blankToNull(dto.getPartNumber()));
        laptopPart.setPartCondition(dto.getPartCondition());
        laptopPart.setWarrantyDays(dto.getWarrantyDays());
        laptopPart.setNotes(blankToNull(dto.getDescription()));
        laptopPart.setLcdSizeInches(dto.getLcdSizeInches());
        laptopPart.setLcdResolution(blankToNull(dto.getLcdResolution()));
        laptopPart.setLcdPanelType(blankToNull(dto.getLcdPanelType()));
        laptopPart.setLcdConnector(blankToNull(dto.getLcdConnector()));
        laptopPart.setLcdRefreshRateHz(dto.getLcdRefreshRateHz());
        laptopPart.setLcdSurface(blankToNull(dto.getLcdSurface()));
        laptopPart.setLcdTouch(dto.getLcdTouch());
        laptopPart.setLcdMounting(blankToNull(dto.getLcdMounting()));
        laptopPart.setKbLayout(blankToNull(dto.getKbLayout()));
        laptopPart.setKbBacklit(dto.getKbBacklit());
        laptopPart.setKbColor(blankToNull(dto.getKbColor()));
        laptopPart.setKbWithPalmrest(dto.getKbWithPalmrest());
        laptopPart.setKbWithFrame(dto.getKbWithFrame());
        laptopPart.setBatteryCapacityWh(dto.getBatteryCapacityWh());
        laptopPart.setBatteryVoltage(dto.getBatteryVoltage());
        laptopPart.setBatteryCells(dto.getBatteryCells());
        laptopPart.setBatteryChemistry(blankToNull(dto.getBatteryChemistry()));
        laptopPart.setChargerWattage(dto.getChargerWattage());
        laptopPart.setChargerOutputVoltage(dto.getChargerOutputVoltage());
        laptopPart.setChargerCurrentA(dto.getChargerCurrentA());
        laptopPart.setChargerConnectorTip(blankToNull(dto.getChargerConnectorTip()));
        laptopPart.setChargerIncludesCord(dto.getChargerIncludesCord());
        laptopPart.setRamType(blankToNull(dto.getRamType()));
        laptopPart.setRamCapacityGb(dto.getRamCapacityGb());
        laptopPart.setRamSpeedMts(dto.getRamSpeedMts());
        laptopPart.setStorageType(blankToNull(dto.getStorageType()));
        laptopPart.setStorageCapacityGb(dto.getStorageCapacityGb());
        laptopPart.setStorageFormFactor(blankToNull(dto.getStorageFormFactor()));
        laptopPart.setStorageInterface(blankToNull(dto.getStorageInterface()));
        laptopPart.setCasingPanel(blankToNull(dto.getCasingPanel()));
        laptopPart.setCasingColor(blankToNull(dto.getCasingColor()));
        laptopPart.setHingeSide(blankToNull(dto.getHingeSide()));
        laptopPart.setDcJackType(blankToNull(dto.getDcJackType()));
        laptopPart.setDcJackTipSize(blankToNull(dto.getDcJackTipSize()));
        laptopPart.setWifiStandard(blankToNull(dto.getWifiStandard()));
        laptopPart.setWifiFormFactor(blankToNull(dto.getWifiFormFactor()));
        laptopPart.setWifiBluetoothVersion(blankToNull(dto.getWifiBluetoothVersion()));
        laptopPart.setTouchpadConnector(blankToNull(dto.getTouchpadConnector()));
        laptopPart.setTouchpadWithBracket(dto.getTouchpadWithBracket());
        laptopPart.setTouchpadColor(blankToNull(dto.getTouchpadColor()));
    }

    /** Nulls every spec field that does not belong to the part's own type. */
    private static void clearOtherTypesSpecs(LaptopParts laptopPart) {
        PartType type = laptopPart.getPartType();
        if (type != PartType.LCD) {
            laptopPart.setLcdSizeInches(null);
            laptopPart.setLcdResolution(null);
            laptopPart.setLcdPanelType(null);
            laptopPart.setLcdConnector(null);
            laptopPart.setLcdRefreshRateHz(null);
            laptopPart.setLcdSurface(null);
            laptopPart.setLcdTouch(null);
            laptopPart.setLcdMounting(null);
        }
        if (type != PartType.KEYBOARD) {
            laptopPart.setKbLayout(null);
            laptopPart.setKbBacklit(null);
            laptopPart.setKbColor(null);
            laptopPart.setKbWithPalmrest(null);
            laptopPart.setKbWithFrame(null);
        }
        if (type != PartType.BATTERY) {
            laptopPart.setBatteryCapacityWh(null);
            laptopPart.setBatteryVoltage(null);
            laptopPart.setBatteryCells(null);
            laptopPart.setBatteryChemistry(null);
        }
        if (type != PartType.CHARGER) {
            laptopPart.setChargerWattage(null);
            laptopPart.setChargerOutputVoltage(null);
            laptopPart.setChargerCurrentA(null);
            laptopPart.setChargerConnectorTip(null);
            laptopPart.setChargerIncludesCord(null);
        }
        if (type != PartType.RAM) {
            laptopPart.setRamType(null);
            laptopPart.setRamCapacityGb(null);
            laptopPart.setRamSpeedMts(null);
        }
        if (type != PartType.STORAGE) {
            laptopPart.setStorageType(null);
            laptopPart.setStorageCapacityGb(null);
            laptopPart.setStorageFormFactor(null);
            laptopPart.setStorageInterface(null);
        }
        if (type != PartType.CASING) {
            laptopPart.setCasingPanel(null);
            laptopPart.setCasingColor(null);
        }
        if (type != PartType.HINGES) {
            laptopPart.setHingeSide(null);
        }
        if (type != PartType.DC_JACK) {
            laptopPart.setDcJackType(null);
            laptopPart.setDcJackTipSize(null);
        }
        if (type != PartType.WIFI_CARD) {
            laptopPart.setWifiStandard(null);
            laptopPart.setWifiFormFactor(null);
            laptopPart.setWifiBluetoothVersion(null);
        }
        if (type != PartType.TOUCHPAD) {
            laptopPart.setTouchpadConnector(null);
            laptopPart.setTouchpadWithBracket(null);
            laptopPart.setTouchpadColor(null);
        }
    }

    /** Entity to DTO for the type-based edit forms. */
    public LaptopPartsDto toDto(LaptopParts laptopPart) {
        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand(laptopPart.getBrand());
        dto.setPartName(laptopPart.getPartName());
        dto.setStocks(laptopPart.getStocks());
        dto.setPrice(laptopPart.getPrice());
        dto.setCompatibleModels(laptopPart.getCompatibleModels());
        dto.setPartNumber(laptopPart.getPartNumber());
        dto.setPartCondition(laptopPart.getPartCondition());
        dto.setWarrantyDays(laptopPart.getWarrantyDays());
        dto.setDescription(laptopPart.getNotes());
        dto.setLcdSizeInches(laptopPart.getLcdSizeInches());
        dto.setLcdResolution(laptopPart.getLcdResolution());
        dto.setLcdPanelType(laptopPart.getLcdPanelType());
        dto.setLcdConnector(laptopPart.getLcdConnector());
        dto.setLcdRefreshRateHz(laptopPart.getLcdRefreshRateHz());
        dto.setLcdSurface(laptopPart.getLcdSurface());
        dto.setLcdTouch(laptopPart.getLcdTouch());
        dto.setLcdMounting(laptopPart.getLcdMounting());
        dto.setKbLayout(laptopPart.getKbLayout());
        dto.setKbBacklit(laptopPart.getKbBacklit());
        dto.setKbColor(laptopPart.getKbColor());
        dto.setKbWithPalmrest(laptopPart.getKbWithPalmrest());
        dto.setKbWithFrame(laptopPart.getKbWithFrame());
        dto.setBatteryCapacityWh(laptopPart.getBatteryCapacityWh());
        dto.setBatteryVoltage(laptopPart.getBatteryVoltage());
        dto.setBatteryCells(laptopPart.getBatteryCells());
        dto.setBatteryChemistry(laptopPart.getBatteryChemistry());
        dto.setChargerWattage(laptopPart.getChargerWattage());
        dto.setChargerOutputVoltage(laptopPart.getChargerOutputVoltage());
        dto.setChargerCurrentA(laptopPart.getChargerCurrentA());
        dto.setChargerConnectorTip(laptopPart.getChargerConnectorTip());
        dto.setChargerIncludesCord(laptopPart.getChargerIncludesCord());
        dto.setRamType(laptopPart.getRamType());
        dto.setRamCapacityGb(laptopPart.getRamCapacityGb());
        dto.setRamSpeedMts(laptopPart.getRamSpeedMts());
        dto.setStorageType(laptopPart.getStorageType());
        dto.setStorageCapacityGb(laptopPart.getStorageCapacityGb());
        dto.setStorageFormFactor(laptopPart.getStorageFormFactor());
        dto.setStorageInterface(laptopPart.getStorageInterface());
        dto.setCasingPanel(laptopPart.getCasingPanel());
        dto.setCasingColor(laptopPart.getCasingColor());
        dto.setHingeSide(laptopPart.getHingeSide());
        dto.setDcJackType(laptopPart.getDcJackType());
        dto.setDcJackTipSize(laptopPart.getDcJackTipSize());
        dto.setWifiStandard(laptopPart.getWifiStandard());
        dto.setWifiFormFactor(laptopPart.getWifiFormFactor());
        dto.setWifiBluetoothVersion(laptopPart.getWifiBluetoothVersion());
        dto.setTouchpadConnector(laptopPart.getTouchpadConnector());
        dto.setTouchpadWithBracket(laptopPart.getTouchpadWithBracket());
        dto.setTouchpadColor(laptopPart.getTouchpadColor());
        return dto;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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