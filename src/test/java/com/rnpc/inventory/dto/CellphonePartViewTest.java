package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.entity.CellphoneParts.PartType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Key-spec formatting and view-row building in CellphonePartView. Pure objects - no Spring
 * context, no database. Mirrors LaptopPartViewTest's structure for CellphonePartView's own ten
 * types.
 */
class CellphonePartViewTest {

    private static CellphoneParts part(PartType type) {
        CellphoneParts p = new CellphoneParts();
        p.setPartType(type);
        return p;
    }

    @Test
    void keySpecForEveryType() {
        CellphoneParts screen = part(PartType.SCREEN);
        screen.setScreenPanelType("AMOLED");
        screen.setScreenSizeInches(6.1);
        screen.setScreenGrade("Service pack");
        assertEquals("AMOLED 6.1\", Service pack", CellphonePartView.keySpec(screen));

        CellphoneParts battery = part(PartType.BATTERY);
        battery.setBatteryCapacityMah(4000);
        battery.setBatteryVoltage(3.85);
        assertEquals("4000 mAh, 3.85 V", CellphonePartView.keySpec(battery));

        CellphoneParts board = part(PartType.CHARGING_BOARD);
        board.setPortConnector("USB-C");
        board.setPortOnFlex(true);
        assertEquals("USB-C, on flex", CellphonePartView.keySpec(board));

        CellphoneParts glass = part(PartType.BACK_GLASS);
        glass.setCoverMaterial("Glass");
        glass.setCoverColor("Black");
        assertEquals("Glass, Black", CellphonePartView.keySpec(glass));

        CellphoneParts housing = part(PartType.HOUSING);
        housing.setHousingColor("Black");
        housing.setHousingWithButtons(true);
        assertEquals("Black, with buttons", CellphonePartView.keySpec(housing));

        CellphoneParts flex = part(PartType.FLEX_CABLE);
        flex.setFlexFunction("Power/volume");
        assertEquals("Power/volume", CellphonePartView.keySpec(flex));

        CellphoneParts camera = part(PartType.CAMERA);
        camera.setCameraPosition("Rear main");
        camera.setCameraMegapixels(50.0);
        assertEquals("Rear main, 50 MP", CellphonePartView.keySpec(camera));

        CellphoneParts fingerprint = part(PartType.FINGERPRINT);
        fingerprint.setFingerprintPosition("Under display");
        assertEquals("Under display", CellphonePartView.keySpec(fingerprint));

        CellphoneParts sensor = part(PartType.SENSOR);
        sensor.setSensorKind("Proximity");
        sensor.setSensorUnderDisplay(false);
        assertEquals("Proximity", CellphonePartView.keySpec(sensor));
    }

    @Test
    void blankPartsAreSkipped() {
        CellphoneParts screen = part(PartType.SCREEN);
        screen.setScreenSizeInches(6.5);
        screen.setScreenGrade("  ");
        assertEquals("6.5\"", CellphonePartView.keySpec(screen));

        CellphoneParts battery = part(PartType.BATTERY);
        battery.setBatteryVoltage(3.8);
        assertEquals("3.8 V", CellphonePartView.keySpec(battery));

        CellphoneParts board = part(PartType.CHARGING_BOARD);
        board.setPortOnFlex(false);
        assertEquals("", CellphonePartView.keySpec(board));
    }

    @Test
    void nothingSetGivesAnEmptyKeySpec() {
        for (PartType type : PartType.values()) {
            assertEquals("", CellphonePartView.keySpec(part(type)), type.name());
        }
        assertEquals("", CellphonePartView.keySpec(new CellphoneParts()));
    }

    private static List<String> rows(List<CellphonePartView.Spec> specs) {
        return specs.stream().map(s -> s.getLabel() + "=" + s.getValue()).collect(Collectors.toList());
    }

    @Test
    void specRowsCarryUnitsYesNoAndNotSet() {
        CellphoneParts screen = part(PartType.SCREEN);
        screen.setScreenPanelType("AMOLED");
        screen.setScreenSizeInches(6.1);
        screen.setScreenTouchIncluded(false);

        // Every spec row of the type, with units, before the tiles are taken out.
        List<CellphonePartView.Spec> all = CellphonePartView.specs(screen);
        assertEquals(List.of("Panel type=AMOLED", "Size=6.1 in", "Grade=Not set", "With frame=Not set",
                "Touch included=No"), rows(all));
        assertFalse(all.get(0).isBlank());
        assertTrue(all.get(2).isBlank());

        CellphonePartView v = CellphonePartView.from(screen);
        assertEquals("LCD / Screen specs", v.getSpecsTitle());
        assertEquals("SCR", v.getTypeCode());
    }

    @Test
    void specsListDoesNotRepeatTheKeySpecTiles() {
        CellphoneParts screen = part(PartType.SCREEN);
        screen.setScreenPanelType("AMOLED");
        screen.setScreenSizeInches(6.1);
        screen.setScreenGrade("Service pack");
        CellphonePartView v = CellphonePartView.from(screen);

        assertEquals(List.of("Panel type=AMOLED", "Size=6.1 in", "Grade=Service pack"), rows(v.getKeyTiles()));
        assertEquals(List.of("With frame=Not set", "Touch included=Not set"), rows(v.getSpecs()));

        // A type whose only spec is a tile ends up with an empty list - the modal hides the section.
        CellphoneParts flex = part(PartType.FLEX_CABLE);
        flex.setFlexFunction("Home button");
        assertTrue(CellphonePartView.from(flex).getSpecs().isEmpty());
    }

