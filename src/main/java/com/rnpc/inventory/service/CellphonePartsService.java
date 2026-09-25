package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.entity.CellphoneParts.PartType;
import com.rnpc.inventory.repository.CellphonePartsRepository;
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
public class CellphonePartsService {
    private final CellphonePartsRepository cellphonePartsRepository;

    @Autowired
    public CellphonePartsService(CellphonePartsRepository cellphonePartsRepository) {
        this.cellphonePartsRepository = cellphonePartsRepository;
    }

    public List<CellphoneParts> getAllCellphoneParts(){
        return cellphonePartsRepository.findAll(Sort.by(Sort.Direction.DESC, "cellphonePartId"));
    }

    public CellphoneParts getCellphonePartById(Long id){
        return cellphonePartsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cellphone part with ID " + id + " not found"));
    }

    public CellphoneParts saveCellphonePart(CellphonePartsDto cellphonePartsDto){
        String storageFileName = handleFileUpload(cellphonePartsDto.getImageFile());
        CellphoneParts cellphoneParts = mapToEntity(cellphonePartsDto);
        cellphoneParts.setCreatedAt(new Date());
        cellphoneParts.setImageFileName(storageFileName);

        return cellphonePartsRepository.save(cellphoneParts);
    }

    /**
     * Type-based create (/cellphone/{type}/create). The type comes from the route, never from the
     * form. Spec fields belonging to other types are nulled before saving.
     */
    public CellphoneParts saveTypedPart(PartType type, CellphonePartsDto dto) {
        CellphoneParts cellphonePart = new CellphoneParts();
        cellphonePart.setPartType(type);
        copyTypedFields(cellphonePart, dto);
        clearOtherTypesSpecs(cellphonePart);
        cellphonePart.setCreatedAt(new Date());
        cellphonePart.setImageFileName(handleFileUpload(dto.getImageFile()));
        return cellphonePartsRepository.save(cellphonePart);
    }

