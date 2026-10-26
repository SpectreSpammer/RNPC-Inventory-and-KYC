package com.rnpc.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rnpc.inventory.controller.DashboardController;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Display-ready view of one repair for the CUSTOMER pages - the repair list at /repair and the
 * details modal - the counterpart of ClientView / LaptopPartView. Every string the customer sees is
 * decided here, in Java: the status label and plain-words sentence, the progress steps, the date,
 * the device label, and a placeholder for each field that is still empty. The templates only lay
 * values out, so nothing can print the word "null".
 *
 * Remarks are deliberately NOT a field here, and neither are the client's name or phone: this is
 * the only thing the customer templates read, so what is not on it cannot reach a customer.
 *
 * The list page serializes a list of these into ALL_REPAIRS (th:inline) to filter and sort in the
 * browser; the detail-only getters are @JsonIgnore'd so that list carries just what filtering needs.
 */
public class RepairView {

    /** One progress step: its label and whether the repair has reached it. */
    public static final class Step {
        private final String label;
        private final boolean done;

        Step(String label, boolean done) {
            this.label = label;
            this.done = done;
        }

        public String getLabel() { return label; }
        public boolean isDone() { return done; }
    }

    /** A detail value: the text to show, and whether it is a placeholder (shown muted italic). */
    public static final class Field {
        private final String text;
        private final boolean placeholder;

        Field(String text, boolean placeholder) {
            this.text = text;
            this.placeholder = placeholder;
        }

        public String getText() { return text; }
        public boolean isPlaceholder() { return placeholder; }
    }

    /** The four steps, in order. CANCELLED fills none of them. */
    static final List<String> STEP_LABELS = List.of("Received", "In progress", "Completed", "Released");

    private Long id;
    private String jobOrderNumber;
    private String deviceType;
    private String deviceLabel;
    private String issue;
    private String status;
    private RepairStatus statusEnum;
    private String statusLabel;
    private int statusRank;
    private String statusSentence;
    private int stepsFilled;
    private List<Step> steps;
    private String date;
    private String dateIso;
    private long dateEpoch;
    private double cost;
    private boolean costAssessed;
    private String costSuffix;
    private String costPlaceholder;
    private Field technician;
    private Field warranty;
    private Field fix;
    private Field recommendation;
    private String photoFile;
    private String searchText;

    public static RepairView from(RepairRecord r) {
        RepairView v = new RepairView();
        RepairStatus status = r.getStatus();

        v.id = r.getRepairId();
        v.jobOrderNumber = nz(r.getJobOrderNumber());
        v.deviceType = nz(r.getDeviceType());
        v.deviceLabel = r.getDeviceLabel();
        v.issue = nz(r.getIssueDescription());

        v.status = status == null ? "" : status.name();
        v.statusEnum = status;
        v.statusLabel = statusLabel(status);
        v.statusRank = status == null ? RepairStatus.values().length : status.ordinal();
        v.statusSentence = statusSentence(status);
        v.stepsFilled = stepsFilled(status);
        v.steps = steps(v.stepsFilled);

        v.date = r.getRepairDate() == null ? "Not set" : format("MMM d, yyyy", r.getRepairDate());
        v.dateIso = r.getRepairDate() == null ? "" : format("yyyy-MM-dd", r.getRepairDate());
        v.dateEpoch = r.getRepairDate() == null ? 0L : r.getRepairDate().getTime();

        // Nothing has set a cost on a fresh ticket or an appointment-sourced repair, so 0.0 means
        // "not assessed yet", not free - the same rule the slip uses (ticketPrint.html). On a
        // CANCELLED repair the cost is the diagnostic fee the shop kept, and 0 means no check-up.
        v.cost = r.getCost();
        v.costAssessed = hasAmount(r.getCost());
        v.costSuffix = costSuffix(status, r.getCost());
        v.costPlaceholder = costPlaceholder(status);

        v.technician = field(r.getTechnician(), "Not assigned yet");
        v.warranty = warranty(r);
        v.fix = field(r.getFix(), "Not recorded yet");
        v.recommendation = field(r.getRecommendation(), "None");

        v.photoFile = blank(r.getImageFileName()) ? "" : r.getImageFileName();

        v.searchText = (v.jobOrderNumber + " " + v.deviceLabel + " " + v.issue).toLowerCase(Locale.ROOT);
        return v;
    }

    // ---- The wording and the progress mapping ---------------------------------------------------

