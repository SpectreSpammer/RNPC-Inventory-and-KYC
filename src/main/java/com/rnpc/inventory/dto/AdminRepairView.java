package com.rnpc.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import com.rnpc.inventory.util.PhoneNumbers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Display-ready view of one repair for the ADMIN pages - the repair / ticket list at /repair and
 * /ticket/view and the admin details modal (repairs batch A1). The counterpart of {@link RepairView},
 * which is the CUSTOMER's and deliberately has no remarks, client name or phone: this class is the
 * only place those are allowed, so customer data can never be made to carry them by accident.
 * Never hand an AdminRepairView to a customer template.
 *
 * What it shares with RepairView is only what is identical for both audiences and holds no private
 * data: the status labels, the four progress-step labels and the step count per status, and the
 * warranty wording. Everything audience-specific is its own - the plain-words sentences ("Received
 * and waiting for a technician." where the customer reads "We've received your device..."), the
 * date format (the admin design pads the day: "Oct 03, 2026"), and the client and remarks.
 *
 * The list page serializes a list of these into ALL_REPAIRS (th:inline) to filter and sort in the
 * browser, so the detail-only getters - and the contact number and remarks above all - are
 * {@code @JsonIgnore}d and never reach that script block.
 */
public class AdminRepairView {

    private Long id;
    private String jobOrderNumber;
    private Long clientId;
    private String clientName;
    private String clientSortKey;
    private RepairView.Field contact;
    private String deviceType;
    private String deviceLabel;
    private String issue;
    private String status;
    private RepairStatus statusEnum;
    private String statusLabel;
    private int statusRank;
    private String statusSentence;
    private int stepsFilled;
    private List<RepairView.Step> steps;
    private String date;
    private String dateIso;
    private long dateEpoch;
    private double cost;
    private boolean costAssessed;
    private String costSuffix;
    private String costPlaceholder;
    private RepairView.Field technician;
    private RepairView.Field warranty;
    private RepairView.Field fix;
    private RepairView.Field recommendation;
    private RepairView.Field remarks;
    private String photoFile;
    private String searchText;

    public static AdminRepairView from(RepairRecord r) {
        AdminRepairView v = new AdminRepairView();
        RepairStatus status = r.getStatus();
        Client client = r.getClient();

        v.id = r.getRepairId();
        v.jobOrderNumber = nz(r.getJobOrderNumber());
        v.clientId = client == null ? null : client.getClientId();
        v.clientName = client == null || blank(client.getFullName()) ? "Unknown client" : client.getFullName().trim();
        v.clientSortKey = v.clientName.toLowerCase(Locale.ROOT);
        String contact = client == null ? null : PhoneNumbers.format(client.getContactNumber());
        v.contact = blank(contact) ? new RepairView.Field("Not provided", true) : new RepairView.Field(contact, false);
        v.deviceType = nz(r.getDeviceType());
        v.deviceLabel = r.getDeviceLabel();
        v.issue = nz(r.getIssueDescription());

        v.status = status == null ? "" : status.name();
        v.statusEnum = status;
        v.statusLabel = RepairView.statusLabel(status);
        v.statusRank = status == null ? RepairStatus.values().length : status.ordinal();
        v.statusSentence = statusSentence(status);
        v.stepsFilled = RepairView.stepsFilled(status);
        v.steps = steps(v.stepsFilled);

        v.date = r.getRepairDate() == null ? "Not set" : format("MMM dd, yyyy", r.getRepairDate());
        v.dateIso = r.getRepairDate() == null ? "" : format("yyyy-MM-dd", r.getRepairDate());
        v.dateEpoch = r.getRepairDate() == null ? 0L : r.getRepairDate().getTime();

        // 0.0 means "not assessed yet", not free - nothing sets a cost on a fresh ticket. On a
        // CANCELLED repair the cost is the diagnostic fee the shop kept - the rule is RepairView's.
        v.cost = r.getCost();
        v.costAssessed = RepairView.hasAmount(r.getCost());
        v.costSuffix = RepairView.costSuffix(status, r.getCost());
        v.costPlaceholder = RepairView.costPlaceholder(status);

        v.technician = field(r.getTechnician(), "Not assigned yet");
        v.warranty = RepairView.warranty(r);
        v.fix = field(r.getFix(), "Not recorded yet");
        v.recommendation = field(r.getRecommendation(), "None");
        v.remarks = field(r.getRemarks(), "No remarks");

        v.photoFile = blank(r.getImageFileName()) ? "" : r.getImageFileName();

        v.searchText = (v.jobOrderNumber + " " + v.clientName + " " + v.deviceLabel + " " + v.issue)
                .toLowerCase(Locale.ROOT);
        return v;
    }

    /** The plain-words sentence under the job order number, worded for the shop rather than the client. */
    static String statusSentence(RepairStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Received and waiting for a technician.";
            case IN_PROGRESS -> "A technician is working on this device.";
            case COMPLETED -> "The work is done. Ready to hand back to the client.";
            case RELEASED -> "Handed back to the client.";
            case CANCELLED -> "This repair was cancelled.";
        };
    }

    private static List<RepairView.Step> steps(int filled) {
        List<RepairView.Step> out = new ArrayList<>();
        for (int i = 0; i < RepairView.STEP_LABELS.size(); i++) {
            out.add(new RepairView.Step(RepairView.STEP_LABELS.get(i), i < filled));
        }
        return List.copyOf(out);
    }

    private static RepairView.Field field(String value, String placeholder) {
        return blank(value) ? new RepairView.Field(placeholder, true) : new RepairView.Field(value.trim(), false);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String format(String pattern, Date date) {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(date);
    }

    // ---- Getters. The list-page ones are serialized into ALL_REPAIRS; the rest are @JsonIgnore'd.

    public Long getId() { return id; }
    public String getJobOrderNumber() { return jobOrderNumber; }
    public Long getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getClientSortKey() { return clientSortKey; }
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
    @JsonIgnore public RepairView.Field getContact() { return contact; }
    @JsonIgnore public String getStatusSentence() { return statusSentence; }
    @JsonIgnore public String getCostSuffix() { return costSuffix; }
    @JsonIgnore public String getCostPlaceholder() { return costPlaceholder; }
    @JsonIgnore public int getStepsFilled() { return stepsFilled; }
    @JsonIgnore public List<RepairView.Step> getSteps() { return steps; }
    @JsonIgnore public RepairView.Field getTechnician() { return technician; }
    @JsonIgnore public RepairView.Field getWarranty() { return warranty; }
    @JsonIgnore public RepairView.Field getFix() { return fix; }
    @JsonIgnore public RepairView.Field getRecommendation() { return recommendation; }
    @JsonIgnore public RepairView.Field getRemarks() { return remarks; }
    @JsonIgnore public String getPhotoFile() { return photoFile; }
    @JsonIgnore public boolean isHasPhoto() { return !photoFile.isEmpty(); }
}
