package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class CasePartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String towerClass;
    private String motherboardSupport;

    @Min(value = 0, message = "This value cannot be negative")
    private int maxGpuLengthMm;

    @Min(value = 0, message = "The price cannot be negative")
    private double price;

    @Min(value = 0, message = "The stocks cannot be negative")
    private int stocks;

    private MultipartFile imageFile;

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTowerClass() {
        return towerClass;
    }

    public void setTowerClass(String towerClass) {
        this.towerClass = towerClass;
    }

    public String getMotherboardSupport() {
        return motherboardSupport;
    }

    public void setMotherboardSupport(String motherboardSupport) {
        this.motherboardSupport = motherboardSupport;
    }

    public int getMaxGpuLengthMm() {
        return maxGpuLengthMm;
    }

    public void setMaxGpuLengthMm(int maxGpuLengthMm) {
        this.maxGpuLengthMm = maxGpuLengthMm;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStocks() {
        return stocks;
    }

    public void setStocks(int stocks) {
        this.stocks = stocks;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
