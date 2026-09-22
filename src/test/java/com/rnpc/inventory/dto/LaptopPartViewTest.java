package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Key-spec formatting and view-row building in LaptopPartView. Pure objects - no Spring context,
 * no database.
 */
class LaptopPartViewTest {

    private static LaptopParts part(PartType type) {
        LaptopParts p = new LaptopParts();
        p.setPartType(type);
        return p;
    }

    @Test
    void keySpecForEveryType() {
        LaptopParts lcd = part(PartType.LCD);
        lcd.setLcdSizeInches(15.6);
        lcd.setLcdResolution("FHD 1920x1080");
        lcd.setLcdPanelType("IPS");
        lcd.setLcdConnector("30-pin eDP");
        assertEquals("15.6\" FHD IPS, 30-pin eDP", LaptopPartView.keySpec(lcd));

        LaptopParts battery = part(PartType.BATTERY);
        battery.setBatteryCapacityWh(42.0);
        battery.setBatteryVoltage(11.4);
        assertEquals("42 Wh, 11.4 V", LaptopPartView.keySpec(battery));

        LaptopParts ram = part(PartType.RAM);
        ram.setRamType("DDR4");
        ram.setRamCapacityGb(8);
        ram.setRamSpeedMts(3200);
        assertEquals("DDR4, 8GB, 3200 MT/s", LaptopPartView.keySpec(ram));

        LaptopParts storage = part(PartType.STORAGE);
        storage.setStorageCapacityGb(512);
        storage.setStorageType("NVMe");
        storage.setStorageFormFactor("M.2 2280");
        assertEquals("512GB NVMe, M.2 2280", LaptopPartView.keySpec(storage));

        LaptopParts charger = part(PartType.CHARGER);
        charger.setChargerWattage(65);
        charger.setChargerConnectorTip("USB-C");
        assertEquals("65 W, USB-C", LaptopPartView.keySpec(charger));

        LaptopParts keyboard = part(PartType.KEYBOARD);
        keyboard.setKbLayout("US");
        keyboard.setKbBacklit(true);
        assertEquals("US, backlit", LaptopPartView.keySpec(keyboard));

        LaptopParts fan = part(PartType.FAN);
        fan.setFanAssembly("CPU fan");
        fan.setFanConnectorPins(4);
        assertEquals("CPU fan, 4-pin", LaptopPartView.keySpec(fan));

        LaptopParts board = part(PartType.MOTHERBOARD);
        board.setMbOnboardCpu("i5-1135G7");
        board.setMbOnboardRam("8GB");
        assertEquals("i5-1135G7, 8GB onboard", LaptopPartView.keySpec(board));

        LaptopParts casing = part(PartType.CASING);
        casing.setCasingPanel("A");
        casing.setCasingColor("silver");
        assertEquals("A cover, silver", LaptopPartView.keySpec(casing));

        LaptopParts hinges = part(PartType.HINGES);
        hinges.setHingeSide("Pair");
        assertEquals("Pair", LaptopPartView.keySpec(hinges));

        LaptopParts jack = part(PartType.DC_JACK);
        jack.setDcJackType("Barrel with cable");
        jack.setDcJackTipSize("7.4x5.0");
        assertEquals("Barrel with cable, 7.4x5.0", LaptopPartView.keySpec(jack));

        LaptopParts wifi = part(PartType.WIFI_CARD);
        wifi.setWifiStandard("Wi-Fi 6");
        wifi.setWifiFormFactor("M.2 2230");
        assertEquals("Wi-Fi 6, M.2 2230", LaptopPartView.keySpec(wifi));

        LaptopParts touchpad = part(PartType.TOUCHPAD);
        touchpad.setTouchpadWithBracket(true);
        assertEquals("With bracket", LaptopPartView.keySpec(touchpad));
    }

