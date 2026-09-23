package com.rnpc.inventory.entity;

/**
 * What condition a part is in, shared by the laptop and cellphone catalogs.
 *
 * Promoted out of LaptopParts in the cellphone redesign (batch 1) so both entities use one enum.
 * The constant names are unchanged - NEW, OEM_PULL, REFURBISHED - and both entities still map it
 * with @Enumerated(STRING) onto a part_condition VARCHAR(32) column, so every stored value keeps
 * loading exactly as before. The column is named part_condition because CONDITION is a reserved
 * word in MySQL/MariaDB.
 */
public enum PartCondition {
    NEW("New"),
    OEM_PULL("OEM pull"),
    REFURBISHED("Refurbished");

    private final String label;

    PartCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
