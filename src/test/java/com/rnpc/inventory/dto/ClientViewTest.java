package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Display strings, avatar initials, contact number formatting and job-order chip capping in
 * ClientView. Pure objects - no Spring context, no database.
 */
class ClientViewTest {

    private static Client client(String name, String number) {
        Client c = new Client();
        c.setClientId(7L);
        c.setFullName(name);
        c.setContactNumber(number);
        return c;
    }

    private static RepairRecord repair(String jobOrder) {
        RepairRecord r = new RepairRecord();
        r.setJobOrderNumber(jobOrder);
        return r;
    }

    /** A record from an appointment that has not been confirmed: Repair History hides it. */
    private static RepairRecord hiddenRepair(String jobOrder) {
        Appointment a = new Appointment();
        a.setStatus(Appointment.Status.PENDING);
        RepairRecord r = repair(jobOrder);
        r.setAppointment(a);
        return r;
    }

    private static List<RepairRecord> repairs(String... jobOrders) {
        List<RepairRecord> out = new ArrayList<>();
        for (String j : jobOrders) {
            out.add(repair(j));
        }
        return out;
    }

    // ---- Initials -------------------------------------------------------------------------------

    @Test
    void initialsAreTheFirstLetterOfTheFirstAndLastWord() {
        assertEquals("NT", ClientView.initials("Nand Test"));
        assertEquals("NS", ClientView.initials("nami san"));
        assertEquals("JC", ClientView.initials("Juan dela Cruz"));
        assertEquals("MR", ClientView.initials("Mary-Ann Reyes"));
    }

    @Test
    void aSingleWordGivesOneLetterUpperCased() {
        assertEquals("R", ClientView.initials("robin"));
        assertEquals("R", ClientView.initials("  robin  "));
    }

    @Test
    void extraSpacesAndLeadingPunctuationAreSkipped() {
        assertEquals("NT", ClientView.initials("   Nand     Test  "));
        assertEquals("NT", ClientView.initials("(Nand) \"Test\""));
        // The first word is pure punctuation, so it contributes no letter; only "baker" does.
        assertEquals("B", ClientView.initials("((( baker"));
    }

    /**
     * Built from code points rather than a literal accented character in the source: this file's
     * javac invocation is not guaranteed to read non-ASCII source literals as UTF-8 (the project has
     * no explicit sourceEncoding), so a literal accent here silently corrupts under the wrong
     * platform default instead of failing loudly. Code points sidestep that entirely.
     */
    @Test
    void anAccentedInitialIsUpperCasedProperly() {
        String lowerA = codePointString(0x00E1);   // a with acute, lower case
        String upperA = codePointString(0x00C1);   // A with acute, upper case
        assertEquals(upperA, ClientView.initials(lowerA + "lvaro"));
    }

