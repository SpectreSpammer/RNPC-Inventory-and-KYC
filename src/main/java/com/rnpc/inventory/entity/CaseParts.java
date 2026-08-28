package com.rnpc.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "rnpc_case_parts")
public class CaseParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int caseId;

    private String brand;
    private String modelName;
    private String towerClass;
    private String motherboardSupport;
    private int maxGpuLengthMm;
    private double price;
    private int stocks;
    private String imageFileName;
    private Date createdAt;

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
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

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
