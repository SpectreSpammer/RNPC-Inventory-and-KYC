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
    void validLcdPassesWithoutAnyLegacyField() {
        // No category, storageSize or description: the old rules must not apply here.
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
    void legacyPathKeepsTheOldRulesAndDoesNotRequireCondition() {
        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand("Dell");
        dto.setPartName("LCD");
        dto.setStocks(3);
        dto.setPrice(2450);
        dto.setDescription("short");
        Set<String> errors = errorFields(dto, PartType.Groups.Legacy.class);
        assertEquals(Set.of("category", "storageSize", "description"), errors);

        dto.setCategory("Notebook");
        dto.setStorageSize("None Applicable");
        dto.setDescription("15.6 inch FHD panel");
        assertEquals(Set.of(), errorFields(dto, PartType.Groups.Legacy.class));
    }

    @Test
    void legacyPathStillRejectsAFreeTextPartName() {
        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand("Dell");
        dto.setPartName("Inspiron 15 3000 LCD");
        dto.setCategory("Notebook");
        dto.setStorageSize("None Applicable");
        dto.setDescription("15.6 inch FHD panel");
        assertTrue(errorFields(dto, PartType.Groups.Legacy.class).contains("partName"));
    }
}
