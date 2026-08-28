package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class PsuPartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    @Min(value = 0, message = "This value cannot be negative")
    private int wattageW;

    private String plusRating;
    private String modularity;
    private String formFactor;

    @Min(value = 0, message = "This value cannot be negative")
    private int warrantyYears;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
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

    public int getWattageW() {
        return wattageW;
    }

    public void setWattageW(int wattageW) {
        this.wattageW = wattageW;
    }

    public String getPlusRating() {
        return plusRating;
    }

    public void setPlusRating(String plusRating) {
        this.plusRating = plusRating;
    }

    public String getModularity() {
        return modularity;
    }

    public void setModularity(String modularity) {
        this.modularity = modularity;
    }

    public String getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(String formFactor) {
        this.formFactor = formFactor;
    }

    public int getWarrantyYears() {
        return warrantyYears;
    }

    public void setWarrantyYears(int warrantyYears) {
        this.warrantyYears = warrantyYears;
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
