package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CoolerPartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String type;

    private String radiatorHeight;

    private String socketSupport;

    private String rgb;

    @Size(max = 2000, message = "The notes cannot exceed 2000 characters")
    private String notes;

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

    public String getRadiatorHeight() {
        return radiatorHeight;
    }

    public void setRadiatorHeight(String radiatorHeight) {
        this.radiatorHeight = radiatorHeight;
    }

    public String getSocketSupport() {
        return socketSupport;
    }

    public void setSocketSupport(String socketSupport) {
        this.socketSupport = socketSupport;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
