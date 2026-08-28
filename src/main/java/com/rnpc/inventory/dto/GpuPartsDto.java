package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class GpuPartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    private String series;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String architecture;

    private String vram;

    private String memoryType;

    @Min(value = 0, message = "The recommended PSU wattage cannot be negative")
    private int recommendedPsuW;

    private String powerConnector;

    private String typicalAibPartners;

    @Min(value = 0, message = "The launch year cannot be negative")
    private int launchYear;

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

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getVram() {
        return vram;
    }

    public void setVram(String vram) {
        this.vram = vram;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public int getRecommendedPsuW() {
        return recommendedPsuW;
    }

    public void setRecommendedPsuW(int recommendedPsuW) {
        this.recommendedPsuW = recommendedPsuW;
    }

    public String getPowerConnector() {
        return powerConnector;
    }

    public void setPowerConnector(String powerConnector) {
        this.powerConnector = powerConnector;
    }

    public String getTypicalAibPartners() {
        return typicalAibPartners;
    }

    public void setTypicalAibPartners(String typicalAibPartners) {
        this.typicalAibPartners = typicalAibPartners;
    }

    public int getLaunchYear() {
        return launchYear;
    }

    public void setLaunchYear(int launchYear) {
        this.launchYear = launchYear;
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
