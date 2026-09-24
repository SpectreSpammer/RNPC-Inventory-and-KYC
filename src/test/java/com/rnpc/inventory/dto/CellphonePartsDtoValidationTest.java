package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.entity.CellphoneParts.PartType;
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

/**
 * The cellphone DTO's validation groups, run exactly the way CellphonePartsController runs them:
 * SmartValidator.validate(dto, result, Default.class, group). Plain Hibernate Validator behind
 * Spring's adapter - no Spring context and no database. Mirrors LaptopPartsDtoValidationTest's
 * structure for cellphone batch 3's two types, Screen and Battery.
 */
class CellphonePartsDtoValidationTest {

    private final SpringValidatorAdapter validator =
            new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());

    private Set<String> errorFields(CellphonePartsDto dto, Class<?> group) {
        BindingResult result = new BeanPropertyBindingResult(dto, "cellphonePartsDto");
        validator.validate(dto, result, Default.class, group);
        return result.getFieldErrors().stream().map(FieldError::getField).collect(Collectors.toSet());
    }

    private CellphonePartsDto common() {
        CellphonePartsDto dto = new CellphonePartsDto();
        dto.setBrand("Samsung");
        dto.setPartName("Galaxy A12 screen");
        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setPartCondition(PartCondition.NEW);
        dto.setStocks(0);
        dto.setPrice(500);
        return dto;
    }

    private CellphonePartsDto validScreen() {
        CellphonePartsDto dto = common();
        dto.setScreenPanelType("AMOLED");
        dto.setScreenGrade("Service pack");
        dto.setScreenSizeInches(6.1);
        return dto;
    }

    private CellphonePartsDto validBattery() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 battery");
        dto.setBatteryCapacityMah(5000);
        dto.setBatteryVoltage(3.87);
        dto.setBatteryChemistry("Li-ion");
        return dto;
    }

    @Test
    void validScreenPassesWithNoNotes() {
        // Notes (description) are optional on every form.
        assertEquals(Set.of(), errorFields(validScreen(), PartType.SCREEN.getGroup()));
    }

    @Test
    void screenRequiresItsSpecsCompatibleModelsAndCondition() {
        CellphonePartsDto dto = common();
        dto.setCompatibleModels(" ".trim());
        dto.setPartCondition(null);
        assertEquals(Set.of("screenPanelType", "screenGrade", "compatibleModels", "partCondition"),
                errorFields(dto, PartType.SCREEN.getGroup()));
    }

    @Test
    void screenRejectsValuesOutsideTheAllowLists() {
        CellphonePartsDto dto = validScreen();
        dto.setScreenPanelType("Retina");
        dto.setScreenGrade("Refurbished");
        assertEquals(Set.of("screenPanelType", "screenGrade"), errorFields(dto, PartType.SCREEN.getGroup()));
    }

    @Test
    void screenSizeRangeCatchesImplausibleValues() {
        CellphonePartsDto dto = validScreen();
        dto.setScreenSizeInches(2.9);
        assertEquals(Set.of("screenSizeInches"), errorFields(dto, PartType.SCREEN.getGroup()));

        dto.setScreenSizeInches(8.1);
        assertEquals(Set.of("screenSizeInches"), errorFields(dto, PartType.SCREEN.getGroup()));

        dto.setScreenSizeInches(3.0);   // both ends inclusive
        assertEquals(Set.of(), errorFields(dto, PartType.SCREEN.getGroup()));
        dto.setScreenSizeInches(8.0);
        assertEquals(Set.of(), errorFields(dto, PartType.SCREEN.getGroup()));
    }

    @Test
    void screenSizeIsOptional() {
        CellphonePartsDto dto = validScreen();
        dto.setScreenSizeInches(null);
        assertEquals(Set.of(), errorFields(dto, PartType.SCREEN.getGroup()));
    }

    @Test
    void batteryRequiresCapacityButNotScreenFields() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 battery");
        assertEquals(Set.of("batteryCapacityMah"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityMah(4000);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void batteryCapacityRangeCatchesImplausibleValues() {
        CellphonePartsDto dto = validBattery();
        dto.setBatteryCapacityMah(499);
        assertEquals(Set.of("batteryCapacityMah"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityMah(10001);
        assertEquals(Set.of("batteryCapacityMah"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryCapacityMah(500);   // both ends inclusive
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
        dto.setBatteryCapacityMah(10000);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void batteryVoltageRangeIsOptionalButBoundedWhenGiven() {
        CellphonePartsDto dto = validBattery();
        dto.setBatteryVoltage(null);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryVoltage(2.9);
        assertEquals(Set.of("batteryVoltage"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryVoltage(5.1);
        assertEquals(Set.of("batteryVoltage"), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryVoltage(3.0);      // both ends inclusive
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
        dto.setBatteryVoltage(5.0);
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void batteryChemistryIsOptionalWithAnAllowList() {
        CellphonePartsDto dto = validBattery();
        dto.setBatteryChemistry("");
        assertEquals(Set.of(), errorFields(dto, PartType.BATTERY.getGroup()));

        dto.setBatteryChemistry("NiMH");
        assertEquals(Set.of("batteryChemistry"), errorFields(dto, PartType.BATTERY.getGroup()));
    }

    @Test
    void compatibleModelsIsRequiredForBothNewTypes() {
        CellphonePartsDto screen = validScreen();
        screen.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(screen, PartType.SCREEN.getGroup()));

        CellphonePartsDto battery = validBattery();
        battery.setCompatibleModels("");
        assertEquals(Set.of("compatibleModels"), errorFields(battery, PartType.BATTERY.getGroup()));
    }

    @Test
    void legacyRulesDoNotApplyToScreenOrBattery() {
        // No category, storageSize or old partName pattern set - the Legacy-only rules must not fire.
        assertEquals(Set.of(), errorFields(validScreen(), PartType.SCREEN.getGroup()));
        assertEquals(Set.of(), errorFields(validBattery(), PartType.BATTERY.getGroup()));
    }

    @Test
    void screenAndBatteryRulesDoNotLeakIntoTheLegacyGroup() {
        // A valid pre-redesign DTO stays valid even with typed fields left unset - Screen/Battery
        // rules are scoped to their own groups.
        CellphonePartsDto dto = new CellphonePartsDto();
        dto.setBrand("Samsung");
        dto.setPartName("Super AMOLED Display");
        dto.setCategory("Display");
        dto.setStorageSize("None Applicable");
        dto.setStocks(0);
        dto.setPrice(500);
        dto.setDescription("A description long enough");
        assertEquals(Set.of(), errorFields(dto, PartType.Groups.Legacy.class));
    }

    @Test
    void newTypeRulesDoNotLeakIntoOtherTypes() {
        // A valid Screen with nothing set for Battery stays valid, and vice versa.
        CellphonePartsDto screen = validScreen();
        screen.setBatteryCapacityMah(1);
        screen.setBatteryVoltage(100.0);
        assertEquals(Set.of(), errorFields(screen, PartType.SCREEN.getGroup()));

        CellphonePartsDto battery = validBattery();
        battery.setScreenPanelType("Retina");
        battery.setScreenSizeInches(100.0);
        assertEquals(Set.of(), errorFields(battery, PartType.BATTERY.getGroup()));
    }
}
