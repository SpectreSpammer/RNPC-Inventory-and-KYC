package com.rnpc.inventory.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/*
 * One DTO for every laptop part type, sorted into Bean Validation groups:
 *
 *   Default             - rules every form shares (brand, partName, stocks, price, field lengths).
 *   PartType.Groups.X   - one per type, e.g. Lcd; each extends Typed, which holds the rules every
 *                         type's form shares (partCondition). Run by the controller as
 *                         validate(dto, result, Default.class, type.getGroup()).
 *
 * There is deliberately no partType field: the type comes from the route, never the form.
 * "Notes" is bound as description here - LaptopParts.notes maps onto the description column.
 */
public class LaptopPartsDto {

	/** Single source for the brand allow-list and the brand dropdown. */
	public static final String BRAND_PATTERN = "Acer|Apple|Asus|Dell|HP|Huawei|Lenovo|MSI|Samsung|Toshiba|Generic";
	public static final List<String> BRAND_OPTIONS = Arrays.asList(BRAND_PATTERN.split("\\|"));

	private int laptopPartId;

	@NotEmpty(message = "The brand is required!")
	@Pattern(regexp = BRAND_PATTERN, message = "Invalid brand selected")
	private String brand;

	@NotEmpty(message = "The part name is required!")
	@Size(max = 255, message = "The part name cannot exceed 255 characters")
	private String partName;

	@Min(value = 0, message = "The stocks cannot be negative!")
	private int stocks;

	@Min(0)
	private double price;

	/** The Notes field on every form (LaptopParts.notes, stored in the description column). */
	@Size(max = 2000, message = "The notes cannot exceed 2000 characters")
	private String description;

	private Date createdAt;

	private String imageFileName;

	private MultipartFile imageFile;

	// ---- Common fields of the type-based model ----

	// Required for the model-specific types; optional for generic ones (Charger, RAM, Storage,
	// Wi-Fi Card, Other).
	@NotEmpty(message = "The compatible models are required!", groups = {PartType.Groups.Lcd.class,
			PartType.Groups.Battery.class, PartType.Groups.Keyboard.class, PartType.Groups.Casing.class,
			PartType.Groups.Hinges.class, PartType.Groups.DcJack.class, PartType.Groups.Touchpad.class})
	@Size(max = 500, message = "The compatible models cannot exceed 500 characters")
	private String compatibleModels;

	@Size(max = 64, message = "The part number cannot exceed 64 characters")
	private String partNumber;

	@NotNull(message = "The condition is required!", groups = PartType.Groups.Typed.class)
	private PartCondition partCondition;

	@Min(value = 0, message = "The warranty days cannot be negative!")
	private Integer warrantyDays;

	// ---- LCD ----
	@NotNull(message = "The screen size is required!", groups = PartType.Groups.Lcd.class)
	@Positive(message = "The screen size must be greater than 0!", groups = PartType.Groups.Lcd.class)
	private Double lcdSizeInches;
	@NotEmpty(message = "The resolution is required!", groups = PartType.Groups.Lcd.class)
	@Pattern(regexp = "HD 1366x768|HD\\+ 1600x900|FHD 1920x1080|QHD 2560x1440|UHD 3840x2160", message = "Invalid resolution selected", groups = PartType.Groups.Lcd.class)
	private String lcdResolution;
	@NotEmpty(message = "The panel type is required!", groups = PartType.Groups.Lcd.class)
	@Pattern(regexp = "TN|IPS|VA|OLED", message = "Invalid panel type selected", groups = PartType.Groups.Lcd.class)
	private String lcdPanelType;
	@NotEmpty(message = "The connector is required!", groups = PartType.Groups.Lcd.class)
	@Pattern(regexp = "30-pin eDP|40-pin eDP|40-pin LVDS", message = "Invalid connector selected", groups = PartType.Groups.Lcd.class)
	private String lcdConnector;
	@Positive(message = "Invalid refresh rate selected", groups = PartType.Groups.Lcd.class)
	private Integer lcdRefreshRateHz;
	@Pattern(regexp = "Matte|Glossy|", message = "Invalid surface selected", groups = PartType.Groups.Lcd.class)
	private String lcdSurface;
	private Boolean lcdTouch;
	@Pattern(regexp = "With brackets|Without brackets|Slim|", message = "Invalid mounting selected", groups = PartType.Groups.Lcd.class)
	private String lcdMounting;

