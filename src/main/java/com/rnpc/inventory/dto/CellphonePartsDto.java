package com.rnpc.inventory.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.entity.CellphoneParts.PartType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * One DTO for both cellphone models, sorted into Bean Validation groups - same shape as
 * LaptopPartsDto:
 *
 *   Default             - rules every form shares (brand, partName, stocks, price, field lengths).
 *   PartType.Groups.X   - one per type, e.g. Screen; each extends Typed, which holds the rules
 *                         every type's form shares (partCondition). Run by the controller as
 *                         validate(dto, result, Default.class, type.getGroup()).
 *   Legacy              - the pre-redesign rules (category, storageSize, the old partName
 *                         allow-list, description's 10-character minimum). Only the old
 *                         /cellphone/create path runs it.
 *
 * There is deliberately no partType field: the type comes from the route, never the form.
 * "Notes" is bound as description here - CellphoneParts.notes maps onto the description column.
 */
public class CellphonePartsDto {

    /** Single source for the brand allow-list and the brand dropdown. */
    public static final String BRAND_PATTERN = "Samsung|Apple|Xiaomi|Oppo|Vivo|OnePlus|Realme";
    public static final List<String> BRAND_OPTIONS = Arrays.asList(BRAND_PATTERN.split("\\|"));

    private Long cellphonePartId;

    @NotEmpty(message = "The brand is required!")
    @Pattern(regexp = BRAND_PATTERN, message = "Invalid brand selected")
    private String brand;

    @NotEmpty(message = "The part name is required!")
    @Size(max = 255, message = "The part name cannot exceed 255 characters")
    @Pattern(regexp = "Super AMOLED Display|Li-Ion Battery|USB-C Charging Port|Triple Camera Module|Motherboard Flex Cable|Fingerprint Scanner|Back Glass Panel",
            message = "Invalid part name selected", groups = PartType.Groups.Legacy.class)
    private String partName;

    @NotEmpty(message = "The category is required!", groups = PartType.Groups.Legacy.class)
    @Pattern(regexp = "Display|Battery|Camera|Charging Port|Motherboard|Sensor|Back Cover",
            message = "Invalid category selected", groups = PartType.Groups.Legacy.class)
    private String category;

    @NotEmpty(message = "The storage size is required!", groups = PartType.Groups.Legacy.class)
    @Pattern(regexp = "None Applicable|32GB|64GB|128GB|256GB|512GB|1TB|2TB",
            message = "Invalid storage size selected", groups = PartType.Groups.Legacy.class)
    private String storageSize;

    @Min(value = 0, message = "The stocks cannot be negative!")
    private int stocks;

    @Min(0)
    private double price;

    /** The Notes field on the type-based forms (CellphoneParts.notes, stored in description). */
    @Size(min = 10, message = "The description should be at least 10 characters", groups = PartType.Groups.Legacy.class)
    @Size(max = 2000, message = "The description cannot exceed 2000 characters")
    private String description;

    private Date createdAt;

    private String imageFileName;

    private MultipartFile imageFile;

    // ---- Common fields of the type-based model ----

    @NotEmpty(message = "The compatible models are required!", groups = {PartType.Groups.Screen.class,
            PartType.Groups.Battery.class, PartType.Groups.ChargingBoard.class, PartType.Groups.BackGlass.class,
            PartType.Groups.Housing.class, PartType.Groups.FlexCable.class, PartType.Groups.Camera.class,
            PartType.Groups.Fingerprint.class, PartType.Groups.Sensor.class})
    @Size(max = 500, message = "The compatible models cannot exceed 500 characters")
    private String compatibleModels;

    @Size(max = 64, message = "The part number cannot exceed 64 characters")
    private String partNumber;

    @NotNull(message = "The condition is required!", groups = PartType.Groups.Typed.class)
    private PartCondition partCondition;

    @Min(value = 0, message = "The warranty days cannot be negative!")
    private Integer warrantyDays;

    // ---- SCREEN ----
    @NotEmpty(message = "The panel type is required!", groups = PartType.Groups.Screen.class)
    @Pattern(regexp = "OLED|AMOLED|Super AMOLED|LCD IPS|TFT", message = "Invalid panel type selected", groups = PartType.Groups.Screen.class)
    private String screenPanelType;
    @NotEmpty(message = "The grade is required!", groups = PartType.Groups.Screen.class)
    @Pattern(regexp = "Original|Service pack|Aftermarket incell|Aftermarket OLED", message = "Invalid grade selected", groups = PartType.Groups.Screen.class)
    private String screenGrade;
    // Sanity range, not a specification: wide enough for any phone screen, narrow enough to catch
    // a slipped decimal or a laptop-sized figure typed into the wrong form.
    @DecimalMin(value = "3", message = "The size must be between 3 and 8 inches", groups = PartType.Groups.Screen.class)
    @DecimalMax(value = "8", message = "The size must be between 3 and 8 inches", groups = PartType.Groups.Screen.class)
    private Double screenSizeInches;
    private Boolean screenWithFrame;
    private Boolean screenTouchIncluded;

    // ---- BATTERY ----
    @NotNull(message = "The capacity is required!", groups = PartType.Groups.Battery.class)
    @Min(value = 500, message = "The capacity must be between 500 and 10000 mAh", groups = PartType.Groups.Battery.class)
    @Max(value = 10000, message = "The capacity must be between 500 and 10000 mAh", groups = PartType.Groups.Battery.class)
    private Integer batteryCapacityMah;
    @DecimalMin(value = "3", message = "The voltage must be between 3 and 5 V", groups = PartType.Groups.Battery.class)
    @DecimalMax(value = "5", message = "The voltage must be between 3 and 5 V", groups = PartType.Groups.Battery.class)
    private Double batteryVoltage;
    @Pattern(regexp = "Li-ion|Li-polymer|", message = "Invalid chemistry selected", groups = PartType.Groups.Battery.class)
    private String batteryChemistry;

    // ---- CHARGING BOARD ----
    @NotEmpty(message = "The connector is required!", groups = PartType.Groups.ChargingBoard.class)
    @Pattern(regexp = "USB-C|Lightning|Micro-USB", message = "Invalid connector selected", groups = PartType.Groups.ChargingBoard.class)
    private String portConnector;
    private Boolean portOnFlex;
    private Boolean portWithMic;

    // ---- BACK GLASS ----
    @NotEmpty(message = "The material is required!", groups = PartType.Groups.BackGlass.class)
    @Pattern(regexp = "Glass|Plastic|Glass with frame", message = "Invalid material selected", groups = PartType.Groups.BackGlass.class)
    private String coverMaterial;
    @Pattern(regexp = "Black|White|Blue|Green|Gold|Silver|Purple|", message = "Invalid color selected", groups = PartType.Groups.BackGlass.class)
    private String coverColor;
    private Boolean coverWithLens;

    // ---- HOUSING / FRAME ----
    @Pattern(regexp = "Black|White|Blue|Green|Gold|Silver|Purple|", message = "Invalid color selected", groups = PartType.Groups.Housing.class)
    private String housingColor;
    private Boolean housingWithButtons;
    private Boolean housingWithBackGlass;

    // ---- FLEX CABLE ----
    @NotEmpty(message = "The function is required!", groups = PartType.Groups.FlexCable.class)
    @Pattern(regexp = "Power/volume|Home button|Proximity|Antenna|Main board|Loudspeaker", message = "Invalid function selected", groups = PartType.Groups.FlexCable.class)
    private String flexFunction;

    // ---- CAMERA ----
    @NotEmpty(message = "The position is required!", groups = PartType.Groups.Camera.class)
    @Pattern(regexp = "Rear main|Rear ultrawide|Rear telephoto|Front", message = "Invalid position selected", groups = PartType.Groups.Camera.class)
    private String cameraPosition;
    // Sanity range, not a specification: wide enough for any phone camera module.
    @DecimalMin(value = "0.3", message = "The resolution must be between 0.3 and 200 MP", groups = PartType.Groups.Camera.class)
    @DecimalMax(value = "200", message = "The resolution must be between 0.3 and 200 MP", groups = PartType.Groups.Camera.class)
    private Double cameraMegapixels;
    private Boolean cameraModule;

    // ---- FINGERPRINT ----
    @NotEmpty(message = "The position is required!", groups = PartType.Groups.Fingerprint.class)
    @Pattern(regexp = "Rear-mounted|Side / power button|Under display optical|Under display ultrasonic",
            message = "Invalid position selected", groups = PartType.Groups.Fingerprint.class)
    private String fingerprintPosition;
    private Boolean fingerprintWithFlex;

    // ---- SENSOR ----
    @NotEmpty(message = "The kind is required!", groups = PartType.Groups.Sensor.class)
    @Pattern(regexp = "Proximity|Face ID dot projector|Gyroscope|Ambient light", message = "Invalid kind selected", groups = PartType.Groups.Sensor.class)
    private String sensorKind;
    private Boolean sensorUnderDisplay;

    // ---- OTHER has no spec fields ----

    public Long getCellphonePartId() {
        return cellphonePartId;
    }

    public void setCellphonePartId(Long cellphonePartId) {
        this.cellphonePartId = cellphonePartId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(String storageSize) {
        this.storageSize = storageSize;
    }

    public int getStocks() {
        return stocks;
    }

    public void setStocks(int stocks) {
        this.stocks = stocks;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public String getCompatibleModels() {
        return compatibleModels;
    }

    public void setCompatibleModels(String compatibleModels) {
        this.compatibleModels = compatibleModels;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public PartCondition getPartCondition() {
        return partCondition;
    }

    public void setPartCondition(PartCondition partCondition) {
        this.partCondition = partCondition;
    }

    public Integer getWarrantyDays() {
        return warrantyDays;
    }

    public void setWarrantyDays(Integer warrantyDays) {
        this.warrantyDays = warrantyDays;
    }

    public String getScreenPanelType() {
        return screenPanelType;
    }

    public void setScreenPanelType(String screenPanelType) {
        this.screenPanelType = screenPanelType;
    }

    public String getScreenGrade() {
        return screenGrade;
    }

    public void setScreenGrade(String screenGrade) {
        this.screenGrade = screenGrade;
    }

    public Double getScreenSizeInches() {
        return screenSizeInches;
    }

    public void setScreenSizeInches(Double screenSizeInches) {
        this.screenSizeInches = screenSizeInches;
    }

    public Boolean getScreenWithFrame() {
        return screenWithFrame;
    }

    public void setScreenWithFrame(Boolean screenWithFrame) {
        this.screenWithFrame = screenWithFrame;
    }

    public Boolean getScreenTouchIncluded() {
        return screenTouchIncluded;
    }

    public void setScreenTouchIncluded(Boolean screenTouchIncluded) {
        this.screenTouchIncluded = screenTouchIncluded;
    }

    public Integer getBatteryCapacityMah() {
        return batteryCapacityMah;
    }

    public void setBatteryCapacityMah(Integer batteryCapacityMah) {
        this.batteryCapacityMah = batteryCapacityMah;
    }

    public Double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(Double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public String getBatteryChemistry() {
        return batteryChemistry;
    }

    public void setBatteryChemistry(String batteryChemistry) {
        this.batteryChemistry = batteryChemistry;
    }

    public String getPortConnector() {
        return portConnector;
    }

    public void setPortConnector(String portConnector) {
        this.portConnector = portConnector;
    }

    public Boolean getPortOnFlex() {
        return portOnFlex;
    }

    public void setPortOnFlex(Boolean portOnFlex) {
        this.portOnFlex = portOnFlex;
    }

    public Boolean getPortWithMic() {
        return portWithMic;
    }

    public void setPortWithMic(Boolean portWithMic) {
        this.portWithMic = portWithMic;
    }

    public String getCoverMaterial() {
        return coverMaterial;
    }

    public void setCoverMaterial(String coverMaterial) {
        this.coverMaterial = coverMaterial;
    }

    public String getCoverColor() {
        return coverColor;
    }

    public void setCoverColor(String coverColor) {
        this.coverColor = coverColor;
    }

    public Boolean getCoverWithLens() {
        return coverWithLens;
    }

    public void setCoverWithLens(Boolean coverWithLens) {
        this.coverWithLens = coverWithLens;
    }

    public String getHousingColor() {
        return housingColor;
    }

    public void setHousingColor(String housingColor) {
        this.housingColor = housingColor;
    }

    public Boolean getHousingWithButtons() {
        return housingWithButtons;
    }

    public void setHousingWithButtons(Boolean housingWithButtons) {
        this.housingWithButtons = housingWithButtons;
    }

    public Boolean getHousingWithBackGlass() {
        return housingWithBackGlass;
    }

    public void setHousingWithBackGlass(Boolean housingWithBackGlass) {
        this.housingWithBackGlass = housingWithBackGlass;
    }

    public String getFlexFunction() {
        return flexFunction;
    }

    public void setFlexFunction(String flexFunction) {
        this.flexFunction = flexFunction;
    }

    public String getCameraPosition() {
        return cameraPosition;
    }

    public void setCameraPosition(String cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    public Double getCameraMegapixels() {
        return cameraMegapixels;
    }

    public void setCameraMegapixels(Double cameraMegapixels) {
        this.cameraMegapixels = cameraMegapixels;
    }

    public Boolean getCameraModule() {
        return cameraModule;
    }

    public void setCameraModule(Boolean cameraModule) {
        this.cameraModule = cameraModule;
    }

    public String getFingerprintPosition() {
        return fingerprintPosition;
    }

    public void setFingerprintPosition(String fingerprintPosition) {
        this.fingerprintPosition = fingerprintPosition;
    }

    public Boolean getFingerprintWithFlex() {
        return fingerprintWithFlex;
    }

    public void setFingerprintWithFlex(Boolean fingerprintWithFlex) {
        this.fingerprintWithFlex = fingerprintWithFlex;
    }

    public String getSensorKind() {
        return sensorKind;
    }

    public void setSensorKind(String sensorKind) {
        this.sensorKind = sensorKind;
    }

    public Boolean getSensorUnderDisplay() {
        return sensorUnderDisplay;
    }

    public void setSensorUnderDisplay(Boolean sensorUnderDisplay) {
        this.sensorUnderDisplay = sensorUnderDisplay;
    }
}
