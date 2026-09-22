package com.rnpc.inventory.dto;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/*
 * One DTO for both laptop models, sorted into Bean Validation groups:
 *
 *   Default             - rules every form shares (brand, partName, stocks, price, field lengths).
 *   PartType.Groups.X   - one per type, e.g. Lcd; each extends Typed, which holds the rules every
 *                         type-based form shares (partCondition). Run by the controller as
 *                         validate(dto, result, Default.class, type.getGroup()).
 *   Legacy              - the pre-redesign rules (category, storageSize, the old partName
 *                         allow-list, description's 10-character minimum). Only the old
 *                         /laptop/create path and the edit of a partType-null row run it.
 *
 * There is deliberately no partType field: the type comes from the route, never the form.
 * "notes" in the redesign is still bound as description here - it is the same column
 * (LaptopParts.notes maps onto description).
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
	@Pattern(regexp = "LCD|Keyboard|Trackpad|Ram|SSD|M.2", message = "Invalid part name selected", groups = PartType.Groups.Legacy.class)
	private String partName;

	@NotEmpty(message = "The category is required!", groups = PartType.Groups.Legacy.class)
	@Pattern(regexp = "Convertible|Netbook|Notebook|Gaming Laptop|Macbook", message = "Invalid category selected", groups = PartType.Groups.Legacy.class)
	private String category;

	@NotEmpty(message = "The storage size is required!", groups = PartType.Groups.Legacy.class)
	@Pattern(regexp = "None Applicable|120 GB|240 GB|500 GB|1 TB|2 TB", message = "Invalid storage size selected", groups = PartType.Groups.Legacy.class)
	private String storageSize;

	@Min(value = 0, message = "The stocks cannot be negative!")
	private int stocks;

	@Min(0)
	private double price;

	@Size(min = 10, message = "The description should be at least 10 characters", groups = PartType.Groups.Legacy.class)
	@Size(max = 2000, message = "The description cannot exceed 2000 characters")
	private String description;

	private Date createdAt;

	private String imageFileName;

	private MultipartFile imageFile;

	// ---- Common fields of the type-based model ----

	@NotEmpty(message = "The compatible models are required!", groups = {PartType.Groups.Lcd.class, PartType.Groups.Battery.class})
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
	private String kbLayout;
	private Boolean kbBacklit;
	private String kbColor;
	private Boolean kbWithPalmrest;
	private Boolean kbWithFrame;

	// ---- BATTERY ----
	@NotNull(message = "The capacity is required!", groups = PartType.Groups.Battery.class)
	@Positive(message = "The capacity must be greater than 0!", groups = PartType.Groups.Battery.class)
	private Double batteryCapacityWh;
	@NotNull(message = "The voltage is required!", groups = PartType.Groups.Battery.class)
	@Positive(message = "The voltage must be greater than 0!", groups = PartType.Groups.Battery.class)
	private Double batteryVoltage;
	@Positive(message = "Invalid cell count selected", groups = PartType.Groups.Battery.class)
	private Integer batteryCells;
	@Pattern(regexp = "Li-ion|Li-polymer|", message = "Invalid chemistry selected", groups = PartType.Groups.Battery.class)
	private String batteryChemistry;

	// ---- CHARGER ----
	private Integer chargerWattage;
	private Double chargerOutputVoltage;
	private Double chargerCurrentA;
	private String chargerConnectorTip;
	private Boolean chargerIncludesCord;

	// ---- RAM ----
	private String ramType;
	private Integer ramCapacityGb;
	private Integer ramSpeedMts;

	// ---- STORAGE ----
	private String storageType;
	private Integer storageCapacityGb;
	private String storageFormFactor;
	private String storageInterface;

	// ---- FAN ----
	private String fanAssembly;
	private Integer fanConnectorPins;
	private Double fanVoltage;

	// ---- MOTHERBOARD ----
	private String mbOnboardCpu;
	private String mbGpu;
	private String mbOnboardRam;
	private String mbTestedStatus;

	// ---- CASING ----
	private String casingPanel;
	private String casingColor;

	// ---- HINGES ----
	private String hingeSide;

	// ---- DC_JACK ----
	private String dcJackType;
	private String dcJackTipSize;

	// ---- WIFI_CARD ----
	private String wifiStandard;
	private String wifiFormFactor;
	private String wifiBluetoothVersion;

	// ---- TOUCHPAD ----
	private String touchpadConnector;
	private Boolean touchpadWithBracket;
	private String touchpadColor;

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

	public String getFanAssembly() {
		return fanAssembly;
	}

	public void setFanAssembly(String fanAssembly) {
		this.fanAssembly = fanAssembly;
	}

	public Integer getFanConnectorPins() {
		return fanConnectorPins;
	}

	public void setFanConnectorPins(Integer fanConnectorPins) {
		this.fanConnectorPins = fanConnectorPins;
	}

	public Double getFanVoltage() {
		return fanVoltage;
	}

	public void setFanVoltage(Double fanVoltage) {
		this.fanVoltage = fanVoltage;
	}

	public String getMbOnboardCpu() {
		return mbOnboardCpu;
	}

	public void setMbOnboardCpu(String mbOnboardCpu) {
		this.mbOnboardCpu = mbOnboardCpu;
	}

	public String getMbGpu() {
		return mbGpu;
	}

	public void setMbGpu(String mbGpu) {
		this.mbGpu = mbGpu;
	}

	public String getMbOnboardRam() {
		return mbOnboardRam;
	}

	public void setMbOnboardRam(String mbOnboardRam) {
		this.mbOnboardRam = mbOnboardRam;
	}

	public String getMbTestedStatus() {
		return mbTestedStatus;
	}

	public void setMbTestedStatus(String mbTestedStatus) {
		this.mbTestedStatus = mbTestedStatus;
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
