package com.rnpc.inventory.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;



@Entity
@Table(name = "rnpc_laptop_parts")
public class LaptopParts {

	/*
	 * Laptop parts redesign, batch 1: additive only. One table, common fields plus nullable
	 * per-type spec columns. Every column added here is NULLABLE (wrapper types, no
	 * nullable = false) because ddl-auto=update only ever adds, and a NOT NULL column added to a
	 * populated table would be filled with '' or 0 - which an enum cannot load.
	 *
	 * Every new column is named explicitly. Spring's default naming would turn chargerCurrentA
	 * into "charger_currenta" (it never splits the final character), and explicit names keep the
	 * DDL in CLAUDE.md honest.
	 *
	 * category and storageSize are the old model and stay until the create/edit batch replaces
	 * the current forms. Their columns stay in the table after that either way: ddl-auto never
	 * drops.
	 */

	/**
	 * The fourteen part types. The slug is the URL segment the per-type create routes will use
	 * (/laptop/{slug}/create); group is the Bean Validation group carrying that type's
	 * required-field rules on the DTO.
	 *
	 * Stored as VARCHAR(32), not a native MySQL ENUM: Hibernate 6 can generate ENUM('LCD', ...)
	 * for @Enumerated(STRING), and ddl-auto=update cannot alter it when a constant is added.
	 */
	public enum PartType {
		LCD("LCD / Screen", "lcd", Groups.Lcd.class),
		KEYBOARD("Keyboard", "keyboard", Groups.Keyboard.class),
		BATTERY("Battery", "battery", Groups.Battery.class),
		CHARGER("Charger", "charger", Groups.Charger.class),
		RAM("RAM", "ram", Groups.Ram.class),
		STORAGE("Storage", "storage", Groups.Storage.class),
		FAN("Fan / Heatsink", "fan", Groups.Fan.class),
		MOTHERBOARD("Motherboard", "motherboard", Groups.Motherboard.class),
		CASING("Casing", "casing", Groups.Casing.class),
		HINGES("Hinges", "hinges", Groups.Hinges.class),
		DC_JACK("DC Jack / Port", "dc-jack", Groups.DcJack.class),
		WIFI_CARD("Wi-Fi Card", "wifi-card", Groups.WifiCard.class),
		TOUCHPAD("Touchpad", "touchpad", Groups.Touchpad.class),
		OTHER("Other", "other", Groups.Other.class);

		/** Bean Validation group markers, one per type. */
		public interface Groups {
			interface Lcd {}
			interface Keyboard {}
			interface Battery {}
			interface Charger {}
			interface Ram {}
			interface Storage {}
			interface Fan {}
			interface Motherboard {}
			interface Casing {}
			interface Hinges {}
			interface DcJack {}
			interface WifiCard {}
			interface Touchpad {}
			interface Other {}
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

	/** Column is part_condition: CONDITION is a reserved word in MySQL/MariaDB. */
	public enum PartCondition {
		NEW("New"),
		OEM_PULL("OEM pull"),
		REFURBISHED("Refurbished");

		private final String label;

		PartCondition(String label) {
			this.label = label;
		}

		public String getLabel() { return label; }
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int laptopPartId;


	private String brand;
	/** Existing column. Old model: the part type (LCD, Keyboard, ...). New model: free text. */
	private String partName;
	/** Old model (laptop type). Kept until the create/edit batch. */
	private String category;
	/** Old model. Kept until the create/edit batch. */
	private String storageSize;
	private int stocks;
	private double price;

	/**
	 * The redesign's "notes", mapped onto the existing description column rather than renamed -
	 * ddl-auto cannot rename. getDescription()/setDescription() below keep the current
	 * controller, service and templates working unchanged.
	 */
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

	// ---- Common fields added by the redesign. partName and brand are the existing columns above. ----
	/** Free text, e.g. "Inspiron 15 3511, 3520" */
	@Column(name = "compatible_models", length = 500)
	private String compatibleModels;
	/** Optional; battery codes go here too */
	@Column(name = "part_number", length = 64)
	private String partNumber;
	/** Null means no warranty */
	@Column(name = "warranty_days")
	private Integer warrantyDays;

