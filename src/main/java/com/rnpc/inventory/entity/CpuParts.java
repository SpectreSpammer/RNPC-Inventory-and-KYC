package com.rnpc.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "rnpc_cpu_parts")
public class CpuParts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int cpuId;

    private String brand;
    private String generation;
    private String modelName;
    private String socket;
    private int cores;
    private int thread;
    private double baseClockGhz;
    private double boostClockGhz;
    private String integratedGraphics;
    private String memorySupport;
    private int launchYear;
    private double price;
    private int stocks;
    private String imageFileName;
    private Date createdAt;

    public int getCpuId() {
        return cpuId;
    }

    public void setCpuId(int cpuId) {
        this.cpuId = cpuId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public double getBaseClockGhz() {
        return baseClockGhz;
    }

    public void setBaseClockGhz(double baseClockGhz) {
        this.baseClockGhz = baseClockGhz;
    }

    public double getBoostClockGhz() {
        return boostClockGhz;
    }

    public void setBoostClockGhz(double boostClockGhz) {
        this.boostClockGhz = boostClockGhz;
    }

    public String getIntegratedGraphics() {
        return integratedGraphics;
    }

    public void setIntegratedGraphics(String integratedGraphics) {
        this.integratedGraphics = integratedGraphics;
    }

    public String getMemorySupport() {
        return memorySupport;
    }

    public void setMemorySupport(String memorySupport) {
        this.memorySupport = memorySupport;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
