package com.rnpc.inventory.dto;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.LaptopParts.PartType;

/**
 * Display-ready view of one laptop part for the /laptop list page and its View modal - the laptop
 * counterpart of PartsListView. Everything the page shows is decided here, in Java: the type
 * label and tile code, the key-spec string, the three key-spec tiles, the Part details rows and
 * the per-type spec rows with their units. The template only lays them out.
 *
 * The page serializes a list of these into ALL_PARTS (th:inline), so every property is a plain
 * String/number/boolean or a list of Spec - no Date, no enum, nothing Jackson has to guess at.
 *
 * A pre-redesign row (partType null) is "Unassigned": no key spec, and its old category and
 * storage size shown as a "Legacy details" section so nothing stored is hidden.
 */
public class LaptopPartView {

    /** Key used for rows with no part type, in place of an enum name. */
    public static final String UNASSIGNED = "UNASSIGNED";
    public static final String UNASSIGNED_LABEL = "Unassigned";

    /** One labelled value. blank means "show Not set in grey". */
    public static class Spec {
        private final String label;
        private final String value;
        private final boolean blank;

        public Spec(String label, String value) {
            this.label = label;
            this.blank = value == null || value.isBlank();
            this.value = this.blank ? "Not set" : value;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
        public boolean isBlank() { return blank; }
    }

    private int id;
    private String typeKey;
    private String typeLabel;
    private String typeCode;
    private int typeOrder;
    private String brand;
    private String partName;
    private String partNumber;
    private String compatibleModels;
    private int stocks;
    private double price;
    private String imageFileName;
    private String keySpec;
    private List<Spec> keyTiles;
    private List<Spec> details;
    private String specsTitle;
    private List<Spec> specs;
    private String notes;
    private String added;

    public static LaptopPartView from(LaptopParts p) {
        LaptopPartView v = new LaptopPartView();
        PartType type = p.getPartType();
        v.id = p.getLaptopPartId();
        v.typeKey = type == null ? UNASSIGNED : type.name();
        v.typeLabel = type == null ? UNASSIGNED_LABEL : type.getLabel();
        v.typeCode = code(type);
        v.typeOrder = type == null ? PartType.values().length : type.ordinal();
        v.brand = nz(p.getBrand());
        v.partName = nz(p.getPartName());
        v.partNumber = nz(p.getPartNumber());
        v.compatibleModels = nz(p.getCompatibleModels());
        v.stocks = p.getStocks();
        v.price = p.getPrice();
        v.imageFileName = p.getImageFileName();
        v.keySpec = keySpec(p);
        List<Spec> allSpecs = specs(p);
        v.keyTiles = keyTiles(p, allSpecs);
        // Like /computer's modal: a spec already shown as a key-spec tile is not repeated in the
        // "<Type> specs" list. (Padding tiles such as Condition are not spec rows, so unaffected.)
        v.specs = new ArrayList<>(allSpecs);
        v.specs.removeAll(v.keyTiles);
        v.specsTitle = type == null ? "Legacy details" : type.getLabel() + " specs";
        v.details = List.of(
                new Spec("Brand", p.getBrand()),
                new Spec("Compatible models", p.getCompatibleModels()),
                new Spec("Part number", p.getPartNumber()),
                new Spec("Condition", p.getPartCondition() == null ? null : p.getPartCondition().getLabel()),
                new Spec("Warranty", p.getWarrantyDays() == null ? null : p.getWarrantyDays() + " days"));
        v.notes = nz(p.getNotes());
        v.added = p.getCreatedAt() == null ? "" : new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(p.getCreatedAt());
        return v;
    }

    // ---- Tile code ----------------------------------------------------------------------------

    /** Short code for the Part cell's tile when there is no photo. Never "?". */
    static String code(PartType type) {
        if (type == null) return "LAP";
        switch (type) {
            case LCD: return "LCD";
            case KEYBOARD: return "KEY";
            case BATTERY: return "BAT";
            case CHARGER: return "CHG";
            case RAM: return "RAM";
            case STORAGE: return "STO";
            case CASING: return "CAS";
            case HINGES: return "HNG";
            case DC_JACK: return "DC";
            case WIFI_CARD: return "WIFI";
            case TOUCHPAD: return "TPD";
            default: return "OTH";
        }
    }

    // ---- Key spec -----------------------------------------------------------------------------

