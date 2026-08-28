package com.rnpc.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rnpc_storage_parts")
public class StorageParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int storageId;

    private String brand;
    private String modelName;
    private String category;
    private String formFactor;
    private String interfaceType;
    private String capacities;
    private int seqReadMbs;
    private int seqWriteMbs;
    private String dramCache;
    private String warranty;
    private String typicalUse;
    private double price;
    private int stocks;
    private String imageFileName;
    private java.util.Date createdAt;

    public int getStorageId() {
        return storageId;
    }

    public void setStorageId(int storageId) {
        this.storageId = storageId;
    }

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

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public java.util.Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }
}
