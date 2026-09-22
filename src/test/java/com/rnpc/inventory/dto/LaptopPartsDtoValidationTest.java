package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;
import jakarta.validation.Validation;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The laptop DTO's validation groups, run exactly the way LaptopPartsController runs them:
 * SmartValidator.validate(dto, result, Default.class, group). Plain Hibernate Validator behind
 * Spring's adapter - no Spring context and no database.
 */
class LaptopPartsDtoValidationTest {

    private final SpringValidatorAdapter validator =
            new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());

    private Set<String> errorFields(LaptopPartsDto dto, Class<?> group) {
        BindingResult result = new BeanPropertyBindingResult(dto, "laptopPartsDto");
        validator.validate(dto, result, Default.class, group);
        return result.getFieldErrors().stream().map(FieldError::getField).collect(Collectors.toSet());
    }

    private LaptopPartsDto common() {
        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand("HP");
        dto.setPartName("Pavilion 15 LCD");
        dto.setCompatibleModels("Pavilion 15-eg, 15-eh");
        dto.setPartCondition(PartCondition.NEW);
        dto.setStocks(0);
        dto.setPrice(3500);
        return dto;
    }

    private LaptopPartsDto validLcd() {
        LaptopPartsDto dto = common();
        dto.setLcdSizeInches(15.6);
        dto.setLcdResolution("HD+ 1600x900");
        dto.setLcdPanelType("IPS");
        dto.setLcdConnector("30-pin eDP");
        dto.setLcdSurface("");      // optional select left on "Not specified"
        dto.setLcdMounting("");
        return dto;
    }

    @Test
    void validLcdPassesWithNoNotes() {
        // Notes (description) are optional on every form.
        assertEquals(Set.of(), errorFields(validLcd(), PartType.LCD.getGroup()));
    }

    @Test
    void lcdRequiresItsSpecsCompatibleModelsAndCondition() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(" ".trim());
        dto.setPartCondition(null);
        assertEquals(Set.of("lcdSizeInches", "lcdResolution", "lcdPanelType", "lcdConnector",
                        "compatibleModels", "partCondition"),
                errorFields(dto, PartType.LCD.getGroup()));
    }

    @Test
    void lcdRejectsValuesOutsideTheAllowLists() {
        LaptopPartsDto dto = validLcd();
        dto.setLcdConnector("50-pin eDP");
        dto.setLcdSurface("Satin");
        assertEquals(Set.of("lcdConnector", "lcdSurface"), errorFields(dto, PartType.LCD.getGroup()));
    }

    @Test
    void batteryRequiresCapacityAndVoltageButNotLcdFields() {
        LaptopPartsDto dto = common();
        dto.setBatteryChemistry("");
        assertEquals(Set.of("batteryCapacityWh", "batteryVoltage"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityWh(42.0);
        dto.setBatteryVoltage(11.4);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void batteryRangesCatchImplausibleValues() {
        LaptopPartsDto dto = common();
        dto.setBatteryCapacityWh(9.9);
        dto.setBatteryVoltage(20.1);
        assertEquals(Set.of("batteryCapacityWh", "batteryVoltage"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityWh(4000.0);   // a mAh figure typed into the Wh field
        dto.setBatteryVoltage(2.9);
        assertEquals(Set.of("batteryCapacityWh", "batteryVoltage"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityWh(10.0);     // both ends inclusive
        dto.setBatteryVoltage(20.0);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
        dto.setBatteryCapacityWh(150.0);
        dto.setBatteryVoltage(3.0);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void ramRequiresTypeAndCapacityButNotCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        dto.setRamSpeedMts(null);
        assertEquals(Set.of("ramType", "ramCapacityGb"), errorFields(dto, PartType.RAM.getGroup()));

        dto.setRamType("DDR4");
        dto.setRamCapacityGb(8);
        assertEquals(Set.of(), errorFields(dto, PartType.RAM.getGroup()));

        dto.setRamType("DDR2");
        assertEquals(Set.of("ramType"), errorFields(dto, PartType.RAM.getGroup()));
    }

    @Test
    void storageRequiresTypeCapacityAndFormFactor() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels("");
        dto.setStorageInterface("");
        assertEquals(Set.of("storageType", "storageCapacityGb", "storageFormFactor"),
                errorFields(dto, PartType.STORAGE.getGroup()));

        dto.setStorageType("SATA SSD");
        dto.setStorageCapacityGb(1000);
        dto.setStorageFormFactor("2.5\"");
        assertEquals(Set.of(), errorFields(dto, PartType.STORAGE.getGroup()));

        dto.setStorageFormFactor("M.2 22110");
        dto.setStorageInterface("PCIe 5.0 x4");
        assertEquals(Set.of("storageFormFactor", "storageInterface"), errorFields(dto, PartType.STORAGE.getGroup()));
    }

    @Test
    void chargerRequiresWattageVoltageAndTipWithinRange() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        assertEquals(Set.of("chargerWattage", "chargerOutputVoltage", "chargerConnectorTip"),
                errorFields(dto, PartType.CHARGER.getGroup()));

        dto.setChargerWattage(65);
        dto.setChargerOutputVoltage(19.5);
        dto.setChargerConnectorTip("4.5x3.0mm");
        dto.setChargerIncludesCord(true);
        assertEquals(Set.of(), errorFields(dto, PartType.CHARGER.getGroup()));

        dto.setChargerOutputVoltage(4.9);
        dto.setChargerCurrentA(0.4);
        assertEquals(Set.of("chargerOutputVoltage", "chargerCurrentA"), errorFields(dto, PartType.CHARGER.getGroup()));

        dto.setChargerOutputVoltage(48.1);
        dto.setChargerCurrentA(10.1);
        assertEquals(Set.of("chargerOutputVoltage", "chargerCurrentA"), errorFields(dto, PartType.CHARGER.getGroup()));

        dto.setChargerOutputVoltage(5.0);
        dto.setChargerCurrentA(10.0);
        dto.setChargerConnectorTip("4.5x3.0 mm");  // not the listed spelling
        assertEquals(Set.of("chargerConnectorTip"), errorFields(dto, PartType.CHARGER.getGroup()));
    }

    @Test
    void keyboardRequiresLayoutAndCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels("");
        dto.setKbColor("");
        assertEquals(Set.of("kbLayout", "compatibleModels"), errorFields(dto, PartType.KEYBOARD.getGroup()));

        dto.setCompatibleModels("Aspire 5 A515-54, A515-55");
        dto.setKbLayout("US");
        dto.setKbBacklit(true);
        dto.setKbWithPalmrest(false);
        assertEquals(Set.of(), errorFields(dto, PartType.KEYBOARD.getGroup()));

        dto.setKbLayout("JP");
        dto.setKbColor("Red");
        assertEquals(Set.of("kbLayout", "kbColor"), errorFields(dto, PartType.KEYBOARD.getGroup()));
    }

    @Test
    void casingRequiresPanelAndCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        dto.setCasingColor("");
        assertEquals(Set.of("casingPanel", "compatibleModels"), errorFields(dto, PartType.CASING.getGroup()));

        dto.setCompatibleModels("Aspire 5 A515-54");
        dto.setCasingPanel("A cover (lid)");     // parentheses are literal in the allow-list
        dto.setCasingColor("Gold");
        assertEquals(Set.of(), errorFields(dto, PartType.CASING.getGroup()));

        dto.setCasingPanel("A cover lid");
        dto.setCasingColor("Red");
        assertEquals(Set.of("casingPanel", "casingColor"), errorFields(dto, PartType.CASING.getGroup()));
    }

    @Test
    void hingesRequireSideAndCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels("");
        assertEquals(Set.of("hingeSide", "compatibleModels"), errorFields(dto, PartType.HINGES.getGroup()));

        dto.setCompatibleModels("IdeaPad 3 15IIL05");
        dto.setHingeSide("Pair");
        assertEquals(Set.of(), errorFields(dto, PartType.HINGES.getGroup()));

        dto.setHingeSide("Both");
        assertEquals(Set.of("hingeSide"), errorFields(dto, PartType.HINGES.getGroup()));
    }

    @Test
    void dcJackRequiresTypeAndCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        dto.setDcJackTipSize("");
        assertEquals(Set.of("dcJackType", "compatibleModels"), errorFields(dto, PartType.DC_JACK.getGroup()));

        dto.setCompatibleModels("Inspiron 15 3511");
        dto.setDcJackType("Soldered barrel");
        dto.setDcJackTipSize("7.4x5.0mm");
        assertEquals(Set.of(), errorFields(dto, PartType.DC_JACK.getGroup()));

        dto.setDcJackTipSize("USB-C");           // a charger tip, not a DC jack tip size
        assertEquals(Set.of("dcJackTipSize"), errorFields(dto, PartType.DC_JACK.getGroup()));
    }

    @Test
    void wifiCardRequiresStandardAndFormFactorButNotCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        dto.setWifiBluetoothVersion("");
        assertEquals(Set.of("wifiStandard", "wifiFormFactor"), errorFields(dto, PartType.WIFI_CARD.getGroup()));

        dto.setWifiStandard("Wi-Fi 6E");
        dto.setWifiFormFactor("Mini PCIe");
        dto.setWifiBluetoothVersion("5.0");
        assertEquals(Set.of(), errorFields(dto, PartType.WIFI_CARD.getGroup()));

        dto.setWifiBluetoothVersion("5x3");      // the "." is literal in the allow-list
        dto.setWifiStandard("Wi-Fi 8");
        assertEquals(Set.of("wifiBluetoothVersion", "wifiStandard"), errorFields(dto, PartType.WIFI_CARD.getGroup()));
    }

    @Test
    void touchpadNeedsOnlyCompatibleModels() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels("");
        dto.setTouchpadColor("");
        assertEquals(Set.of("compatibleModels"), errorFields(dto, PartType.TOUCHPAD.getGroup()));

        dto.setCompatibleModels("Pavilion 15-eg");
        dto.setTouchpadConnector("6-pin FFC");
        dto.setTouchpadWithBracket(true);
        assertEquals(Set.of(), errorFields(dto, PartType.TOUCHPAD.getGroup()));

        dto.setTouchpadColor("Gold");            // Casing offers Gold, Touchpad does not
        dto.setTouchpadConnector("x".repeat(101));
        assertEquals(Set.of("touchpadColor", "touchpadConnector"), errorFields(dto, PartType.TOUCHPAD.getGroup()));
    }

    @Test
    void otherNeedsOnlyTheCommonFields() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        assertEquals(Set.of(), errorFields(dto, PartType.OTHER.getGroup()));

        dto.setPartCondition(null);              // still a typed part: condition is required
        assertEquals(Set.of("partCondition"), errorFields(dto, PartType.OTHER.getGroup()));
    }

    @Test
    void compatibleModelsStaysOptionalForGenericTypes() {
        LaptopPartsDto dto = common();
        dto.setCompatibleModels(null);
        dto.setRamType("DDR4");
        dto.setRamCapacityGb(8);
        assertEquals(Set.of(), errorFields(dto, PartType.RAM.getGroup()));
    }

    @Test
    void newTypeRulesDoNotLeakIntoOtherTypes() {
        // A valid LCD with nothing set for RAM, Storage or Charger stays valid.
        assertEquals(Set.of(), errorFields(validLcd(), PartType.LCD.getGroup()));
        // And the ranges only apply to their own type.
        LaptopPartsDto dto = validLcd();
        dto.setBatteryCapacityWh(9000.0);
        dto.setChargerOutputVoltage(1.0);
        dto.setKbLayout("JP");
        assertEquals(Set.of(), errorFields(dto, PartType.LCD.getGroup()));
    }

    @Test
    void newBrandAllowListRejectsTheOldHpSpelling() {
        LaptopPartsDto dto = validLcd();
        dto.setBrand("Hp");
        assertEquals(Set.of("brand"), errorFields(dto, PartType.LCD.getGroup()));
    }

    @Test
    void notesAreOptionalWithNoMinimumLengthButCapped() {
        // The pre-redesign rule "at least 10 characters" is gone; only the 2000-character cap stays.
        LaptopPartsDto dto = validLcd();
        dto.setDescription("short");
        assertEquals(Set.of(), errorFields(dto, PartType.LCD.getGroup()));

        dto.setDescription("x".repeat(2001));
        assertEquals(Set.of("description"), errorFields(dto, PartType.LCD.getGroup()));
    }

    @Test
    void partNameIsFreeTextForEveryType() {
        // The old allow-list (LCD|Keyboard|Trackpad|Ram|SSD|M.2) no longer applies.
        LaptopPartsDto dto = validLcd();
        dto.setPartName("Inspiron 15 3000 LCD, 30-pin");
        assertEquals(Set.of(), errorFields(dto, PartType.LCD.getGroup()));

        dto.setPartName("x".repeat(256));
        assertEquals(Set.of("partName"), errorFields(dto, PartType.LCD.getGroup()));
    }
}