    @Test
    void batteryChargingBoardAndBackGlassRowsAndKeySpecs() {
        CellphoneParts battery = part(PartType.BATTERY);
        battery.setBatteryCapacityMah(5000);
        battery.setBatteryVoltage(3.87);
        battery.setBatteryChemistry("Li-ion");
        assertEquals("5000 mAh, 3.87 V", CellphonePartView.keySpec(battery));
        assertEquals(List.of("Capacity=5000 mAh", "Voltage=3.87 V", "Chemistry=Li-ion"), rows(CellphonePartView.specs(battery)));
        assertTrue(CellphonePartView.from(battery).getSpecs().isEmpty());

        CellphoneParts board = part(PartType.CHARGING_BOARD);
        board.setPortConnector("USB-C");
        board.setPortOnFlex(true);
        board.setPortWithMic(false);
        assertEquals("USB-C, on flex", CellphonePartView.keySpec(board));
        assertEquals(List.of("Connector=USB-C", "On flex=Yes", "With microphone=No"), rows(CellphonePartView.specs(board)));
        assertTrue(CellphonePartView.from(board).getSpecs().isEmpty());

        CellphoneParts glass = part(PartType.BACK_GLASS);
        glass.setCoverMaterial("Glass");
        glass.setCoverColor("Blue");
        glass.setCoverWithLens(true);
        assertEquals("Glass, Blue", CellphonePartView.keySpec(glass));
        CellphonePartView v = CellphonePartView.from(glass);
        assertEquals(List.of("Material=Glass", "Color=Blue", "With camera lens=Yes"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
    }

    @Test
    void housingAndCameraRowsAndTiles() {
        CellphoneParts housing = part(PartType.HOUSING);
        housing.setHousingColor("Black");
        housing.setHousingWithButtons(true);
        housing.setHousingWithBackGlass(false);
        assertEquals("Black, with buttons", CellphonePartView.keySpec(housing));
        CellphonePartView v = CellphonePartView.from(housing);
        assertEquals(List.of("Color=Black", "With buttons=Yes", "With back glass=No"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());

        CellphoneParts camera = part(PartType.CAMERA);
        camera.setCameraPosition("Rear ultrawide");
        camera.setCameraMegapixels(12.0);
        camera.setCameraModule(true);
        assertEquals("Rear ultrawide, 12 MP", CellphonePartView.keySpec(camera));
        v = CellphonePartView.from(camera);
        assertEquals(List.of("Position=Rear ultrawide", "Resolution=12 MP", "Full module=Yes"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
    }

    @Test
    void shortTypesPadTheirTilesAndDoNotRepeatThem() {
        // Flex cable: one spec field, padded with Condition then Warranty.
        CellphoneParts flex = part(PartType.FLEX_CABLE);
        flex.setFlexFunction("Antenna");
        flex.setPartCondition(PartCondition.OEM_PULL);
        flex.setWarrantyDays(15);
        CellphonePartView v = CellphonePartView.from(flex);
        assertEquals(List.of("Function=Antenna", "Condition=OEM pull", "Warranty=15 days"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());

        // Fingerprint: two spec fields, padded with Condition only.
        CellphoneParts fingerprint = part(PartType.FINGERPRINT);
        fingerprint.setFingerprintPosition("Side button");
        fingerprint.setFingerprintWithFlex(true);
        v = CellphonePartView.from(fingerprint);
        assertEquals(List.of("Position=Side button", "With flex cable=Yes", "Condition=Not set"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());

        // Sensor: two spec fields (both used in the key spec), padded with Condition.
        CellphoneParts sensor = part(PartType.SENSOR);
        sensor.setSensorKind("Gyroscope");
        sensor.setSensorUnderDisplay(false);
        v = CellphonePartView.from(sensor);
        assertEquals(List.of("Kind=Gyroscope", "Under display=No", "Condition=Not set"), rows(v.getKeyTiles()));
        assertTrue(v.getSpecs().isEmpty());
    }

    @Test
    void otherHasNoSpecsAndTilesFromTheCommonFields() {
        CellphoneParts other = part(PartType.OTHER);
        other.setPartCondition(PartCondition.NEW);
        other.setWarrantyDays(7);
        other.setPartNumber("SCR-KIT-01");
        CellphonePartView v = CellphonePartView.from(other);
        assertEquals("", v.getKeySpec());
        assertTrue(v.getSpecs().isEmpty());
        assertEquals(List.of("Condition=New", "Warranty=7 days", "Part number=SCR-KIT-01"), rows(v.getKeyTiles()));
        assertEquals("OTH", v.getTypeCode());
    }

    @Test
    void tenPartTypesInOrder() {
        assertEquals(10, PartType.values().length);
        assertEquals(PartType.OTHER, PartType.values()[9]);
        assertEquals("SCREEN", PartType.values()[0].name());
    }

    @Test
    void aStrayNullTypeRowIsShownAsOtherWithoutThrowing() {
        // part_type is NOT NULL once the hand-run SQL has been applied, but a stray null must not throw.
        CellphoneParts stray = new CellphoneParts();
        stray.setPartName("Unknown part");
        stray.setPartCondition(PartCondition.NEW);
        CellphonePartView v = CellphonePartView.from(stray);

        assertEquals("other", v.getTypeKey());
        assertEquals("Other", v.getTypeLabel());
        assertEquals("OTH", v.getTypeCode());
        assertEquals(PartType.OTHER.ordinal(), v.getTypeOrder());
        assertEquals("", v.getKeySpec());
        assertTrue(v.getSpecs().isEmpty());
        assertEquals("Other specs", v.getSpecsTitle());
        assertEquals(List.of("Condition=New", "Warranty=Not set", "Part number=Not set"), rows(v.getKeyTiles()));
    }
}
