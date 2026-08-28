package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class RamPartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String type;

    private String kitCapacity;

    private String moduleConfig;

    @Min(value = 0, message = "The speed cannot be negative")
    private int speedMts;

    private String casLatency;

    @Min(value = 0, message = "The voltage cannot be negative")
    private double voltage;

    private String rgb;

    private String typicalUse;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKitCapacity() {
        return kitCapacity;
    }

    public void setKitCapacity(String kitCapacity) {
        this.kitCapacity = kitCapacity;
    }

    public String getModuleConfig() {
        return moduleConfig;
    }

    public void setModuleConfig(String moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public int getSpeedMts() {
        return speedMts;
    }

    public void setSpeedMts(int speedMts) {
        this.speedMts = speedMts;
    }

    public String getCasLatency() {
        return casLatency;
    }

    public void setCasLatency(String casLatency) {
        this.casLatency = casLatency;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }

    public String getTypicalUse() {
        return typicalUse;
    }

    public void setTypicalUse(String typicalUse) {
        this.typicalUse = typicalUse;
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
