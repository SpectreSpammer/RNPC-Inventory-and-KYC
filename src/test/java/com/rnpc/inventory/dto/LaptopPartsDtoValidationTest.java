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