    /**
     * The one-line summary in the table's Key spec column, e.g. LCD '15.6" FHD IPS, 30-pin eDP',
     * Battery '42 Wh, 11.4 V'. Blank parts are skipped; "" when nothing is set (the page then
     * shows "Not set" in grey).
     */
    public static String keySpec(LaptopParts p) {
        PartType type = p.getPartType();
        if (type == null) return "";
        switch (type) {
            case LCD:
                return join(", ",
                        join(" ", suffix(num(p.getLcdSizeInches()), "\""), resolutionShort(p.getLcdResolution()), p.getLcdPanelType()),
                        p.getLcdConnector());
            case BATTERY:
                return join(", ", unit(num(p.getBatteryCapacityWh()), "Wh"), unit(num(p.getBatteryVoltage()), "V"));
            case RAM:
                return join(", ", p.getRamType(), suffix(num(p.getRamCapacityGb()), "GB"), unit(num(p.getRamSpeedMts()), "MT/s"));
            case STORAGE:
                return join(", ",
                        join(" ", storageCapacity(p.getStorageCapacityGb(), ""), p.getStorageType()),
                        p.getStorageFormFactor());
            case CHARGER:
                return join(", ", unit(num(p.getChargerWattage()), "W"), p.getChargerConnectorTip());
            case KEYBOARD:
                return join(", ", p.getKbLayout(), Boolean.TRUE.equals(p.getKbBacklit()) ? "backlit" : null);
            case CASING:
                // The panel value already names itself ("A cover (lid)", "D bottom").
                return join(", ", p.getCasingPanel(), p.getCasingColor());
            case HINGES:
                return join(", ", p.getHingeSide());
            case DC_JACK:
                return join(", ", p.getDcJackType(), p.getDcJackTipSize());
            case WIFI_CARD:
                return join(", ", p.getWifiStandard(), p.getWifiFormFactor());
            case TOUCHPAD:
                return join(", ", Boolean.TRUE.equals(p.getTouchpadWithBracket()) ? "With bracket" : null,
                        p.getTouchpadConnector());
            default:
                return "";
        }
    }

    /** "FHD 1920x1080" -> "FHD", "HD+ 1600x900" -> "HD+". */
    static String resolutionShort(String resolution) {
        if (blank(resolution)) return null;
        String r = resolution.trim();
        int space = r.indexOf(' ');
        return space > 0 ? r.substring(0, space) : r;
    }

    // ---- Spec rows ----------------------------------------------------------------------------

    /**
     * Key-spec tiles picked by spec-row index, for the types where the first three rows are not
     * the three that matter most. Indexes refer to the order in specs(...) below.
     */
    private static final Map<PartType, int[]> TILE_ROWS = new EnumMap<>(PartType.class);
    static {
        TILE_ROWS.put(PartType.LCD, new int[] {0, 1, 3});          // Size, Resolution, Connector
        TILE_ROWS.put(PartType.CHARGER, new int[] {0, 1, 3});      // Wattage, Output voltage, Connector tip
        TILE_ROWS.put(PartType.KEYBOARD, new int[] {0, 1, 3});     // Layout, Backlit, With palmrest
    }

    /** Every spec field of the part's type, in form order, with units. Empty for Other. */
    static List<Spec> specs(LaptopParts p) {
        List<Spec> s = new ArrayList<>();
        PartType type = p.getPartType();
        if (type == null) {
            s.add(new Spec("Category", p.getCategory()));
            s.add(new Spec("Storage size", p.getStorageSize()));
            return s;
        }
        switch (type) {
            case LCD:
                s.add(new Spec("Size", unit(num(p.getLcdSizeInches()), "in")));
                s.add(new Spec("Resolution", p.getLcdResolution()));
                s.add(new Spec("Panel type", p.getLcdPanelType()));
                s.add(new Spec("Connector", p.getLcdConnector()));
                s.add(new Spec("Refresh rate", unit(num(p.getLcdRefreshRateHz()), "Hz")));
                s.add(new Spec("Surface", p.getLcdSurface()));
                s.add(new Spec("Touch", yesNo(p.getLcdTouch())));
                s.add(new Spec("Mounting", p.getLcdMounting()));
                break;
            case KEYBOARD:
                s.add(new Spec("Layout", p.getKbLayout()));
                s.add(new Spec("Backlit", yesNo(p.getKbBacklit())));
                s.add(new Spec("Color", p.getKbColor()));
                s.add(new Spec("With palmrest", yesNo(p.getKbWithPalmrest())));
                s.add(new Spec("With frame", yesNo(p.getKbWithFrame())));
                break;
            case BATTERY:
                s.add(new Spec("Capacity", unit(num(p.getBatteryCapacityWh()), "Wh")));
                s.add(new Spec("Voltage", unit(num(p.getBatteryVoltage()), "V")));
                s.add(new Spec("Cells", num(p.getBatteryCells())));
                s.add(new Spec("Chemistry", p.getBatteryChemistry()));
                break;
            case CHARGER:
                s.add(new Spec("Wattage", unit(num(p.getChargerWattage()), "W")));
                s.add(new Spec("Output voltage", unit(num(p.getChargerOutputVoltage()), "V")));
                s.add(new Spec("Current", unit(num(p.getChargerCurrentA()), "A")));
                s.add(new Spec("Connector tip", p.getChargerConnectorTip()));
                s.add(new Spec("Includes cord", yesNo(p.getChargerIncludesCord())));
                break;
            case RAM:
                s.add(new Spec("Type", p.getRamType()));
                s.add(new Spec("Capacity", unit(num(p.getRamCapacityGb()), "GB")));
                s.add(new Spec("Speed", unit(num(p.getRamSpeedMts()), "MT/s")));
                break;
            case STORAGE:
                s.add(new Spec("Type", p.getStorageType()));
                s.add(new Spec("Capacity", storageCapacity(p.getStorageCapacityGb(), " ")));
                s.add(new Spec("Form factor", p.getStorageFormFactor()));
                s.add(new Spec("Interface", p.getStorageInterface()));
                break;
            case CASING:
                s.add(new Spec("Panel", p.getCasingPanel()));
                s.add(new Spec("Color", p.getCasingColor()));
                break;
            case HINGES:
                s.add(new Spec("Side", p.getHingeSide()));
                break;
            case DC_JACK:
                s.add(new Spec("Jack type", p.getDcJackType()));
                s.add(new Spec("Tip size", p.getDcJackTipSize()));
                break;
            case WIFI_CARD:
                s.add(new Spec("Standard", p.getWifiStandard()));
                s.add(new Spec("Form factor", p.getWifiFormFactor()));
                s.add(new Spec("Bluetooth version", p.getWifiBluetoothVersion()));
                break;
            case TOUCHPAD:
                s.add(new Spec("Connector", p.getTouchpadConnector()));
                s.add(new Spec("With bracket", yesNo(p.getTouchpadWithBracket())));
                s.add(new Spec("Color", p.getTouchpadColor()));
                break;
            default:
                break;
        }
        return s;
    }

