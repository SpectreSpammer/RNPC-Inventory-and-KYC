package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.Date;


@Entity
@Table(name = "rnpc_cellphone_parts")
public class CellphoneParts {

    /*
     * One table, common fields plus nullable per-type spec columns - the same shape as LaptopParts.
     * Every redesign column is NULLABLE (wrapper types, no nullable = false), because
     * ddl-auto=update only ever adds, and a NOT NULL column added to a populated table would be
     * filled with '' or 0 - which an enum cannot load. part_type and part_condition are made NOT
     * NULL by hand instead (see "Database changes not in migrations" in CLAUDE.md).
     *
     * Every column is named explicitly. Spring's default naming never splits a trailing capital,
     * and explicit names keep the DDL in CLAUDE.md honest.
     *
     * The pre-redesign category and storage_size columns are no longer mapped; they stay in the
     * table until dropped by hand, because ddl-auto never drops.
     */

    /**
     * The ten part types a phone repair shop stocks. The slug is the URL segment of the per-type
     * create routes (/cellphone/{slug}/create) - note SCREEN's is "lcd", not "screen"; group is the
     * Bean Validation group carrying that type's required-field rules on the DTO.
     *
     * Stored as VARCHAR(32), not a native MySQL ENUM: Hibernate 6 can generate ENUM('SCREEN', ...)
     * for @Enumerated(STRING), and ddl-auto=update cannot alter it when a constant is added.
     */
    public enum PartType {
        SCREEN("LCD / Screen", "lcd", Groups.Screen.class),
        BATTERY("Battery", "battery", Groups.Battery.class),
        CHARGING_BOARD("Charging board", "charging-board", Groups.ChargingBoard.class),
        BACK_GLASS("Back glass", "back-glass", Groups.BackGlass.class),
        HOUSING("Housing / frame", "housing", Groups.Housing.class),
        FLEX_CABLE("Flex cable", "flex-cable", Groups.FlexCable.class),
        CAMERA("Camera", "camera", Groups.Camera.class),
        FINGERPRINT("Fingerprint", "fingerprint", Groups.Fingerprint.class),
        SENSOR("Sensor", "sensor", Groups.Sensor.class),
        OTHER("Other", "other", Groups.Other.class);

        /**
         * Bean Validation group markers, one per type. Every type group extends Typed, so a rule
         * in Typed (e.g. partCondition required) applies to every type-based form: validating a
         * group also validates the groups it extends.
         */
        public interface Groups {
            interface Typed {}
            interface Screen extends Typed {}
            interface Battery extends Typed {}
            interface ChargingBoard extends Typed {}
            interface BackGlass extends Typed {}
            interface Housing extends Typed {}
            interface FlexCable extends Typed {}
            interface Camera extends Typed {}
            interface Fingerprint extends Typed {}
            interface Sensor extends Typed {}
            interface Other extends Typed {}
        }

        private final String label;
        private final String slug;
        private final Class<?> group;

        PartType(String label, String slug, Class<?> group) {
            this.label = label;
            this.slug = slug;
            this.group = group;
        }

        public String getLabel() { return label; }
        public String getSlug() { return slug; }
        public Class<?> getGroup() { return group; }

        /** The type for a URL slug, or null when there is none - the caller turns that into a 404. */
        public static PartType fromSlug(String slug) {
            if (slug == null) return null;
            for (PartType t : values()) {
                if (t.slug.equalsIgnoreCase(slug)) return t;
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cellphonePartId;

    private String brand;
    /** Free text, e.g. "Galaxy A12 battery". */
    private String partName;
    private int stocks;
    private double price;

    /** Notes, mapped onto the pre-existing description column rather than renamed - ddl-auto cannot rename. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String notes;
    private Date createdAt;
    private String imageFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", length = 32, columnDefinition = "VARCHAR(32)")
    private PartType partType;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_condition", length = 32, columnDefinition = "VARCHAR(32)")
    private PartCondition partCondition;

    // ---- Common fields. brand and partName are above. ----
    /** Free text, e.g. "Galaxy A12, A125F" */
    @Column(name = "compatible_models", length = 500)
    private String compatibleModels;
    /** Optional; service-pack codes go here too */
    @Column(name = "part_number", length = 64)
    private String partNumber;
    /** Null means no warranty */
    @Column(name = "warranty_days")
    private Integer warrantyDays;

    // ---- LCD / Screen ----
    @Column(name = "screen_panel_type")
    private String screenPanelType;
    @Column(name = "screen_grade")
    private String screenGrade;
    @Column(name = "screen_size_inches")
    private Double screenSizeInches;
    @Column(name = "screen_with_frame")
    private Boolean screenWithFrame;
    @Column(name = "screen_touch_included")
    private Boolean screenTouchIncluded;

    // ---- Battery ----
    @Column(name = "battery_capacity_mah")
    private Integer batteryCapacityMah;
    @Column(name = "battery_voltage")
    private Double batteryVoltage;
    @Column(name = "battery_chemistry")
    private String batteryChemistry;

    // ---- Charging board ----
    @Column(name = "port_connector")
    private String portConnector;
    @Column(name = "port_on_flex")
    private Boolean portOnFlex;
    @Column(name = "port_with_mic")
    private Boolean portWithMic;

    // ---- Back glass ----
    @Column(name = "cover_material")
    private String coverMaterial;
    @Column(name = "cover_color")
    private String coverColor;
    @Column(name = "cover_with_lens")
    private Boolean coverWithLens;

    // ---- Housing / frame ----
    @Column(name = "housing_color")
    private String housingColor;
    @Column(name = "housing_with_buttons")
    private Boolean housingWithButtons;
    @Column(name = "housing_with_back_glass")
    private Boolean housingWithBackGlass;

    // ---- Flex cable ----
    @Column(name = "flex_function")
    private String flexFunction;

    // ---- Camera ----
    @Column(name = "camera_position")
    private String cameraPosition;
    @Column(name = "camera_megapixels")
    private Double cameraMegapixels;
    @Column(name = "camera_module")
    private Boolean cameraModule;

    // ---- Fingerprint ----
    @Column(name = "fingerprint_position")
    private String fingerprintPosition;
    @Column(name = "fingerprint_with_flex")
    private Boolean fingerprintWithFlex;

    // ---- Sensor ----
    @Column(name = "sensor_kind")
    private String sensorKind;
    @Column(name = "sensor_under_display")
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public PartType getPartType() {
        return partType;
    }

    public void setPartType(PartType partType) {
        this.partType = partType;
    }

    public PartCondition getPartCondition() {
        return partCondition;
    }

    public void setPartCondition(PartCondition partCondition) {
        this.partCondition = partCondition;
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
