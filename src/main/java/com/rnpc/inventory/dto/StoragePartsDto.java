package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class StoragePartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String category;
    private String formFactor;
    private String interfaceType;
    private String capacities;

    @Min(value = 0, message = "The value cannot be negative")
    private int seqReadMbs;

    @Min(value = 0, message = "The value cannot be negative")
    private int seqWriteMbs;

    private String dramCache;
    private String warranty;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(String formFactor) {
        this.formFactor = formFactor;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getCapacities() {
        return capacities;
    }

    public void setCapacities(String capacities) {
        this.capacities = capacities;
    }

    public int getSeqReadMbs() {
        return seqReadMbs;
    }

    public void setSeqReadMbs(int seqReadMbs) {
        this.seqReadMbs = seqReadMbs;
    }

    public int getSeqWriteMbs() {
        return seqWriteMbs;
    }

    public void setSeqWriteMbs(int seqWriteMbs) {
        this.seqWriteMbs = seqWriteMbs;
    }

    public String getDramCache() {
        return dramCache;
    }

    public void setDramCache(String dramCache) {
        this.dramCache = dramCache;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
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
