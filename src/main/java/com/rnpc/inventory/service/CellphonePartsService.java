package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
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


    public CellphoneParts updateCellphonePart(Long id, CellphonePartsDto cellphonePartsDto){
        CellphoneParts cellphonePart = getCellphonePartById(id);
        updateEntity(cellphonePart, cellphonePartsDto);

        if (cellphonePartsDto.getImageFile() != null && !cellphonePartsDto.getImageFile().isEmpty()) {
            String storageFileName = handleFileUpload(cellphonePartsDto.getImageFile());
            cellphonePart.setImageFileName(storageFileName);
        }

        return cellphonePartsRepository.save(cellphonePart);
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
                String uploadDir = "public/images";
                Path filePath = Paths.get(uploadDir + filename);

            }catch (Exception e){
                System.out.println("Error deleting file: " + e.getMessage());
            }
        }
    }
}
