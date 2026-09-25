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
 * structure, extended batch by batch as each type gets its own form: Screen and Battery (batch 3),
 * Charging board, Back glass, Housing and Flex cable (batch 4), Camera, Fingerprint, Sensor and
 * Other (batch 5) - the last four, so every PartType now has a form.
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

    private CellphonePartsDto validChargingBoard() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 charging board");
        dto.setPortConnector("USB-C");
        dto.setPortOnFlex(true);
        dto.setPortWithMic(false);
        return dto;
    }

    private CellphonePartsDto validBackGlass() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 back glass");
        dto.setCoverMaterial("Glass");
        dto.setCoverColor("Black");
        dto.setCoverWithLens(true);
        return dto;
    }

    private CellphonePartsDto validHousing() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 housing");
        dto.setHousingColor("Black");
        dto.setHousingWithButtons(true);
        dto.setHousingWithBackGlass(false);
        return dto;
    }

    private CellphonePartsDto validFlexCable() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 flex cable");
        dto.setFlexFunction("Power/volume");
        return dto;
    }

    private CellphonePartsDto validCamera() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 rear camera");
        dto.setCameraPosition("Rear main");
        dto.setCameraMegapixels(50.0);
        dto.setCameraModule(true);
        return dto;
    }

    private CellphonePartsDto validFingerprint() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 fingerprint sensor");
        dto.setFingerprintPosition("Side / power button");
        dto.setFingerprintWithFlex(true);
        return dto;
    }

    private CellphonePartsDto validSensor() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 proximity sensor");
        dto.setSensorKind("Proximity");
        dto.setSensorUnderDisplay(false);
        return dto;
    }

    private CellphonePartsDto validOther() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 SIM tray");
        dto.setCompatibleModels(null);
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
    void chargingBoardRequiresConnectorAndCompatibleModels() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 charging board");
        dto.setCompatibleModels(null);
        assertEquals(Set.of("portConnector", "compatibleModels"), errorFields(dto, PartType.CHARGING_BOARD.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setPortConnector("Micro-USB");
        assertEquals(Set.of(), errorFields(dto, PartType.CHARGING_BOARD.getGroup()));

        dto.setPortConnector("USB-A");
        assertEquals(Set.of("portConnector"), errorFields(dto, PartType.CHARGING_BOARD.getGroup()));
    }

    @Test
    void chargingBoardCheckboxesAreOptional() {
        CellphonePartsDto dto = validChargingBoard();
        dto.setPortOnFlex(null);
        dto.setPortWithMic(null);
        assertEquals(Set.of(), errorFields(dto, PartType.CHARGING_BOARD.getGroup()));
    }

    @Test
    void backGlassRequiresMaterialAndCompatibleModelsButColorIsOptional() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 back glass");
        dto.setCompatibleModels("");
        assertEquals(Set.of("coverMaterial", "compatibleModels"), errorFields(dto, PartType.BACK_GLASS.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setCoverMaterial("Glass with frame");
        dto.setCoverColor("");
        assertEquals(Set.of(), errorFields(dto, PartType.BACK_GLASS.getGroup()));

        dto.setCoverMaterial("Aluminum");
        dto.setCoverColor("Rose Gold");
        assertEquals(Set.of("coverMaterial", "coverColor"), errorFields(dto, PartType.BACK_GLASS.getGroup()));
    }

    @Test
    void housingNeedsOnlyCompatibleModelsAndCondition() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 housing");
        dto.setCompatibleModels(null);
        dto.setHousingColor("");
        assertEquals(Set.of("compatibleModels"), errorFields(dto, PartType.HOUSING.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setHousingColor("Silver");
        dto.setHousingWithButtons(true);
        assertEquals(Set.of(), errorFields(dto, PartType.HOUSING.getGroup()));

        dto.setHousingColor("Rose Gold");   // Back glass offers this list too, but not this value
        assertEquals(Set.of("housingColor"), errorFields(dto, PartType.HOUSING.getGroup()));
    }

    @Test
    void flexCableRequiresFunctionAndCompatibleModels() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 flex cable");
        dto.setCompatibleModels(null);
        assertEquals(Set.of("flexFunction", "compatibleModels"), errorFields(dto, PartType.FLEX_CABLE.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setFlexFunction("Loudspeaker");
        assertEquals(Set.of(), errorFields(dto, PartType.FLEX_CABLE.getGroup()));

        dto.setFlexFunction("Speaker");
        assertEquals(Set.of("flexFunction"), errorFields(dto, PartType.FLEX_CABLE.getGroup()));
    }

    @Test
    void cameraRequiresPositionAndCompatibleModelsButResolutionAndModuleAreOptional() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 rear camera");
        dto.setCompatibleModels(null);
        assertEquals(Set.of("cameraPosition", "compatibleModels"), errorFields(dto, PartType.CAMERA.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setCameraPosition("Front");
        assertEquals(Set.of(), errorFields(dto, PartType.CAMERA.getGroup()));

        dto.setCameraPosition("Rear wide");
        assertEquals(Set.of("cameraPosition"), errorFields(dto, PartType.CAMERA.getGroup()));
    }

    @Test
    void cameraResolutionRangeCatchesImplausibleValues() {
        CellphonePartsDto dto = validCamera();
        dto.setCameraMegapixels(0.2);
        assertEquals(Set.of("cameraMegapixels"), errorFields(dto, PartType.CAMERA.getGroup()));

        dto.setCameraMegapixels(200.1);
        assertEquals(Set.of("cameraMegapixels"), errorFields(dto, PartType.CAMERA.getGroup()));

        dto.setCameraMegapixels(0.3);   // both ends inclusive
        assertEquals(Set.of(), errorFields(dto, PartType.CAMERA.getGroup()));
        dto.setCameraMegapixels(200.0);
        assertEquals(Set.of(), errorFields(dto, PartType.CAMERA.getGroup()));

        dto.setCameraMegapixels(null);  // optional
        assertEquals(Set.of(), errorFields(dto, PartType.CAMERA.getGroup()));
    }

    @Test
    void fingerprintRequiresPositionAndCompatibleModelsButFlexIsOptional() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 fingerprint sensor");
        dto.setCompatibleModels("");
        assertEquals(Set.of("fingerprintPosition", "compatibleModels"), errorFields(dto, PartType.FINGERPRINT.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setFingerprintPosition("Under display ultrasonic");
        assertEquals(Set.of(), errorFields(dto, PartType.FINGERPRINT.getGroup()));

        dto.setFingerprintPosition("In-screen");
        assertEquals(Set.of("fingerprintPosition"), errorFields(dto, PartType.FINGERPRINT.getGroup()));
    }

    @Test
    void sensorRequiresKindAndCompatibleModelsButUnderDisplayIsOptional() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 proximity sensor");
        dto.setCompatibleModels(null);
        assertEquals(Set.of("sensorKind", "compatibleModels"), errorFields(dto, PartType.SENSOR.getGroup()));

        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setSensorKind("Gyroscope");
        assertEquals(Set.of(), errorFields(dto, PartType.SENSOR.getGroup()));

        dto.setSensorKind("Barometer");
        assertEquals(Set.of("sensorKind"), errorFields(dto, PartType.SENSOR.getGroup()));
    }

    @Test
    void otherHasNoRulesBeyondConditionAndCompatibleModelsStaysOptional() {
        CellphonePartsDto dto = common();
        dto.setPartName("Galaxy A12 SIM tray");
        dto.setCompatibleModels(null);
        assertEquals(Set.of(), errorFields(dto, PartType.OTHER.getGroup()));

        dto.setPartCondition(null);   // still a typed part: condition is required
        assertEquals(Set.of("partCondition"), errorFields(dto, PartType.OTHER.getGroup()));
    }

    @Test
    void compatibleModelsIsRequiredForEveryTypedType() {
        CellphonePartsDto screen = validScreen();
        screen.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(screen, PartType.SCREEN.getGroup()));

        CellphonePartsDto battery = validBattery();
        battery.setCompatibleModels("");
        assertEquals(Set.of("compatibleModels"), errorFields(battery, PartType.BATTERY.getGroup()));

        CellphonePartsDto chargingBoard = validChargingBoard();
        chargingBoard.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(chargingBoard, PartType.CHARGING_BOARD.getGroup()));

        CellphonePartsDto backGlass = validBackGlass();
        backGlass.setCompatibleModels("");
        assertEquals(Set.of("compatibleModels"), errorFields(backGlass, PartType.BACK_GLASS.getGroup()));

        CellphonePartsDto housing = validHousing();
        housing.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(housing, PartType.HOUSING.getGroup()));

        CellphonePartsDto flexCable = validFlexCable();
        flexCable.setCompatibleModels("");
        assertEquals(Set.of("compatibleModels"), errorFields(flexCable, PartType.FLEX_CABLE.getGroup()));

        CellphonePartsDto camera = validCamera();
        camera.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(camera, PartType.CAMERA.getGroup()));

        CellphonePartsDto fingerprint = validFingerprint();
        fingerprint.setCompatibleModels("");
        assertEquals(Set.of("compatibleModels"), errorFields(fingerprint, PartType.FINGERPRINT.getGroup()));

        CellphonePartsDto sensor = validSensor();
        sensor.setCompatibleModels(null);
        assertEquals(Set.of("compatibleModels"), errorFields(sensor, PartType.SENSOR.getGroup()));
    }

    @Test
    void compatibleModelsStaysOptionalForOther() {
        // Other is the one typed group deliberately left off the compatibleModels @NotEmpty list.
        assertEquals(Set.of(), errorFields(validOther(), PartType.OTHER.getGroup()));
    }

    @Test
    void legacyRulesDoNotApplyToAnyOfTheNewTypes() {
        // No category, storageSize or old partName pattern set - the Legacy-only rules must not fire.
        assertEquals(Set.of(), errorFields(validScreen(), PartType.SCREEN.getGroup()));
        assertEquals(Set.of(), errorFields(validBattery(), PartType.BATTERY.getGroup()));
        assertEquals(Set.of(), errorFields(validChargingBoard(), PartType.CHARGING_BOARD.getGroup()));
        assertEquals(Set.of(), errorFields(validBackGlass(), PartType.BACK_GLASS.getGroup()));
        assertEquals(Set.of(), errorFields(validHousing(), PartType.HOUSING.getGroup()));
        assertEquals(Set.of(), errorFields(validFlexCable(), PartType.FLEX_CABLE.getGroup()));
        assertEquals(Set.of(), errorFields(validCamera(), PartType.CAMERA.getGroup()));
        assertEquals(Set.of(), errorFields(validFingerprint(), PartType.FINGERPRINT.getGroup()));
        assertEquals(Set.of(), errorFields(validSensor(), PartType.SENSOR.getGroup()));
        assertEquals(Set.of(), errorFields(validOther(), PartType.OTHER.getGroup()));
    }

    @Test
    void newTypeRulesDoNotLeakIntoTheLegacyGroup() {
        // A valid pre-redesign DTO stays valid even with every typed field left unset - every
        // type's rules are scoped to their own group.
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
        // A valid Screen with nothing set for any other type stays valid, and vice versa.
        CellphonePartsDto screen = validScreen();
        screen.setBatteryCapacityMah(1);
        screen.setBatteryVoltage(100.0);
        screen.setPortConnector("Lightning");
        screen.setCoverMaterial("Aluminum");
        screen.setFlexFunction("Speaker");
        assertEquals(Set.of(), errorFields(screen, PartType.SCREEN.getGroup()));

        CellphonePartsDto battery = validBattery();
        battery.setScreenPanelType("Retina");
        battery.setScreenSizeInches(100.0);
        assertEquals(Set.of(), errorFields(battery, PartType.BATTERY.getGroup()));

        CellphonePartsDto chargingBoard = validChargingBoard();
        chargingBoard.setCoverMaterial("Aluminum");
        chargingBoard.setHousingColor("Rose Gold");
        assertEquals(Set.of(), errorFields(chargingBoard, PartType.CHARGING_BOARD.getGroup()));

        CellphonePartsDto backGlass = validBackGlass();
        backGlass.setFlexFunction("Speaker");
        backGlass.setPortConnector("USB-A");
        assertEquals(Set.of(), errorFields(backGlass, PartType.BACK_GLASS.getGroup()));

        CellphonePartsDto housing = validHousing();
        housing.setCoverMaterial("Aluminum");
        housing.setScreenGrade("Refurbished");
        assertEquals(Set.of(), errorFields(housing, PartType.HOUSING.getGroup()));

        CellphonePartsDto flexCable = validFlexCable();
        flexCable.setHousingColor("Rose Gold");
        flexCable.setBatteryCapacityMah(1);
        assertEquals(Set.of(), errorFields(flexCable, PartType.FLEX_CABLE.getGroup()));

        CellphonePartsDto camera = validCamera();
        camera.setFingerprintPosition("In-screen");
        camera.setSensorKind("Barometer");
        assertEquals(Set.of(), errorFields(camera, PartType.CAMERA.getGroup()));

        CellphonePartsDto fingerprint = validFingerprint();
        fingerprint.setCameraPosition("Rear wide");
        fingerprint.setCameraMegapixels(1000.0);
        assertEquals(Set.of(), errorFields(fingerprint, PartType.FINGERPRINT.getGroup()));

        CellphonePartsDto sensor = validSensor();
        sensor.setFlexFunction("Speaker");
        sensor.setFingerprintPosition("In-screen");
        assertEquals(Set.of(), errorFields(sensor, PartType.SENSOR.getGroup()));

        CellphonePartsDto other = validOther();
        other.setCameraPosition("Rear wide");
        other.setSensorKind("Barometer");
        assertEquals(Set.of(), errorFields(other, PartType.OTHER.getGroup()));
    }
}
