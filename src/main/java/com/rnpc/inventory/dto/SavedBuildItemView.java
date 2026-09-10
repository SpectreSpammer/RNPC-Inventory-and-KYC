package com.rnpc.inventory.dto;

/**
 * A SavedBuildItem paired with whether its componentId still resolves in the live catalog
 * (see SavedBuildService.viewSavedBuild). category/brand/modelName/price are always the
 * persisted snapshot, never a live re-read - so a saved build still renders correctly even
 * when the part behind it was deleted; {@code available} only gates whether "Load into Builder"
 * can reselect this slot.
 */
public class SavedBuildItemView {

    private final String category;
    private final String brand;
    private final String modelName;
    private final double price;
    private final int componentId;
    private final boolean available;

    public SavedBuildItemView(String category, String brand, String modelName, double price,
                               int componentId, boolean available) {
        this.category = category;
        this.brand = brand;
        this.modelName = modelName;
        this.price = price;
        this.componentId = componentId;
        this.available = available;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getModelName() {
        return modelName;
    }

    public double getPrice() {
        return price;
    }

    public int getComponentId() {
        return componentId;
    }

    public boolean isAvailable() {
        return available;
    }
}
