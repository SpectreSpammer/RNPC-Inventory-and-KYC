package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class CpuPartsDto {

    @NotEmpty(message = "The brand is required!")
    private String brand;

    private String generation;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String socket;

    @Min(value = 0, message = "Cores cannot be negative")
    private int cores;

    @Min(value = 0, message = "Thread cannot be negative")
    private int thread;

    @Min(value = 0, message = "Base clock cannot be negative")
    private double baseClockGhz;

    @Min(value = 0, message = "Boost clock cannot be negative")
    private double boostClockGhz;

    private String integratedGraphics;

    private String memorySupport;

    @Min(value = 0, message = "Launch year cannot be negative")
    private int launchYear;

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

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