	// ---- KEYBOARD ----
	@NotEmpty(message = "The layout is required!", groups = PartType.Groups.Keyboard.class)
	@Pattern(regexp = "US|UK", message = "Invalid layout selected", groups = PartType.Groups.Keyboard.class)
	private String kbLayout;
	private Boolean kbBacklit;
	@Pattern(regexp = "Black|Silver|White|Grey|", message = "Invalid color selected", groups = PartType.Groups.Keyboard.class)
	private String kbColor;
	private Boolean kbWithPalmrest;
	private Boolean kbWithFrame;

	// ---- BATTERY ----
	// Sanity ranges, not specifications: wide enough for any laptop battery, narrow enough to catch
	// a slipped decimal or a mAh figure typed into the Wh field.
	@NotNull(message = "The capacity is required!", groups = PartType.Groups.Battery.class)
	@DecimalMin(value = "10", message = "The capacity must be between 10 and 150 Wh", groups = PartType.Groups.Battery.class)
	@DecimalMax(value = "150", message = "The capacity must be between 10 and 150 Wh", groups = PartType.Groups.Battery.class)
	private Double batteryCapacityWh;
	@NotNull(message = "The voltage is required!", groups = PartType.Groups.Battery.class)
	@DecimalMin(value = "3", message = "The voltage must be between 3 and 20 V", groups = PartType.Groups.Battery.class)
	@DecimalMax(value = "20", message = "The voltage must be between 3 and 20 V", groups = PartType.Groups.Battery.class)
	private Double batteryVoltage;
	@Positive(message = "Invalid cell count selected", groups = PartType.Groups.Battery.class)
	private Integer batteryCells;
	@Pattern(regexp = "Li-ion|Li-polymer|", message = "Invalid chemistry selected", groups = PartType.Groups.Battery.class)
	private String batteryChemistry;

	// ---- CHARGER ----
	// Numeric selects (wattage here, capacities and speed below) are guarded with @Positive rather
	// than an exact allow-list: @Pattern does not apply to numbers, and an off-list number from a
	// tampered form is harmless. String selects keep exact @Pattern allow-lists.
	@NotNull(message = "The wattage is required!", groups = PartType.Groups.Charger.class)
	@Positive(message = "Invalid wattage selected", groups = PartType.Groups.Charger.class)
	private Integer chargerWattage;
	@NotNull(message = "The output voltage is required!", groups = PartType.Groups.Charger.class)
	@DecimalMin(value = "5", message = "The output voltage must be between 5 and 48 V", groups = PartType.Groups.Charger.class)
	@DecimalMax(value = "48", message = "The output voltage must be between 5 and 48 V", groups = PartType.Groups.Charger.class)
	private Double chargerOutputVoltage;
	@DecimalMin(value = "0.5", message = "The current must be between 0.5 and 10 A", groups = PartType.Groups.Charger.class)
	@DecimalMax(value = "10", message = "The current must be between 0.5 and 10 A", groups = PartType.Groups.Charger.class)
	private Double chargerCurrentA;
	@NotEmpty(message = "The connector tip is required!", groups = PartType.Groups.Charger.class)
	@Pattern(regexp = "4\\.5x3\\.0mm|7\\.4x5\\.0mm|5\\.5x2\\.5mm|4\\.0x1\\.7mm|USB-C|Lenovo slim tip", message = "Invalid connector tip selected", groups = PartType.Groups.Charger.class)
	private String chargerConnectorTip;
	private Boolean chargerIncludesCord;