    private static String codePointString(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    @Test
    void aMissingOrBlankNameGivesAQuestionMark() {
        assertEquals("?", ClientView.initials(null));
        assertEquals("?", ClientView.initials(""));
        assertEquals("?", ClientView.initials("   "));
        assertEquals("?", ClientView.initials("---"));
    }

    // ---- Contact number -------------------------------------------------------------------------

    @Test
    void theContactNumberIsShownInItsCanonicalForm() {
        assertEquals("0917 234 5678", ClientView.from(client("A", "09172345678"), null).getContactNumber());
        assertEquals("0917 234 5678", ClientView.from(client("A", "+63 917-234-5678"), null).getContactNumber());
        assertEquals("0917 234 5678", ClientView.from(client("A", "0917 234 5678"), null).getContactNumber());
    }

    @Test
    void aLandlineKeepsWhatWasTyped() {
        assertEquals("(02) 8123 4567", ClientView.from(client("A", "(02)  8123 4567"), null).getContactNumber());
    }

    @Test
    void theDigitsKeyLetsAnySpellingBeFound() {
        ClientView v = ClientView.from(client("A", "0999 123 4567"), null);
        assertEquals("09991234567", v.getContactDigits());
        // A search for 09991234567 finds 0999 123 4567 because both reduce to the same key.
        assertTrue(v.getContactDigits().contains("09991234567"));

        assertEquals("09991234567", ClientView.from(client("A", "+639991234567"), null).getContactDigits());
    }

    // ---- Job order chips -----------------------------------------------------------------------

    @Test
    void noMoreThanTwoChipsThenPlusNMore() {
        assertEquals(2, ClientView.MAX_CHIPS);

        ClientView none = ClientView.from(client("A", "0917 234 5678"), repairs());
        assertEquals(List.of(), none.getJobOrders());
        assertEquals(0, none.getMoreJobOrders());
        assertEquals("", none.getMoreLabel());

        ClientView one = ClientView.from(client("A", "0917 234 5678"), repairs("JO-00001"));
        assertEquals(List.of("JO-00001"), one.getJobOrders());
        assertEquals("", one.getMoreLabel());

        ClientView two = ClientView.from(client("A", "0917 234 5678"), repairs("JO-00002", "JO-00001"));
        assertEquals(List.of("JO-00002", "JO-00001"), two.getJobOrders());
        assertEquals(0, two.getMoreJobOrders());
        assertEquals("", two.getMoreLabel());

        ClientView three = ClientView.from(client("A", "0917 234 5678"), repairs("JO-00006", "JO-00005", "JO-00004"));
        assertEquals(List.of("JO-00006", "JO-00005"), three.getJobOrders());
        assertEquals(1, three.getMoreJobOrders());
        assertEquals("+1 more", three.getMoreLabel());

        ClientView five = ClientView.from(client("A", "0917 234 5678"),
                repairs("JO-5", "JO-4", "JO-3", "JO-2", "JO-1"));
        assertEquals(List.of("JO-5", "JO-4"), five.getJobOrders());
        assertEquals(3, five.getMoreJobOrders());
        assertEquals("+3 more", five.getMoreLabel());
        assertEquals(5, five.getJobOrderCount());
    }

    @Test
    void everyJobOrderIsStillSearchableAndInTheTooltipEvenWhenCapped() {
        ClientView v = ClientView.from(client("A", "0917 234 5678"), repairs("JO-00006", "JO-00005", "JO-00004"));
        assertEquals("JO-00006, JO-00005, JO-00004", v.getAllJobOrders());
    }

    @Test
    void aRepairFromAnUnconfirmedAppointmentIsNotAChipButStillCountsForTheDeleteWarning() {
        List<RepairRecord> all = new ArrayList<>(repairs("JO-00003", "JO-00001"));
        all.add(1, hiddenRepair("JO-00002"));

        ClientView v = ClientView.from(client("A", "0917 234 5678"), all);
        assertEquals(List.of("JO-00003", "JO-00001"), v.getJobOrders());
        assertEquals(2, v.getJobOrderCount());
        // Deleting the client deletes the hidden record too, so the warning counts all three.
        assertEquals(3, v.getRepairCount());
        assertEquals("JO-00003, JO-00001", v.getAllJobOrders());
    }

    // ---- Empty and missing values --------------------------------------------------------------

    @Test
    void aClientWithNoRepairsHasNoChipsAndZeroCounts() {
        ClientView v = ClientView.from(client("nami san", "0920 999 1234"), null);
        assertEquals(List.of(), v.getJobOrders());
        assertEquals(0, v.getJobOrderCount());
        assertEquals(0, v.getRepairCount());
        assertEquals("", v.getAllJobOrders());
        assertEquals("", v.getMoreLabel());
        assertNotNull(v.getJobOrders());
    }

    @Test
    void missingFieldsBecomeEmptyStringsNeverNull() {
        Client c = new Client();
        c.setClientId(1L);
        ClientView v = ClientView.from(c, null);
        assertEquals("", v.getFullName());
        assertEquals("?", v.getInitials());
        assertEquals("", v.getEmail());
        assertEquals("", v.getContactNumber());
        assertEquals("", v.getContactDigits());
        assertEquals("", v.getAddress());
        assertEquals("", v.getAdded());
        assertEquals(0L, v.getAddedEpoch());
    }

    // ---- Date ----------------------------------------------------------------------------------

    @Test
    void theCreatedDateIsFormattedAndKeptAsAnEpochForSorting() {
        Client c = client("A", "0917 234 5678");
        Date created = new Date(1_789_000_000_000L);
        c.setCreatedAt(created);
        ClientView v = ClientView.from(c, null);

        assertEquals(created.getTime(), v.getAddedEpoch());
        // Derived from the same input rather than a year baked into the assertion, so this keeps
        // working whatever year the millis above happen to land on.
        String expected = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(created);
        assertEquals(expected, v.getAdded());
    }

    @Test
    void theIdAndEmailAndAddressPassThrough() {
        Client c = client("Nand Test", "0999 123 4567");
        c.setEmail("nand.test@example.com");
        c.setAddress("123 Test St");
        ClientView v = ClientView.from(c, null);
        assertEquals(7L, v.getId());
        assertEquals("nand.test@example.com", v.getEmail());
        assertEquals("123 Test St", v.getAddress());
        assertEquals("NT", v.getInitials());
    }
}