    @Test
    void blankPartsAreSkipped() {
        LaptopParts lcd = part(PartType.LCD);
        lcd.setLcdSizeInches(14.0);
        lcd.setLcdPanelType("  ");
        lcd.setLcdConnector("40-pin LVDS");
        assertEquals("14\", 40-pin LVDS", LaptopPartView.keySpec(lcd));

        LaptopParts battery = part(PartType.BATTERY);
        battery.setBatteryVoltage(7.6);
        assertEquals("7.6 V", LaptopPartView.keySpec(battery));

        LaptopParts keyboard = part(PartType.KEYBOARD);
        keyboard.setKbBacklit(false);
        assertEquals("", LaptopPartView.keySpec(keyboard));
    }

    @Test
    void nothingSetGivesAnEmptyKeySpec() {
        for (PartType type : PartType.values()) {
            assertEquals("", LaptopPartView.keySpec(part(type)), type.name());
        }
        assertEquals("", LaptopPartView.keySpec(new LaptopParts()));
    }

    @Test
    void specRowsCarryUnitsYesNoAndNotSet() {
        LaptopParts lcd = part(PartType.LCD);
        lcd.setLcdSizeInches(15.6);
        lcd.setLcdRefreshRateHz(144);
        lcd.setLcdTouch(false);
        LaptopPartView v = LaptopPartView.from(lcd);

        List<String> rows = v.getSpecs().stream().map(s -> s.getLabel() + "=" + s.getValue()).collect(Collectors.toList());
        assertEquals(List.of("Size=15.6 in", "Resolution=Not set", "Panel type=Not set", "Connector=Not set",
                "Refresh rate=144 Hz", "Surface=Not set", "Touch=No", "Mounting=Not set"), rows);
        assertTrue(v.getSpecs().get(1).isBlank());
        assertFalse(v.getSpecs().get(0).isBlank());
        assertEquals("LCD / Screen specs", v.getSpecsTitle());
        assertEquals("LCD", v.getTypeCode());
    }

    @Test
    void keyTilesArePaddedToThreeForShortTypes() {
        LaptopParts hinges = part(PartType.HINGES);
        hinges.setHingeSide("Left");
        hinges.setPartCondition(PartCondition.OEM_PULL);
        hinges.setWarrantyDays(30);
        List<String> tiles = LaptopPartView.from(hinges).getKeyTiles().stream()
                .map(s -> s.getLabel() + "=" + s.getValue()).collect(Collectors.toList());
        assertEquals(List.of("Side=Left", "Condition=OEM pull", "Warranty=30 days"), tiles);

        LaptopParts battery = part(PartType.BATTERY);
        battery.setBatteryCapacityWh(56.0);
        tiles = LaptopPartView.from(battery).getKeyTiles().stream().map(s -> s.getLabel()).collect(Collectors.toList());
        assertEquals(List.of("Capacity", "Voltage", "Cells"), tiles);

        tiles = LaptopPartView.from(part(PartType.LCD)).getKeyTiles().stream().map(s -> s.getLabel()).collect(Collectors.toList());
        assertEquals(List.of("Size", "Resolution", "Connector"), tiles);
    }

    @Test
    void unassignedRowShowsItsLegacyFieldsAndNoTiles() {
        LaptopParts legacy = new LaptopParts();
        legacy.setCategory("Notebook");
        LaptopPartView v = LaptopPartView.from(legacy);

        assertEquals(LaptopPartView.UNASSIGNED, v.getTypeKey());
        assertEquals("Unassigned", v.getTypeLabel());
        assertEquals("LAP", v.getTypeCode());
        assertEquals(PartType.values().length, v.getTypeOrder());
        assertEquals("", v.getKeySpec());
        assertTrue(v.getKeyTiles().isEmpty());
        assertEquals("Legacy details", v.getSpecsTitle());
        assertEquals("Notebook", v.getSpecs().get(0).getValue());
        assertTrue(v.getSpecs().get(1).isBlank());
    }
}
