package com.rnpc.inventory.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The one place device type, brand and model name are joined for display. An appointment-sourced
 * repair has no brand or model, and a walk-in ticket can have no brand - nothing may print "null"
 * or leave a dangling separator.
 */
class RepairRecordTest {

    private static RepairRecord repair(String deviceType, String brand, String model) {
        RepairRecord r = new RepairRecord();
        r.setDeviceType(deviceType);
        r.setBrand(brand);
        r.setModelName(model);
        return r;
    }

    @Test
    void aFullyKnownDeviceJoinsAllThreeParts() {
        RepairRecord r = repair("Desktop", "AMD", "Ryzen 5 3200G");

        assertEquals("AMD Ryzen 5 3200G", r.getBrandModel());
        assertEquals("Desktop (AMD Ryzen 5 3200G)", r.getDeviceLabel());
        assertEquals("Desktop - AMD Ryzen 5 3200G", r.deviceSummary(" - "));
        assertEquals("Desktop, AMD Ryzen 5 3200G", r.deviceSummary(", "));
        assertEquals("Desktop AMD Ryzen 5 3200G", r.deviceSummary(" "));
    }

    @Test
    void aBlankBrandSkipsTheBrandWithoutALeftoverSeparator() {
        RepairRecord r = repair("Laptop", "", "Aspire 5");

        assertEquals("Aspire 5", r.getBrandModel());
        assertEquals("Laptop (Aspire 5)", r.getDeviceLabel());
        assertEquals("Laptop - Aspire 5", r.deviceSummary(" - "));
    }

    @Test
    void anAppointmentSourcedRepairWithNoBrandOrModelShowsJustTheDeviceType() {
        RepairRecord r = repair("Cellphone", null, null);

        assertEquals("", r.getBrandModel());
        assertEquals("Cellphone", r.getDeviceLabel());
        assertEquals("Cellphone", r.deviceSummary(" - "));
        assertEquals("Cellphone", r.deviceSummary(", "));
    }

    @Test
    void whitespaceOnlyPartsCountAsBlank() {
        RepairRecord r = repair("Laptop", "   ", " ");

        assertEquals("Laptop", r.getDeviceLabel());
        assertEquals("Laptop", r.deviceSummary(", "));
    }

    @Test
    void nothingKnownGivesAnEmptyStringNotNull() {
        RepairRecord r = repair(null, null, null);

        assertEquals("", r.getBrandModel());
        assertEquals("", r.getDeviceLabel());
        assertEquals("", r.deviceSummary(" - "));
    }

    @Test
    void noHelperEverPrintsTheWordNull() {
        for (RepairRecord r : new RepairRecord[] {
                repair(null, null, null), repair("Laptop", null, null), repair(null, "Acer", null),
                repair(null, null, "Aspire 5"), repair("Laptop", null, "Aspire 5")}) {
            assertFalse(r.getBrandModel().contains("null"));
            assertFalse(r.getDeviceLabel().contains("null"));
            assertFalse(r.deviceSummary(", ").contains("null"));
        }
    }
}
