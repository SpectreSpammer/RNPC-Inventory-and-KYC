package com.rnpc.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rnpc_motherboard_parts")
public class MotherboardParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int motherboardId;

    private String brand;
    private String modelName;
    private String chipset;
    private String socket;
    private String cpuPlatform;
    private String formFactor;
    private String memoryType;
    private int ramSlots;
    private String maxRam;
    private int m2Slots;
    private String pcieVersion;

    @Column(columnDefinition = "TEXT")
    private String notableFeatures;

    private double price;
    private int stocks;
    private String imageFileName;
    private java.util.Date createdAt;

    public int getMotherboardId() {
        return motherboardId;
    }

    public void setMotherboardId(int motherboardId) {
        this.motherboardId = motherboardId;
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

    public String getChipset() {
        return chipset;
    }

    public void setChipset(String chipset) {
        this.chipset = chipset;
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getCpuPlatform() {
        return cpuPlatform;
    }

    public void setCpuPlatform(String cpuPlatform) {
        this.cpuPlatform = cpuPlatform;
    }

    public String getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(String formFactor) {
        this.formFactor = formFactor;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public int getRamSlots() {
        return ramSlots;
    }

    public void setRamSlots(int ramSlots) {
        this.ramSlots = ramSlots;
    }

    public String getMaxRam() {
        return maxRam;
    }

    public void setMaxRam(String maxRam) {
        this.maxRam = maxRam;
    }

    public int getM2Slots() {
        return m2Slots;
    }

    public void setM2Slots(int m2Slots) {
        this.m2Slots = m2Slots;
    }

    public String getPcieVersion() {
        return pcieVersion;
    }

    public void setPcieVersion(String pcieVersion) {
        this.pcieVersion = pcieVersion;
    }

    public String getNotableFeatures() {
        return notableFeatures;
    }

    public void setNotableFeatures(String notableFeatures) {
        this.notableFeatures = notableFeatures;
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