    /**
     * The three key-spec tiles. By default the type's first three spec rows (the spec lists above
     * are ordered most important first); TILE_ROWS overrides that where later rows matter more
     * for a replacement. A type with fewer than three specs - Casing, Hinges, DC Jack, Other - is
     * padded with Condition, Warranty and Part number, so there are always three tiles.
     * Unassigned rows get none.
     */
    static List<Spec> keyTiles(LaptopParts p, List<Spec> specs) {
        if (p.getPartType() == null) return List.of();
        List<Spec> tiles = new ArrayList<>();
        int[] rows = TILE_ROWS.get(p.getPartType());
        if (rows != null) {
            for (int row : rows) tiles.add(specs.get(row));
        } else {
            tiles.addAll(specs.subList(0, Math.min(3, specs.size())));
        }
        List<Spec> padding = List.of(
                new Spec("Condition", p.getPartCondition() == null ? null : p.getPartCondition().getLabel()),
                new Spec("Warranty", p.getWarrantyDays() == null ? null : p.getWarrantyDays() + " days"),
                new Spec("Part number", p.getPartNumber()));
        for (int i = 0; tiles.size() < 3 && i < padding.size(); i++) {
            tiles.add(padding.get(i));
        }
        return tiles;
    }

    // ---- Small formatting helpers ---------------------------------------------------------------

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** 42.0 -> "42", 11.40 -> "11.4", 3200 -> "3200"; null stays null. */
    static String num(Number n) {
        if (n == null) return null;
        return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
    }

    /**
     * Storage capacity is stored in GB (1 TB = 1000 GB, as drives are sold). Whole terabytes read
     * as TB, matching the form's options: 512 -> "512GB", 1000 -> "1TB", 2000 -> "2TB". sep is
     * the gap before the unit - "" for the compact key-spec line, " " for the spec rows.
     */
    static String storageCapacity(Integer gb, String sep) {
        if (gb == null) return null;
        if (gb >= 1000 && gb % 1000 == 0) return (gb / 1000) + sep + "TB";
        return gb + sep + "GB";
    }

    /** "42" + "Wh" -> "42 Wh". */
    private static String unit(String value, String unit) {
        return blank(value) ? null : value + " " + unit;
    }

    /** "8" + "GB" -> "8GB" (no space), for the compact key-spec line. */
    private static String suffix(String value, String suffix) {
        return blank(value) ? null : value.trim() + suffix;
    }

    private static String yesNo(Boolean b) {
        return b == null ? null : (b ? "Yes" : "No");
    }

    /** Joins the non-blank parts; "" when every part is blank. */
    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (blank(part)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(part.trim());
        }
        return sb.toString();
    }

    // ---- Getters (serialized into ALL_PARTS) ----------------------------------------------------

    public int getId() { return id; }
    public String getTypeKey() { return typeKey; }
    public String getTypeLabel() { return typeLabel; }
    public String getTypeCode() { return typeCode; }
    public int getTypeOrder() { return typeOrder; }
    public String getBrand() { return brand; }
    public String getPartName() { return partName; }
    public String getPartNumber() { return partNumber; }
    public String getCompatibleModels() { return compatibleModels; }
    public int getStocks() { return stocks; }
    public double getPrice() { return price; }
    public String getImageFileName() { return imageFileName; }
    public String getKeySpec() { return keySpec; }
    public List<Spec> getKeyTiles() { return keyTiles; }
    public List<Spec> getDetails() { return details; }
    public String getSpecsTitle() { return specsTitle; }
    public List<Spec> getSpecs() { return specs; }
    public String getNotes() { return notes; }
    public String getAdded() { return added; }
}
