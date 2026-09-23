package com.rnpc.inventory.dto;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.entity.CellphoneParts.PartType;

/**
 * Display-ready view of one cellphone part for the /cellphone list page and its View modal - the
 * cellphone counterpart of LaptopPartView. Deliberately duplicated rather than shared: the two
 * catalogs have different types and fields, and this codebase copies the parts-slice pattern per
 * category instead of generalising it.
 *
 * Everything the page shows is decided here, in Java: the type label and tile code, the key-spec
 * string, the three key-spec tiles, the Part details rows and the per-type spec rows with their
 * units. The template only lays them out.
 *
 * The page serializes a list of these into ALL_PARTS (th:inline), so every property is a plain
 * String/number/boolean or a list of Spec - no Date, no enum, nothing Jackson has to guess at.
 *
 * A row with a null part type should not exist; if one appears it is shown as Other (typeOf
 * below) rather than throwing, the same way LaptopPartView treats a stray laptop row. Every
 * cellphone row is like this today, since batch 1 added part_type without backfilling it.
 */
public class CellphonePartView {

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

    private Long id;
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

    public static CellphonePartView from(CellphoneParts p) {
        CellphonePartView v = new CellphonePartView();
        PartType type = typeOf(p);
        v.id = p.getCellphonePartId();
        // The slug, not type.name(): see LaptopPartView.from - the same identifier the edit page's
        // ?type= Cancel link and update redirect carry, so the list page's on-load restore matches
        // a chip against it directly.
        v.typeKey = type.getSlug();
        v.typeLabel = type.getLabel();
        v.typeCode = code(type);
        v.typeOrder = type.ordinal();
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
        // Like /computer's and /laptop's modals: a spec already shown as a key-spec tile is not
        // repeated in the "<Type> specs" list. (Padding tiles such as Condition are not spec rows,
        // so unaffected.)
        v.specs = new ArrayList<>(allSpecs);
        v.specs.removeAll(v.keyTiles);
        v.specsTitle = type.getLabel() + " specs";
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

    /** The part's type, with a stray null read as OTHER - never null, never throws. */
    static PartType typeOf(CellphoneParts p) {
        return p.getPartType() == null ? PartType.OTHER : p.getPartType();
    }

    // ---- Tile code ----------------------------------------------------------------------------

    /** Short code for the Part cell's tile when there is no photo. Never "?". */
    static String code(PartType type) {
        switch (type) {
            case SCREEN: return "SCR";
            case BATTERY: return "BAT";
            case CHARGING_BOARD: return "CHG";
            case BACK_GLASS: return "BAK";
            case HOUSING: return "HSG";
            case FLEX_CABLE: return "FLX";
            case CAMERA: return "CAM";
            case FINGERPRINT: return "FGR";
            case SENSOR: return "SNS";
            default: return "OTH";
        }
    }

    // ---- Key spec -----------------------------------------------------------------------------

    /**
     * The one-line summary in the table's Key spec column, e.g. Screen 'AMOLED 6.1", Service
     * pack', Battery '4000 mAh, 3.85 V'. Blank parts are skipped; "" when nothing is set (the page
     * then shows "Not set" in grey).
     */
    public static String keySpec(CellphoneParts p) {
        PartType type = typeOf(p);
        switch (type) {
            case SCREEN:
                return join(", ",
                        join(" ", p.getScreenPanelType(), suffix(num(p.getScreenSizeInches()), "\"")),
                        p.getScreenGrade());
            case BATTERY:
                return join(", ", unit(num(p.getBatteryCapacityMah()), "mAh"), unit(num(p.getBatteryVoltage()), "V"));
            case CHARGING_BOARD:
                return join(", ", p.getPortConnector(), Boolean.TRUE.equals(p.getPortOnFlex()) ? "on flex" : null);
            case BACK_GLASS:
                return join(", ", p.getCoverMaterial(), p.getCoverColor());
            case HOUSING:
                return join(", ", p.getHousingColor(), Boolean.TRUE.equals(p.getHousingWithButtons()) ? "with buttons" : null);
            case FLEX_CABLE:
                return join(", ", p.getFlexFunction());
            case CAMERA:
                return join(", ", p.getCameraPosition(), unit(num(p.getCameraMegapixels()), "MP"));
            case FINGERPRINT:
                return join(", ", p.getFingerprintPosition());
            case SENSOR:
                return join(", ", p.getSensorKind(), Boolean.TRUE.equals(p.getSensorUnderDisplay()) ? "under display" : null);
            default:
                return "";
        }
    }

    // ---- Spec rows ----------------------------------------------------------------------------

    /**
     * Every spec field of the part's type, in form order, with units. Empty for Other.
     *
     * Rows are ordered so the fields that matter most for a replacement come first - unlike
     * LaptopPartView, no type here needs a TILE_ROWS-style override to pick different rows for
     * its key-spec tiles.
     */
    static List<Spec> specs(CellphoneParts p) {
        List<Spec> s = new ArrayList<>();
        switch (typeOf(p)) {
            case SCREEN:
                s.add(new Spec("Panel type", p.getScreenPanelType()));
                s.add(new Spec("Size", unit(num(p.getScreenSizeInches()), "in")));
                s.add(new Spec("Grade", p.getScreenGrade()));
                s.add(new Spec("With frame", yesNo(p.getScreenWithFrame())));
                s.add(new Spec("Touch included", yesNo(p.getScreenTouchIncluded())));
                break;
            case BATTERY:
                s.add(new Spec("Capacity", unit(num(p.getBatteryCapacityMah()), "mAh")));
                s.add(new Spec("Voltage", unit(num(p.getBatteryVoltage()), "V")));
                s.add(new Spec("Chemistry", p.getBatteryChemistry()));
                break;
            case CHARGING_BOARD:
                s.add(new Spec("Connector", p.getPortConnector()));
                s.add(new Spec("On flex", yesNo(p.getPortOnFlex())));
                s.add(new Spec("With microphone", yesNo(p.getPortWithMic())));
                break;
            case BACK_GLASS:
                s.add(new Spec("Material", p.getCoverMaterial()));
                s.add(new Spec("Color", p.getCoverColor()));
                s.add(new Spec("With camera lens", yesNo(p.getCoverWithLens())));
                break;
            case HOUSING:
                s.add(new Spec("Color", p.getHousingColor()));
                s.add(new Spec("With buttons", yesNo(p.getHousingWithButtons())));
                s.add(new Spec("With back glass", yesNo(p.getHousingWithBackGlass())));
                break;
            case FLEX_CABLE:
                s.add(new Spec("Function", p.getFlexFunction()));
                break;
            case CAMERA:
                s.add(new Spec("Position", p.getCameraPosition()));
                s.add(new Spec("Resolution", unit(num(p.getCameraMegapixels()), "MP")));
                s.add(new Spec("Full module", yesNo(p.getCameraModule())));
                break;
            case FINGERPRINT:
                s.add(new Spec("Position", p.getFingerprintPosition()));
                s.add(new Spec("With flex cable", yesNo(p.getFingerprintWithFlex())));
                break;
            case SENSOR:
                s.add(new Spec("Kind", p.getSensorKind()));
                s.add(new Spec("Under display", yesNo(p.getSensorUnderDisplay())));
                break;
            default:
                break;
        }
        return s;
    }

    /**
     * The three key-spec tiles: the type's first three spec rows (the spec lists above are
     * ordered most important first). A type with fewer than three specs - Flex cable,
     * Fingerprint, Sensor, Other - is padded with Condition, Warranty and Part number, so there
     * are always three tiles.
     */
    static List<Spec> keyTiles(CellphoneParts p, List<Spec> specs) {
        List<Spec> tiles = new ArrayList<>(specs.subList(0, Math.min(3, specs.size())));
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

    public Long getId() { return id; }
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