	// ---- RAM ----
	@NotEmpty(message = "The RAM type is required!", groups = PartType.Groups.Ram.class)
	@Pattern(regexp = "DDR3L|DDR4|DDR5|LPDDR4X|LPDDR5", message = "Invalid RAM type selected", groups = PartType.Groups.Ram.class)
	private String ramType;
	@NotNull(message = "The capacity is required!", groups = PartType.Groups.Ram.class)
	@Positive(message = "Invalid capacity selected", groups = PartType.Groups.Ram.class)
	private Integer ramCapacityGb;
	@Positive(message = "Invalid speed selected", groups = PartType.Groups.Ram.class)
	private Integer ramSpeedMts;

	// ---- STORAGE ----
	@NotEmpty(message = "The storage type is required!", groups = PartType.Groups.Storage.class)
	@Pattern(regexp = "SATA SSD|NVMe SSD|HDD|eMMC", message = "Invalid storage type selected", groups = PartType.Groups.Storage.class)
	private String storageType;
	@NotNull(message = "The capacity is required!", groups = PartType.Groups.Storage.class)
	@Positive(message = "Invalid capacity selected", groups = PartType.Groups.Storage.class)
	private Integer storageCapacityGb;
	@NotEmpty(message = "The form factor is required!", groups = PartType.Groups.Storage.class)
	@Pattern(regexp = "2\\.5\"|M\\.2 2280|M\\.2 2242|M\\.2 2230", message = "Invalid form factor selected", groups = PartType.Groups.Storage.class)
	private String storageFormFactor;
	@Pattern(regexp = "SATA III|PCIe 3\\.0 x4|PCIe 4\\.0 x4|", message = "Invalid interface selected", groups = PartType.Groups.Storage.class)
	private String storageInterface;

	// ---- CASING ----
	@NotEmpty(message = "The panel is required!", groups = PartType.Groups.Casing.class)
	@Pattern(regexp = "A cover \\(lid\\)|B bezel|C palmrest|D bottom", message = "Invalid panel selected", groups = PartType.Groups.Casing.class)
	private String casingPanel;
	@Pattern(regexp = "Black|Silver|White|Grey|Blue|Gold|", message = "Invalid color selected", groups = PartType.Groups.Casing.class)
	private String casingColor;

	// ---- HINGES ----
	@NotEmpty(message = "The side is required!", groups = PartType.Groups.Hinges.class)
	@Pattern(regexp = "Left|Right|Pair", message = "Invalid side selected", groups = PartType.Groups.Hinges.class)
	private String hingeSide;

	// ---- DC_JACK ----
	@NotEmpty(message = "The jack type is required!", groups = PartType.Groups.DcJack.class)
	@Pattern(regexp = "Barrel with cable|Soldered barrel|USB-C board|USB-C with cable", message = "Invalid jack type selected", groups = PartType.Groups.DcJack.class)
	private String dcJackType;
	@Pattern(regexp = "4\\.5x3\\.0mm|7\\.4x5\\.0mm|5\\.5x2\\.5mm|4\\.0x1\\.7mm|Lenovo slim tip|", message = "Invalid tip size selected", groups = PartType.Groups.DcJack.class)
	private String dcJackTipSize;

	// ---- WIFI_CARD ----
	@NotEmpty(message = "The Wi-Fi standard is required!", groups = PartType.Groups.WifiCard.class)
	@Pattern(regexp = "Wi-Fi 5|Wi-Fi 6|Wi-Fi 6E|Wi-Fi 7", message = "Invalid Wi-Fi standard selected", groups = PartType.Groups.WifiCard.class)
	private String wifiStandard;
	@NotEmpty(message = "The form factor is required!", groups = PartType.Groups.WifiCard.class)
	@Pattern(regexp = "M\\.2 2230|M\\.2 2242|Mini PCIe", message = "Invalid form factor selected", groups = PartType.Groups.WifiCard.class)
	private String wifiFormFactor;
	@Pattern(regexp = "4\\.2|5\\.0|5\\.1|5\\.2|5\\.3|5\\.4|", message = "Invalid Bluetooth version selected", groups = PartType.Groups.WifiCard.class)
	private String wifiBluetoothVersion;

