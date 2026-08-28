package com.rnpc.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rnpc_gpu_parts")
public class GpuParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int gpuId;

    private String brand;
    private String series;
    private String modelName;
    private String architecture;
    private String vram;
    private String memoryType;
    private int recommendedPsuW;
    private String powerConnector;
    private String typicalAibPartners;
    private int launchYear;
    private double price;
    private int stocks;
    private String imageFileName;
    private java.util.Date createdAt;

    public int getGpuId() {
        return gpuId;
    }

    public void setGpuId(int gpuId) {
        this.gpuId = gpuId;
    }

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