	// ---- LCD / Screen ----
	@Column(name = "lcd_size_inches")
	private Double lcdSizeInches;
	@Column(name = "lcd_resolution")
	private String lcdResolution;
	@Column(name = "lcd_panel_type")
	private String lcdPanelType;
	/** "30-pin eDP", "40-pin eDP" or "40-pin LVDS" */
	@Column(name = "lcd_connector")
	private String lcdConnector;
	@Column(name = "lcd_refresh_rate_hz")
	private Integer lcdRefreshRateHz;
	@Column(name = "lcd_surface")
	private String lcdSurface;
	@Column(name = "lcd_touch")
	private Boolean lcdTouch;
	@Column(name = "lcd_mounting")
	private String lcdMounting;

	// ---- Keyboard ----
	@Column(name = "kb_layout")
	private String kbLayout;
	@Column(name = "kb_backlit")
	private Boolean kbBacklit;
	@Column(name = "kb_color")
	private String kbColor;
	@Column(name = "kb_with_palmrest")
	private Boolean kbWithPalmrest;
	@Column(name = "kb_with_frame")
	private Boolean kbWithFrame;

	// ---- Battery ----
	@Column(name = "battery_capacity_wh")
	private Double batteryCapacityWh;
	@Column(name = "battery_voltage")
	private Double batteryVoltage;
	@Column(name = "battery_cells")
	private Integer batteryCells;
	@Column(name = "battery_chemistry")
	private String batteryChemistry;

	// ---- Charger ----
	@Column(name = "charger_wattage")
	private Integer chargerWattage;
	@Column(name = "charger_output_voltage")
	private Double chargerOutputVoltage;
	@Column(name = "charger_current_a")
	private Double chargerCurrentA;
	@Column(name = "charger_connector_tip")
	private String chargerConnectorTip;
	@Column(name = "charger_includes_cord")
	private Boolean chargerIncludesCord;

	// ---- RAM ----
	@Column(name = "ram_type")
	private String ramType;
	@Column(name = "ram_capacity_gb")
	private Integer ramCapacityGb;
	@Column(name = "ram_speed_mts")
	private Integer ramSpeedMts;

	// ---- Storage ----
	@Column(name = "storage_type")
	private String storageType;
	@Column(name = "storage_capacity_gb")
	private Integer storageCapacityGb;
	@Column(name = "storage_form_factor")
	private String storageFormFactor;
	@Column(name = "storage_interface")
	private String storageInterface;

	// ---- Fan / Heatsink ----
	@Column(name = "fan_assembly")
	private String fanAssembly;
	@Column(name = "fan_connector_pins")
	private Integer fanConnectorPins;
	@Column(name = "fan_voltage")
	private Double fanVoltage;

	// ---- Motherboard ----
	@Column(name = "mb_onboard_cpu")
	private String mbOnboardCpu;
	@Column(name = "mb_gpu")
	private String mbGpu;
	@Column(name = "mb_onboard_ram")
	private String mbOnboardRam;
	@Column(name = "mb_tested_status")
	private String mbTestedStatus;

	// ---- Casing ----
	@Column(name = "casing_panel")
	private String casingPanel;
	@Column(name = "casing_color")
	private String casingColor;

	// ---- Hinges ----
	@Column(name = "hinge_side")
	private String hingeSide;

	// ---- DC Jack / Port ----
	@Column(name = "dc_jack_type")
	private String dcJackType;
	@Column(name = "dc_jack_tip_size")
	private String dcJackTipSize;

	// ---- Wi-Fi Card ----
	@Column(name = "wifi_standard")
	private String wifiStandard;
	@Column(name = "wifi_form_factor")
	private String wifiFormFactor;
	@Column(name = "wifi_bluetooth_version")
	private String wifiBluetoothVersion;

	// ---- Touchpad ----
	@Column(name = "touchpad_connector")
	private String touchpadConnector;
	@Column(name = "touchpad_with_bracket")
	private Boolean touchpadWithBracket;
	@Column(name = "touchpad_color")
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
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	/** Old name for notes; same column. */
	public String getDescription() {
		return notes;
	}
	public void setDescription(String description) {
		this.notes = description;
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