	// ---- TOUCHPAD ----
	@Size(max = 100, message = "The connector cannot exceed 100 characters", groups = PartType.Groups.Touchpad.class)
	private String touchpadConnector;
	private Boolean touchpadWithBracket;
	@Pattern(regexp = "Black|Silver|White|Grey|", message = "Invalid color selected", groups = PartType.Groups.Touchpad.class)
	private String touchpadColor;

	// OTHER has no spec fields: its group (PartType.Groups.Other) carries only the Typed rules.

	public int getLaptopPartId() {
		return laptopPartId;
	}

	public void setLaptopPartId(int laptopPartId) {
		this.laptopPartId = laptopPartId;
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

	public Double getLcdSizeInches() {
		return lcdSizeInches;
	}

	public void setLcdSizeInches(Double lcdSizeInches) {
		this.lcdSizeInches = lcdSizeInches;
	}

	public String getLcdResolution() {
		return lcdResolution;
	}

	public void setLcdResolution(String lcdResolution) {
		this.lcdResolution = lcdResolution;
	}

	public String getLcdPanelType() {
		return lcdPanelType;
	}

	public void setLcdPanelType(String lcdPanelType) {
		this.lcdPanelType = lcdPanelType;
	}

	public String getLcdConnector() {
		return lcdConnector;
	}

	public void setLcdConnector(String lcdConnector) {
		this.lcdConnector = lcdConnector;
	}

	public Integer getLcdRefreshRateHz() {
		return lcdRefreshRateHz;
	}

	public void setLcdRefreshRateHz(Integer lcdRefreshRateHz) {
		this.lcdRefreshRateHz = lcdRefreshRateHz;
	}

	public String getLcdSurface() {
		return lcdSurface;
	}

	public void setLcdSurface(String lcdSurface) {
		this.lcdSurface = lcdSurface;
	}

	public Boolean getLcdTouch() {
		return lcdTouch;
	}

	public void setLcdTouch(Boolean lcdTouch) {
		this.lcdTouch = lcdTouch;
	}

	public String getLcdMounting() {
		return lcdMounting;
	}

	public void setLcdMounting(String lcdMounting) {
		this.lcdMounting = lcdMounting;
	}

	public String getKbLayout() {
		return kbLayout;
	}

	public void setKbLayout(String kbLayout) {
		this.kbLayout = kbLayout;
	}

	public Boolean getKbBacklit() {
		return kbBacklit;
	}

	public void setKbBacklit(Boolean kbBacklit) {
		this.kbBacklit = kbBacklit;
	}

	public String getKbColor() {
		return kbColor;
	}

	public void setKbColor(String kbColor) {
		this.kbColor = kbColor;
	}

	public Boolean getKbWithPalmrest() {
		return kbWithPalmrest;
	}

	public void setKbWithPalmrest(Boolean kbWithPalmrest) {
		this.kbWithPalmrest = kbWithPalmrest;
	}

	public Boolean getKbWithFrame() {
		return kbWithFrame;
	}

	public void setKbWithFrame(Boolean kbWithFrame) {
		this.kbWithFrame = kbWithFrame;
	}

	public Double getBatteryCapacityWh() {
		return batteryCapacityWh;
	}

	public void setBatteryCapacityWh(Double batteryCapacityWh) {
		this.batteryCapacityWh = batteryCapacityWh;
	}

	public Double getBatteryVoltage() {
		return batteryVoltage;
	}

	public void setBatteryVoltage(Double batteryVoltage) {
		this.batteryVoltage = batteryVoltage;
	}

	public Integer getBatteryCells() {
		return batteryCells;
	}

	public void setBatteryCells(Integer batteryCells) {
		this.batteryCells = batteryCells;
	}

	public String getBatteryChemistry() {
		return batteryChemistry;
	}

	public void setBatteryChemistry(String batteryChemistry) {
		this.batteryChemistry = batteryChemistry;
	}

	public Integer getChargerWattage() {
		return chargerWattage;
	}

	public void setChargerWattage(Integer chargerWattage) {
		this.chargerWattage = chargerWattage;
	}

	public Double getChargerOutputVoltage() {
		return chargerOutputVoltage;
	}

	public void setChargerOutputVoltage(Double chargerOutputVoltage) {
		this.chargerOutputVoltage = chargerOutputVoltage;
	}

	public Double getChargerCurrentA() {
		return chargerCurrentA;
	}

	public void setChargerCurrentA(Double chargerCurrentA) {
		this.chargerCurrentA = chargerCurrentA;
	}

	public String getChargerConnectorTip() {
		return chargerConnectorTip;
	}

	public void setChargerConnectorTip(String chargerConnectorTip) {
		this.chargerConnectorTip = chargerConnectorTip;
	}

	public Boolean getChargerIncludesCord() {
		return chargerIncludesCord;
	}

	public void setChargerIncludesCord(Boolean chargerIncludesCord) {
		this.chargerIncludesCord = chargerIncludesCord;
	}

	public String getRamType() {
		return ramType;
	}

	public void setRamType(String ramType) {
		this.ramType = ramType;
	}

	public Integer getRamCapacityGb() {
		return ramCapacityGb;
	}

	public void setRamCapacityGb(Integer ramCapacityGb) {
		this.ramCapacityGb = ramCapacityGb;
	}

	public Integer getRamSpeedMts() {
		return ramSpeedMts;
	}

	public void setRamSpeedMts(Integer ramSpeedMts) {
		this.ramSpeedMts = ramSpeedMts;
	}

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public Integer getStorageCapacityGb() {
		return storageCapacityGb;
	}

	public void setStorageCapacityGb(Integer storageCapacityGb) {
		this.storageCapacityGb = storageCapacityGb;
	}

	public String getStorageFormFactor() {
		return storageFormFactor;
	}

	public void setStorageFormFactor(String storageFormFactor) {
		this.storageFormFactor = storageFormFactor;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public String getCasingPanel() {
		return casingPanel;
	}

	public void setCasingPanel(String casingPanel) {
		this.casingPanel = casingPanel;
	}

	public String getCasingColor() {
		return casingColor;
	}

	public void setCasingColor(String casingColor) {
		this.casingColor = casingColor;
	}

	public String getHingeSide() {
		return hingeSide;
	}

	public void setHingeSide(String hingeSide) {
		this.hingeSide = hingeSide;
	}

	public String getDcJackType() {
		return dcJackType;
	}

	public void setDcJackType(String dcJackType) {
		this.dcJackType = dcJackType;
	}

	public String getDcJackTipSize() {
		return dcJackTipSize;
	}

	public void setDcJackTipSize(String dcJackTipSize) {
		this.dcJackTipSize = dcJackTipSize;
	}

	public String getWifiStandard() {
		return wifiStandard;
	}

	public void setWifiStandard(String wifiStandard) {
		this.wifiStandard = wifiStandard;
	}

	public String getWifiFormFactor() {
		return wifiFormFactor;
	}

	public void setWifiFormFactor(String wifiFormFactor) {
		this.wifiFormFactor = wifiFormFactor;
	}

	public String getWifiBluetoothVersion() {
		return wifiBluetoothVersion;
	}

	public void setWifiBluetoothVersion(String wifiBluetoothVersion) {
		this.wifiBluetoothVersion = wifiBluetoothVersion;
	}

	public String getTouchpadConnector() {
		return touchpadConnector;
	}

	public void setTouchpadConnector(String touchpadConnector) {
		this.touchpadConnector = touchpadConnector;
	}

	public Boolean getTouchpadWithBracket() {
		return touchpadWithBracket;
	}

	public void setTouchpadWithBracket(Boolean touchpadWithBracket) {
		this.touchpadWithBracket = touchpadWithBracket;
	}

	public String getTouchpadColor() {
		return touchpadColor;
	}

	public void setTouchpadColor(String touchpadColor) {
		this.touchpadColor = touchpadColor;
	}
}
