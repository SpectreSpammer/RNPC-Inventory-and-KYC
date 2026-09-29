package com.rnpc.inventory.dto;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.util.PhoneNumbers;

/**
 * Display-ready view of one client for the /client list page - the counterpart of LaptopPartView.
 * Everything the page shows is decided here, in Java: the avatar initials, the contact number in
 * its canonical form, the created date and which job orders show as chips. The template only lays
 * values out.
 *
 * The page serializes a list of these into ALL_CLIENTS (th:inline), so every property is a plain
 * String/number or a list of Strings - no Date, no entity.
 *
 * Job orders are the client's repairs that Repair History shows (see RepairRecord.isVisible), the
 * same rule the repair list uses, so the chips never name a job order the admin cannot open. The
 * delete warning uses repairCount instead, which counts every repair record, because deleting a
 * client deletes the hidden ones as well.
 */
public class ClientView {

    /** Chips shown in the Job orders cell before "+N more"; keeps the cell inside its row. */
    public static final int MAX_CHIPS = 2;

    private Long id;
    private String fullName;
    private String initials;
    private String email;
    private String contactNumber;
    private String contactDigits;
    private String address;
    private String added;
    private long addedEpoch;
    private int jobOrderCount;
    private List<String> jobOrders;
    private int moreJobOrders;
    private String moreLabel;
    private String allJobOrders;
    private int repairCount;

    /**
     * @param repairs every repair record of this client (visible or not), newest first; null is
     *                treated as none
     */
    public static ClientView from(Client c, List<RepairRecord> repairs) {
        ClientView v = new ClientView();
        List<RepairRecord> all = repairs == null ? List.of() : repairs;
        List<String> visible = all.stream()
                .filter(RepairRecord::isVisible)
                .map(RepairRecord::getJobOrderNumber)
                .toList();

        v.id = c.getClientId();
        v.fullName = nz(c.getFullName());
        v.initials = initials(c.getFullName());
        v.email = nz(c.getEmail());
        // Shown in the canonical form even for a row saved before the format rule; searching uses
        // the digits key so 09991234567 finds 0999 123 4567 (and +63 spellings find it too).
        v.contactNumber = nz(PhoneNumbers.format(c.getContactNumber()));
        v.contactDigits = PhoneNumbers.matchKey(c.getContactNumber());
        v.address = nz(c.getAddress());
        v.added = c.getCreatedAt() == null ? "" : new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(c.getCreatedAt());
        v.addedEpoch = c.getCreatedAt() == null ? 0L : c.getCreatedAt().getTime();
        v.jobOrderCount = visible.size();
        v.jobOrders = visible.subList(0, Math.min(MAX_CHIPS, visible.size()));
        v.moreJobOrders = visible.size() - v.jobOrders.size();
        v.moreLabel = v.moreJobOrders > 0 ? "+" + v.moreJobOrders + " more" : "";
        v.allJobOrders = String.join(", ", visible);
        v.repairCount = all.size();
        return v;
    }

    /**
     * The avatar letters: the first letter of the first and of the last word, upper-cased - "Nand
     * Test" is NT, "robin" is R, "Juan dela Cruz" is JC. "?" when there is no name at all.
     */
    static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] words = fullName.trim().split("\\s+");
        String first = firstLetter(words[0]);
        String last = words.length > 1 ? firstLetter(words[words.length - 1]) : "";
        String out = first + last;
        return out.isEmpty() ? "?" : out.toUpperCase(Locale.ROOT);
    }

    /** The first letter or digit of a word, skipping leading punctuation; "" if it has none. */
    private static String firstLetter(String word) {
        for (int i = 0; i < word.length(); ) {
            int cp = word.codePointAt(i);
            if (Character.isLetterOrDigit(cp)) {
                return new String(Character.toChars(cp));
            }
            i += Character.charCount(cp);
        }
        return "";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ---- Getters (serialized into ALL_CLIENTS) --------------------------------------------------

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getInitials() { return initials; }
    public String getEmail() { return email; }
    public String getContactNumber() { return contactNumber; }
    public String getContactDigits() { return contactDigits; }
    public String getAddress() { return address; }
    public String getAdded() { return added; }
    public long getAddedEpoch() { return addedEpoch; }
    public int getJobOrderCount() { return jobOrderCount; }
    public List<String> getJobOrders() { return jobOrders; }
    public int getMoreJobOrders() { return moreJobOrders; }
    public String getMoreLabel() { return moreLabel; }
    public String getAllJobOrders() { return allJobOrders; }
    public int getRepairCount() { return repairCount; }
}
