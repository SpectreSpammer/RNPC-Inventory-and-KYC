package com.rnpc.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rnpc_cooler_parts")
public class CoolerParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int coolerId;

    private String brand;
    private String modelName;
    private String type;
    private String radiatorHeight;
    private String socketSupport;
    private String rgb;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private double price;
    private int stocks;
    private String imageFileName;
    private java.util.Date createdAt;

    public int getCoolerId() {
        return coolerId;
    }

    public void setCoolerId(int coolerId) {
        this.coolerId = coolerId;
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
