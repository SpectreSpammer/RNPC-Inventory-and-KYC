package com.rnpc.inventory.entity;

import jakarta.persistence.*;

// Mirrors OrderItem.java:9-20 exactly (category/brand/modelName/price snapshot columns, resolved
// live at save time and never re-read afterward - see SavedBuildService), plus componentId, which
// OrderItem never needed since an order is a historical receipt that's never reopened in the
// picker. componentId lets "Load into Builder" reselect the exact live SKU when it still exists.
@Entity
@Table(
        name = "rnpc_saved_build_items",
        indexes = @Index(name = "idx_saved_build_items_saved_build_id", columnList = "saved_build_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_saved_build_items_build_category",
                columnNames = {"saved_build_id", "category"}
        )
)
public class SavedBuildItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long savedBuildItemId;

    @ManyToOne
    @JoinColumn(name = "saved_build_id", nullable = false)
    private SavedBuild savedBuild;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int componentId;

    private String brand;
    private String modelName;
    private double price;

    public Long getSavedBuildItemId() {
        return savedBuildItemId;
    }

    public void setSavedBuildItemId(Long savedBuildItemId) {
        this.savedBuildItemId = savedBuildItemId;
    }

    public SavedBuild getSavedBuild() {
        return savedBuild;
    }

    public void setSavedBuild(SavedBuild savedBuild) {
        this.savedBuild = savedBuild;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
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
}
