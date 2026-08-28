package com.rnpc.inventory.dto;

import java.util.LinkedHashMap;

/**
 * Flattened view of a part from any of the 8 typed PC-component tables, used
 * only to feed the Build-a-PC page (list rendering + socket/RAM-type
 * compatibility checks). Not persisted. Field names deliberately match what
 * build/buildPc.html's JS already expects (componentId, specs).
 *
 * {@code fields} additionally carries every category-specific column (in the
 * exact order/labels used on that category's own list page, per the
 * PC_Components_Reference.xlsx-derived header spec) so the Build page's part
 * picker can render full per-column detail instead of a single collapsed
 * "specs" line. A {@link LinkedHashMap} is used so Jackson serializes it as a
 * JS object with keys in insertion order, which the picker's JS relies on to
 * build its table header.
 */
public class BuildPartView {

    private int componentId;
    private String category;
    private String brand;
    private String modelName;
    private String socket;
    private String ramType;
    private String specs;
    private double price;
    private int stocks;
    private LinkedHashMap<String, String> fields = new LinkedHashMap<>();
    private String imageFileName;

    public BuildPartView() {
    }

    public BuildPartView(int componentId, String category, String brand, String modelName, String socket,
                          String ramType, String specs, double price, int stocks) {
        this.componentId = componentId;
        this.category = category;
        this.brand = brand;
        this.modelName = modelName;
        this.socket = socket;
        this.ramType = ramType;
        this.specs = specs;
        this.price = price;
        this.stocks = stocks;
    }

    public LinkedHashMap<String, String> getFields() {
        return fields;
    }

    public void setFields(LinkedHashMap<String, String> fields) {
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

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getRamType() {
        return ramType;
    }

    public void setRamType(String ramType) {
        this.ramType = ramType;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
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
}
