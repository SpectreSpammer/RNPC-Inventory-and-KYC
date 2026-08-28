package com.rnpc.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rnpc_ram_parts")
public class RamParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ramId;

    private String brand;
    private String modelName;
    private String type;
    private String kitCapacity;
    private String moduleConfig;
    private int speedMts;
    private String casLatency;
    private double voltage;
    private String rgb;
    private String typicalUse;
    private double price;
    private int stocks;
    private String imageFileName;
    private java.util.Date createdAt;

    public int getRamId() {
        return ramId;
    }

    public void setRamId(int ramId) {
        this.ramId = ramId;
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