    private String handleFileUpload(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()){
            return null;
        }

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists((uploadPath))){
                Files.createDirectories(uploadPath);
            }

            String fileName = new Date().getTime() + "_" + imageFile.getOriginalFilename();
            try(InputStream inputStream = imageFile.getInputStream()){
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    private CellphoneParts mapToEntity(CellphonePartsDto cellphonePartsDto) {
        CellphoneParts cellphonePart = new CellphoneParts();
        cellphonePart.setBrand(cellphonePartsDto.getBrand());
        cellphonePart.setPartName(cellphonePartsDto.getPartName());
        cellphonePart.setCategory(cellphonePartsDto.getCategory());
        cellphonePart.setStorageSize(cellphonePartsDto.getStorageSize());
        cellphonePart.setStocks(cellphonePartsDto.getStocks());
        cellphonePart.setPrice(cellphonePartsDto.getPrice());
        cellphonePart.setDescription(cellphonePartsDto.getDescription());

        return cellphonePart;
    }

    /**
     * Update for both models. A pre-redesign row (partType null) is updated from the old form's
     * fields exactly as before; a typed row takes the new fields, keeps its type, and has other
     * types' spec fields nulled.
     */
    public CellphoneParts updateCellphonePart(Long id, CellphonePartsDto cellphonePartsDto){
        CellphoneParts cellphonePart = getCellphonePartById(id);
        if (cellphonePart.getPartType() == null) {
            updateEntity(cellphonePart, cellphonePartsDto);
        } else {
            copyTypedFields(cellphonePart, cellphonePartsDto);
            clearOtherTypesSpecs(cellphonePart);
        }

        if (cellphonePartsDto.getImageFile() != null && !cellphonePartsDto.getImageFile().isEmpty()) {
            deleteImageFile(cellphonePart.getImageFileName());
            String storageFileName = handleFileUpload(cellphonePartsDto.getImageFile());
            cellphonePart.setImageFileName(storageFileName);
        }

        return cellphonePartsRepository.save(cellphonePart);
    }

    public void removePhoto(Long id) {
        CellphoneParts cellphonePart = getCellphonePartById(id);
        deleteImageFile(cellphonePart.getImageFileName());
        cellphonePart.setImageFileName(null);
        cellphonePartsRepository.save(cellphonePart);
    }

    private void updateEntity(CellphoneParts cellphonePart, CellphonePartsDto cellphonePartsDto){
        cellphonePart.setBrand(cellphonePartsDto.getBrand());
        cellphonePart.setPartName(cellphonePartsDto.getPartName());
        cellphonePart.setCategory(cellphonePartsDto.getCategory());
        cellphonePart.setStorageSize(cellphonePartsDto.getStorageSize());
        cellphonePart.setStocks(cellphonePartsDto.getStocks());
        cellphonePart.setPrice(cellphonePartsDto.getPrice());
        cellphonePart.setDescription(cellphonePartsDto.getDescription());
    }

    public void deleteCellphonePart(Long id){
        CellphoneParts cellphonePart = getCellphonePartById(id);
        deleteImageFile(cellphonePart.getImageFileName());
        cellphonePartsRepository.delete(cellphonePart);
    }

    private void deleteImageFile(String filename){
        if (filename != null && !filename.isEmpty()){
            try{
                String uploadDir = "public/images/";
                Path filePath = Paths.get(uploadDir + filename);
                Files.deleteIfExists(filePath);
            }catch (Exception e){
                System.out.println("Error deleting file: " + e.getMessage());
            }
        }
    }

    /** New-model fields, DTO to entity. Strings are trimmed and blank ones stored as null. */
    private void copyTypedFields(CellphoneParts cellphonePart, CellphonePartsDto dto) {
        cellphonePart.setBrand(blankToNull(dto.getBrand()));
        cellphonePart.setPartName(blankToNull(dto.getPartName()));
        cellphonePart.setStocks(dto.getStocks());
        cellphonePart.setPrice(dto.getPrice());
        cellphonePart.setCompatibleModels(blankToNull(dto.getCompatibleModels()));
        cellphonePart.setPartNumber(blankToNull(dto.getPartNumber()));
        cellphonePart.setPartCondition(dto.getPartCondition());
        cellphonePart.setWarrantyDays(dto.getWarrantyDays());
        cellphonePart.setNotes(blankToNull(dto.getDescription()));
        cellphonePart.setScreenPanelType(blankToNull(dto.getScreenPanelType()));
        cellphonePart.setScreenGrade(blankToNull(dto.getScreenGrade()));
        cellphonePart.setScreenSizeInches(dto.getScreenSizeInches());
        cellphonePart.setScreenWithFrame(dto.getScreenWithFrame());
        cellphonePart.setScreenTouchIncluded(dto.getScreenTouchIncluded());
        cellphonePart.setBatteryCapacityMah(dto.getBatteryCapacityMah());
        cellphonePart.setBatteryVoltage(dto.getBatteryVoltage());
        cellphonePart.setBatteryChemistry(blankToNull(dto.getBatteryChemistry()));
        cellphonePart.setPortConnector(blankToNull(dto.getPortConnector()));
        cellphonePart.setPortOnFlex(dto.getPortOnFlex());
        cellphonePart.setPortWithMic(dto.getPortWithMic());
        cellphonePart.setCoverMaterial(blankToNull(dto.getCoverMaterial()));
        cellphonePart.setCoverColor(blankToNull(dto.getCoverColor()));
        cellphonePart.setCoverWithLens(dto.getCoverWithLens());
        cellphonePart.setHousingColor(blankToNull(dto.getHousingColor()));
        cellphonePart.setHousingWithButtons(dto.getHousingWithButtons());
        cellphonePart.setHousingWithBackGlass(dto.getHousingWithBackGlass());
        cellphonePart.setFlexFunction(blankToNull(dto.getFlexFunction()));
        cellphonePart.setCameraPosition(blankToNull(dto.getCameraPosition()));
        cellphonePart.setCameraMegapixels(dto.getCameraMegapixels());
        cellphonePart.setCameraModule(dto.getCameraModule());
        cellphonePart.setFingerprintPosition(blankToNull(dto.getFingerprintPosition()));
        cellphonePart.setFingerprintWithFlex(dto.getFingerprintWithFlex());
        cellphonePart.setSensorKind(blankToNull(dto.getSensorKind()));
        cellphonePart.setSensorUnderDisplay(dto.getSensorUnderDisplay());
    }

    /** Nulls every spec field that does not belong to the part's own type. */
    private static void clearOtherTypesSpecs(CellphoneParts cellphonePart) {
        PartType type = cellphonePart.getPartType();
        if (type != PartType.SCREEN) {
            cellphonePart.setScreenPanelType(null);
            cellphonePart.setScreenGrade(null);
            cellphonePart.setScreenSizeInches(null);
            cellphonePart.setScreenWithFrame(null);
            cellphonePart.setScreenTouchIncluded(null);
        }
        if (type != PartType.BATTERY) {
            cellphonePart.setBatteryCapacityMah(null);
            cellphonePart.setBatteryVoltage(null);
            cellphonePart.setBatteryChemistry(null);
        }
        if (type != PartType.CHARGING_BOARD) {
            cellphonePart.setPortConnector(null);
            cellphonePart.setPortOnFlex(null);
            cellphonePart.setPortWithMic(null);
        }
        if (type != PartType.BACK_GLASS) {
            cellphonePart.setCoverMaterial(null);
            cellphonePart.setCoverColor(null);
            cellphonePart.setCoverWithLens(null);
        }
        if (type != PartType.HOUSING) {
            cellphonePart.setHousingColor(null);
            cellphonePart.setHousingWithButtons(null);
            cellphonePart.setHousingWithBackGlass(null);
        }
        if (type != PartType.FLEX_CABLE) {
            cellphonePart.setFlexFunction(null);
        }
        if (type != PartType.CAMERA) {
            cellphonePart.setCameraPosition(null);
            cellphonePart.setCameraMegapixels(null);
            cellphonePart.setCameraModule(null);
        }
        if (type != PartType.FINGERPRINT) {
            cellphonePart.setFingerprintPosition(null);
            cellphonePart.setFingerprintWithFlex(null);
        }
        if (type != PartType.SENSOR) {
            cellphonePart.setSensorKind(null);
            cellphonePart.setSensorUnderDisplay(null);
        }
        // OTHER has no spec fields of its own - the blocks above already clear every other type's.
    }

    /** Entity to DTO for the type-based edit forms. */
    public CellphonePartsDto toDto(CellphoneParts cellphonePart) {
        CellphonePartsDto dto = new CellphonePartsDto();
        dto.setBrand(cellphonePart.getBrand());
        dto.setPartName(cellphonePart.getPartName());
        dto.setStocks(cellphonePart.getStocks());
        dto.setPrice(cellphonePart.getPrice());
        dto.setCompatibleModels(cellphonePart.getCompatibleModels());
        dto.setPartNumber(cellphonePart.getPartNumber());
        dto.setPartCondition(cellphonePart.getPartCondition());
        dto.setWarrantyDays(cellphonePart.getWarrantyDays());
        dto.setDescription(cellphonePart.getNotes());
        dto.setScreenPanelType(cellphonePart.getScreenPanelType());
        dto.setScreenGrade(cellphonePart.getScreenGrade());
        dto.setScreenSizeInches(cellphonePart.getScreenSizeInches());
        dto.setScreenWithFrame(cellphonePart.getScreenWithFrame());
        dto.setScreenTouchIncluded(cellphonePart.getScreenTouchIncluded());
        dto.setBatteryCapacityMah(cellphonePart.getBatteryCapacityMah());
        dto.setBatteryVoltage(cellphonePart.getBatteryVoltage());
        dto.setBatteryChemistry(cellphonePart.getBatteryChemistry());
        dto.setPortConnector(cellphonePart.getPortConnector());
        dto.setPortOnFlex(cellphonePart.getPortOnFlex());
        dto.setPortWithMic(cellphonePart.getPortWithMic());
        dto.setCoverMaterial(cellphonePart.getCoverMaterial());
        dto.setCoverColor(cellphonePart.getCoverColor());
        dto.setCoverWithLens(cellphonePart.getCoverWithLens());
        dto.setHousingColor(cellphonePart.getHousingColor());
        dto.setHousingWithButtons(cellphonePart.getHousingWithButtons());
        dto.setHousingWithBackGlass(cellphonePart.getHousingWithBackGlass());
        dto.setFlexFunction(cellphonePart.getFlexFunction());
        dto.setCameraPosition(cellphonePart.getCameraPosition());
        dto.setCameraMegapixels(cellphonePart.getCameraMegapixels());
        dto.setCameraModule(cellphonePart.getCameraModule());
        dto.setFingerprintPosition(cellphonePart.getFingerprintPosition());
        dto.setFingerprintWithFlex(cellphonePart.getFingerprintWithFlex());
        dto.setSensorKind(cellphonePart.getSensorKind());
        dto.setSensorUnderDisplay(cellphonePart.getSensorUnderDisplay());
        return dto;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
