package com.rnpc.inventory.dto;

import java.util.LinkedHashMap;

public class PartsListView {

    private int componentId;
    private String category;
    private String routePrefix;
    private String brand;
    private String modelName;
    private double price;
    private int stocks;
    private String imageFileName;
    private LinkedHashMap<String, String> fields = new LinkedHashMap<>();

    public PartsListView() {
    }

    public PartsListView(int componentId, String category, String routePrefix, String brand, String modelName,
                          double price, int stocks, String imageFileName, LinkedHashMap<String, String> fields) {
        this.componentId = componentId;
        this.category = category;
        this.routePrefix = routePrefix;
        this.brand = brand;
        this.modelName = modelName;
        this.price = price;
        this.stocks = stocks;
        this.imageFileName = imageFileName;
        this.fields = fields;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRoutePrefix() {
        return routePrefix;
    }

    public void setRoutePrefix(String routePrefix) {
        this.routePrefix = routePrefix;
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

    public LinkedHashMap<String, String> getFields() {
        return fields;
    }

    public void setFields(LinkedHashMap<String, String> fields) {
        this.fields = fields;
    }
}