    /** Never the raw enum name. */
    static String statusLabel(RepairStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Pending";
            case IN_PROGRESS -> "In Progress";
            case COMPLETED -> "Completed";
            case RELEASED -> "Released";
            case CANCELLED -> "Cancelled";
        };
    }

    /** The plain-words sentence under the job order number in the details modal. */
    static String statusSentence(RepairStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "We've received your device. It's waiting to be looked at.";
            case IN_PROGRESS -> "A technician is working on your device.";
            case COMPLETED -> "The work is done. Your device is ready to pick up.";
            case RELEASED -> "Your device has been handed back to you.";
            case CANCELLED -> "This repair was cancelled.";
        };
    }

    /** How many of the four progress steps are filled: PENDING 1 ... RELEASED 4, CANCELLED 0. */
    static int stepsFilled(RepairStatus status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case PENDING -> 1;
            case IN_PROGRESS -> 2;
            case COMPLETED -> 3;
            case RELEASED -> 4;
            case CANCELLED -> 0;
        };
    }

    // ---- Cost text. One rule, shared by RepairView and AdminRepairView (nothing in it is private). ----

    /** Appended, muted, after the amount on a CANCELLED repair that carries a cost. */
    static final String DIAGNOSTIC_FEE_SUFFIX = "(diagnostic fee)";
    /** Shown in place of an amount on a CANCELLED repair with no cost: cancelled with no check-up. */
    static final String NO_COST_DASH = "\u2014";
    /** Shown in place of an amount on every other status while no cost has been set. */
    static final String TO_BE_ASSESSED = "To be assessed";

    /**
     * Whether there is an amount to show. Takes a boxed Double so that a cost that later becomes
     * nullable needs no change here: null reads as no amount.
     */
    static boolean hasAmount(Double cost) {
        return cost != null && cost > 0;
    }

    /**
     * "(diagnostic fee)" for a CANCELLED repair with an amount, otherwise empty. A cancelled repair's
     * cost is the diagnostic fee the shop kept (the fee is waived once the client goes ahead, and
     * Cost then holds only the repair price), so on CANCELLED it is not a repair price. The code only
     * reads the cost: no fee amount is stored or assumed anywhere.
     */
    static String costSuffix(RepairStatus status, Double cost) {
        return status == RepairStatus.CANCELLED && hasAmount(cost) ? DIAGNOSTIC_FEE_SUFFIX : "";
    }

    /** What stands in for the amount when there is none: a dash once cancelled, else "To be assessed". */
    static String costPlaceholder(RepairStatus status) {
        return status == RepairStatus.CANCELLED ? NO_COST_DASH : TO_BE_ASSESSED;
    }

    private static List<Step> steps(int filled) {
        Step[] out = new Step[STEP_LABELS.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Step(STEP_LABELS.get(i), i < filled);
        }
        return List.of(out);
    }

    /**
     * Days left, from the logic the customer dashboard's Warranty Tracker already uses
     * (DashboardController.warrantyDaysRemaining). "No warranty set" when the repair has no end
     * date, "Expired" once it has passed.
     */
    static Field warranty(RepairRecord r) {
        if (r.getWarrantyEndDate() == null) {
            return new Field("No warranty set", true);
        }
        long days = DashboardController.warrantyDaysRemaining(r);
        if (days < 0) {
            return new Field("Expired", false);
        }
        if (days == 0) {
            return new Field("Expires today", false);
        }
        return new Field(days + (days == 1 ? " day left" : " days left"), false);
    }

    // ---- Helpers ----------------------------------------------------------------------------------

    private static Field field(String value, String placeholder) {
        return blank(value) ? new Field(placeholder, true) : new Field(value.trim(), false);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String format(String pattern, java.util.Date date) {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(date);
    }

    // ---- Getters. The list-page ones are serialized into ALL_REPAIRS; the rest are @JsonIgnore'd.

    public Long getId() { return id; }
    public String getJobOrderNumber() { return jobOrderNumber; }
    public String getDeviceType() { return deviceType; }
    public String getDeviceLabel() { return deviceLabel; }
    public String getIssue() { return issue; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return statusLabel; }
    public int getStatusRank() { return statusRank; }
    public String getDate() { return date; }
    public String getDateIso() { return dateIso; }
    public long getDateEpoch() { return dateEpoch; }
    public double getCost() { return cost; }
    public boolean isCostAssessed() { return costAssessed; }
    public String getSearchText() { return searchText; }

    /** The enum itself, for the shared status-badge fragment (it switches on status.name()). */
    @JsonIgnore public RepairStatus getStatusEnum() { return statusEnum; }
    @JsonIgnore public String getStatusSentence() { return statusSentence; }
    /** "(diagnostic fee)" on a cancelled repair with an amount, else empty. */
    @JsonIgnore public String getCostSuffix() { return costSuffix; }
    /** What to show when there is no amount: "To be assessed", or a dash once cancelled. */
    @JsonIgnore public String getCostPlaceholder() { return costPlaceholder; }
    @JsonIgnore public int getStepsFilled() { return stepsFilled; }
    @JsonIgnore public List<Step> getSteps() { return steps; }
    @JsonIgnore public Field getTechnician() { return technician; }
    @JsonIgnore public Field getWarranty() { return warranty; }
    @JsonIgnore public Field getFix() { return fix; }
    @JsonIgnore public Field getRecommendation() { return recommendation; }
    @JsonIgnore public String getPhotoFile() { return photoFile; }
    @JsonIgnore public boolean isHasPhoto() { return !photoFile.isEmpty(); }
}
