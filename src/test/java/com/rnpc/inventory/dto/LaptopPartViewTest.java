package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        LaptopParts casing = part(PartType.CASING);
        casing.setCasingPanel("A cover (lid)");
        casing.setCasingColor("Silver");
        assertEquals("A cover (lid), Silver", LaptopPartView.keySpec(casing));

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

    private static List<String> rows(List<LaptopPartView.Spec> specs) {
        return specs.stream().map(s -> s.getLabel() + "=" + s.getValue()).collect(Collectors.toList());
    }

    @Test
    void specRowsCarryUnitsYesNoAndNotSet() {
        LaptopParts lcd = part(PartType.LCD);
        lcd.setLcdSizeInches(15.6);
        lcd.setLcdRefreshRateHz(144);
        lcd.setLcdTouch(false);

        // Every spec row of the type, with units, before the tiles are taken out.
        List<LaptopPartView.Spec> all = LaptopPartView.specs(lcd);
        assertEquals(List.of("Size=15.6 in", "Resolution=Not set", "Panel type=Not set", "Connector=Not set",
                "Refresh rate=144 Hz", "Surface=Not set", "Touch=No", "Mounting=Not set"), rows(all));
        assertTrue(all.get(1).isBlank());
        assertFalse(all.get(0).isBlank());

        LaptopPartView v = LaptopPartView.from(lcd);
        assertEquals("LCD / Screen specs", v.getSpecsTitle());
        assertEquals("LCD", v.getTypeCode());
    }

    @Test
    void specsListDoesNotRepeatTheKeySpecTiles() {
        LaptopParts lcd = part(PartType.LCD);
        lcd.setLcdSizeInches(15.6);
        lcd.setLcdRefreshRateHz(144);
        lcd.setLcdTouch(false);
        LaptopPartView v = LaptopPartView.from(lcd);

        assertEquals(List.of("Size=15.6 in", "Resolution=Not set", "Connector=Not set"), rows(v.getKeyTiles()));
        assertEquals(List.of("Panel type=Not set", "Refresh rate=144 Hz", "Surface=Not set", "Touch=No",
                "Mounting=Not set"), rows(v.getSpecs()));

        // A type whose only spec is a tile ends up with an empty list - the modal hides the section.
        LaptopParts hinges = part(PartType.HINGES);
        hinges.setHingeSide("Pair");
        assertTrue(LaptopPartView.from(hinges).getSpecs().isEmpty());
    }

    @Test
    void ramStorageAndChargerRowsAndKeySpecs() {
        LaptopParts ram = part(PartType.RAM);
        ram.setRamType("DDR5");
        ram.setRamCapacityGb(16);
        ram.setRamSpeedMts(5600);
        assertEquals("DDR5, 16GB, 5600 MT/s", LaptopPartView.keySpec(ram));
        assertEquals(List.of("Type=DDR5", "Capacity=16 GB", "Speed=5600 MT/s"), rows(LaptopPartView.specs(ram)));
        assertTrue(LaptopPartView.from(ram).getSpecs().isEmpty());

        LaptopParts storage = part(PartType.STORAGE);
        storage.setStorageType("SATA SSD");
        storage.setStorageCapacityGb(1000);
        storage.setStorageFormFactor("2.5\"");
        storage.setStorageInterface("SATA III");
        assertEquals("1TB SATA SSD, 2.5\"", LaptopPartView.keySpec(storage));
        assertEquals(List.of("Type=SATA SSD", "Capacity=1 TB", "Form factor=2.5\"", "Interface=SATA III"),
                rows(LaptopPartView.specs(storage)));
        assertEquals(List.of("Interface=SATA III"), rows(LaptopPartView.from(storage).getSpecs()));
        storage.setStorageCapacityGb(512);
        assertEquals("512GB SATA SSD, 2.5\"", LaptopPartView.keySpec(storage));

        LaptopParts charger = part(PartType.CHARGER);
        charger.setChargerWattage(65);
        charger.setChargerOutputVoltage(19.5);
        charger.setChargerCurrentA(3.34);
        charger.setChargerConnectorTip("4.5x3.0mm");
        charger.setChargerIncludesCord(true);
        assertEquals("65 W, 4.5x3.0mm", LaptopPartView.keySpec(charger));
        assertEquals(List.of("Wattage=65 W", "Output voltage=19.5 V", "Current=3.34 A", "Connector tip=4.5x3.0mm",
                "Includes cord=Yes"), rows(LaptopPartView.specs(charger)));
        LaptopPartView v = LaptopPartView.from(charger);
        assertEquals(List.of("Wattage=65 W", "Output voltage=19.5 V", "Connector tip=4.5x3.0mm"), rows(v.getKeyTiles()));
        assertEquals(List.of("Current=3.34 A", "Includes cord=Yes"), rows(v.getSpecs()));
    }

    @Test
    void keyboardTilesAndNoRepeat() {
        LaptopParts keyboard = part(PartType.KEYBOARD);
        keyboard.setKbLayout("UK");
        keyboard.setKbBacklit(true);
        keyboard.setKbColor("Silver");
        keyboard.setKbWithPalmrest(true);
        keyboard.setKbWithFrame(false);
        assertEquals("UK, backlit", LaptopPartView.keySpec(keyboard));
        LaptopPartView v = LaptopPartView.from(keyboard);
        assertEquals(List.of("Layout=UK", "Backlit=Yes", "With palmrest=Yes"), rows(v.getKeyTiles()));
        assertEquals(List.of("Color=Silver", "With frame=No"), rows(v.getSpecs()));
    }

    @Test
    void shortTypesPadTheirTilesAndDoNotRepeatThem() {
        LaptopParts casing = part(PartType.CASING);
        casing.setCasingPanel("D bottom");
        casing.setCasingColor("Black");
        casing.setPartCondition(PartCondition.OEM_PULL);
        LaptopPartView v = LaptopPartView.from(casing);
        assertEquals(List.of("Panel=D bottom", "Color=Black", "Condition=OEM pull"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());

        LaptopParts jack = part(PartType.DC_JACK);
        jack.setDcJackType("USB-C board");
        jack.setWarrantyDays(7);
        v = LaptopPartView.from(jack);
        assertEquals("USB-C board", LaptopPartView.keySpec(jack));
        assertEquals(List.of("Jack type=USB-C board", "Tip size=Not set", "Condition=Not set"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
    }

    @Test
    void wifiAndTouchpadRowsAndTiles() {
        LaptopParts wifi = part(PartType.WIFI_CARD);
        wifi.setWifiStandard("Wi-Fi 6E");
        wifi.setWifiFormFactor("M.2 2230");
        wifi.setWifiBluetoothVersion("5.3");
        assertEquals("Wi-Fi 6E, M.2 2230", LaptopPartView.keySpec(wifi));
        LaptopPartView v = LaptopPartView.from(wifi);
        assertEquals(List.of("Standard=Wi-Fi 6E", "Form factor=M.2 2230", "Bluetooth version=5.3"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
        assertEquals("Wi-Fi Card specs", v.getSpecsTitle());

        LaptopParts touchpad = part(PartType.TOUCHPAD);
        touchpad.setTouchpadConnector("6-pin FFC");
        touchpad.setTouchpadWithBracket(true);
        touchpad.setTouchpadColor("Silver");
        assertEquals("With bracket, 6-pin FFC", LaptopPartView.keySpec(touchpad));
        v = LaptopPartView.from(touchpad);
        assertEquals(List.of("Connector=6-pin FFC", "With bracket=Yes", "Color=Silver"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
    }

    @Test
    void otherHasNoSpecsAndTilesFromTheCommonFields() {
        LaptopParts other = part(PartType.OTHER);
        other.setPartCondition(PartCondition.NEW);
        other.setWarrantyDays(30);
        other.setPartNumber("SPK-01");
        LaptopPartView v = LaptopPartView.from(other);
        assertEquals("", v.getKeySpec());
        assertTrue(v.getSpecs().isEmpty());
        assertEquals(List.of("Condition=New", "Warranty=30 days", "Part number=SPK-01"), rows(v.getKeyTiles()));
        assertEquals("OTH", v.getTypeCode());
    }

    @Test
    void fanAndMotherboardAreNotLaptopPartTypes() {
        // Not stocked: board work is a repair service. Neither slug resolves to a type.
        assertEquals(12, PartType.values().length);
        assertNull(PartType.fromSlug("fan"));
        assertNull(PartType.fromSlug("motherboard"));
        for (PartType t : PartType.values()) {
            assertFalse(t.name().equals("FAN") || t.name().equals("MOTHERBOARD"), t.name());
        }
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
    void aStrayNullTypeRowIsShownAsOtherWithoutThrowing() {
        LaptopParts stray = new LaptopParts();
        stray.setPartName("Unknown part");
        stray.setPartCondition(PartCondition.NEW);
        LaptopPartView v = LaptopPartView.from(stray);

        assertEquals("OTHER", v.getTypeKey());
        assertEquals("Other", v.getTypeLabel());
        assertEquals("OTH", v.getTypeCode());
        assertEquals(PartType.OTHER.ordinal(), v.getTypeOrder());
        assertEquals("", v.getKeySpec());
        assertTrue(v.getSpecs().isEmpty());
        assertEquals("Other specs", v.getSpecsTitle());
        assertEquals(List.of("Condition=New", "Warranty=Not set", "Part number=Not set"), rows(v.getKeyTiles()));
    }
}
