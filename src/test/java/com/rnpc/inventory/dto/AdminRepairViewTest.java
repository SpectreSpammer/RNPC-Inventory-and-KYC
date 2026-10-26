package com.rnpc.inventory.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AdminRepairView is what the admin repair / ticket list and the admin details modal read (repairs
 * batch A1): the one view model allowed to carry the client, the contact number and the shop
 * remarks. These tests are the contract for its wording, its steps, its cost rule and the "never
 * print null" guarantee. No Spring context.
 */
class AdminRepairViewTest {

    private static Date date(String iso) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(iso);
    }

    private static Date inDays(int days) {
        return Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static RepairRecord repair(RepairStatus status) throws Exception {
        Client client = new Client();
        client.setClientId(3L);
        client.setFullName("Nand Test");
        client.setContactNumber("09991234567");
        RepairRecord r = new RepairRecord();
        r.setRepairId(14L);
        r.setJobOrderNumber("JO-00014");
        r.setClient(client);
        r.setDeviceType("Cellphone");
        r.setBrand("Asus");
        r.setModelName("dsadas");
        r.setIssueDescription("bsod");
        r.setRepairDate(date("2026-10-03"));
        r.setStatus(status);
        return r;
    }

    // ---- Labels, sentences and progress, on every status ------------------------------------------

    @Test
    void statusLabelsAreTheSharedReadableOnes() throws Exception {
        assertEquals("Pending", AdminRepairView.from(repair(RepairStatus.PENDING)).getStatusLabel());
        assertEquals("In Progress", AdminRepairView.from(repair(RepairStatus.IN_PROGRESS)).getStatusLabel());
        assertEquals("Completed", AdminRepairView.from(repair(RepairStatus.COMPLETED)).getStatusLabel());
        assertEquals("Released", AdminRepairView.from(repair(RepairStatus.RELEASED)).getStatusLabel());
        assertEquals("Cancelled", AdminRepairView.from(repair(RepairStatus.CANCELLED)).getStatusLabel());
    }

    @Test
    void everyStatusHasTheShopsOwnSentence() {
        assertEquals("Received and waiting for a technician.", AdminRepairView.statusSentence(RepairStatus.PENDING));
        assertEquals("A technician is working on this device.", AdminRepairView.statusSentence(RepairStatus.IN_PROGRESS));
        assertEquals("The work is done. Ready to hand back to the client.", AdminRepairView.statusSentence(RepairStatus.COMPLETED));
        assertEquals("Handed back to the client.", AdminRepairView.statusSentence(RepairStatus.RELEASED));
        assertEquals("This repair was cancelled.", AdminRepairView.statusSentence(RepairStatus.CANCELLED));
        assertEquals("", AdminRepairView.statusSentence(null));
    }

    @Test
    void theSentencesAreNotTheCustomersWording() {
        for (RepairStatus status : RepairStatus.values()) {
            if (status == RepairStatus.CANCELLED) {
                continue; // the one sentence both audiences read the same
            }
            assertFalse(AdminRepairView.statusSentence(status).equals(RepairView.statusSentence(status)), status.name());
        }
    }

    @Test
    void progressFillsOneToFourStepsPerStatusAndCancelledNone() throws Exception {
        assertEquals(List.of(true, false, false, false), doneFlags(RepairStatus.PENDING));
        assertEquals(List.of(true, true, false, false), doneFlags(RepairStatus.IN_PROGRESS));
        assertEquals(List.of(true, true, true, false), doneFlags(RepairStatus.COMPLETED));
        assertEquals(List.of(true, true, true, true), doneFlags(RepairStatus.RELEASED));
        assertEquals(List.of(false, false, false, false), doneFlags(RepairStatus.CANCELLED));
    }

    private static List<Boolean> doneFlags(RepairStatus status) throws Exception {
        return AdminRepairView.from(repair(status)).getSteps().stream()
                .map(RepairView.Step::isDone).collect(Collectors.toList());
    }

    @Test
    void theFourStepsAreLabelledInOrder() throws Exception {
        assertEquals(List.of("Received", "In progress", "Completed", "Released"),
                AdminRepairView.from(repair(RepairStatus.PENDING)).getSteps().stream()
                        .map(RepairView.Step::getLabel).collect(Collectors.toList()));
    }

    @Test
    void statusRankFollowsTheEnumOrder() throws Exception {
        assertEquals(0, AdminRepairView.from(repair(RepairStatus.PENDING)).getStatusRank());
        assertEquals(4, AdminRepairView.from(repair(RepairStatus.CANCELLED)).getStatusRank());
    }

    // ---- Client and contact -----------------------------------------------------------------------

    @Test
    void theClientAndContactAreCarriedWithTheContactFormatted() throws Exception {
        AdminRepairView v = AdminRepairView.from(repair(RepairStatus.PENDING));

        assertEquals(3L, v.getClientId());
        assertEquals("Nand Test", v.getClientName());
        assertEquals("0999 123 4567", v.getContact().getText());
        assertFalse(v.getContact().isPlaceholder());
    }

    @Test
    void aMissingClientNameOrContactGetsAPlaceholderNotNull() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.getClient().setFullName("  ");
        r.getClient().setContactNumber(null);
        AdminRepairView v = AdminRepairView.from(r);

        assertEquals("Unknown client", v.getClientName());
        assertEquals("Not provided", v.getContact().getText());
        assertTrue(v.getContact().isPlaceholder());
    }

    @Test
    void aRecordWithNoClientAtAllStillBuilds() {
        AdminRepairView v = AdminRepairView.from(new RepairRecord());

        assertEquals("Unknown client", v.getClientName());
        assertEquals("Not provided", v.getContact().getText());
        assertEquals(null, v.getClientId());
    }

    // ---- Date, device, cost -----------------------------------------------------------------------

    @Test
    void theDateIsPaddedLikeTheAdminDesign() throws Exception {
        AdminRepairView v = AdminRepairView.from(repair(RepairStatus.PENDING));

        assertEquals("Oct 03, 2026", v.getDate());
        assertEquals("2026-10-03", v.getDateIso());
        assertEquals(date("2026-10-03").getTime(), v.getDateEpoch());
    }

    @Test
    void aMissingDateShowsNotSet() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setRepairDate(null);

        assertEquals("Not set", AdminRepairView.from(r).getDate());
        assertEquals(0L, AdminRepairView.from(r).getDateEpoch());
    }

    @Test
    void theDeviceIsTheSharedLabelAndSurvivesNullBrandAndModel() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        assertEquals("Cellphone (Asus dsadas)", AdminRepairView.from(r).getDeviceLabel());

        r.setBrand(null);
        r.setModelName(null);
        AdminRepairView v = AdminRepairView.from(r);
        assertEquals("Cellphone", v.getDeviceLabel());
        assertFalse(v.getSearchText().contains("null"));
    }

    @Test
    void costIsAssessedOnlyAboveZero() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setCost(2000.0);
        assertTrue(AdminRepairView.from(r).isCostAssessed());
        assertEquals(2000.0, AdminRepairView.from(r).getCost());

        r.setCost(0.0);
        assertFalse(AdminRepairView.from(r).isCostAssessed());
    }

    // ---- Cost text on a cancelled repair ----------------------------------------------------------

    private static AdminRepairView withCost(RepairStatus status, double cost) throws Exception {
        RepairRecord r = repair(status);
        r.setCost(cost);
        return AdminRepairView.from(r);
    }

    @Test
    void cancelledWithACostShowsTheAmountAndTheDiagnosticFeeSuffix() throws Exception {
        AdminRepairView v = withCost(RepairStatus.CANCELLED, 1500.0);

        assertTrue(v.isCostAssessed());
        assertEquals(1500.0, v.getCost());
        assertEquals("(diagnostic fee)", v.getCostSuffix());
    }

    @Test
    void cancelledWithNoCostShowsADashAndNoSuffix() throws Exception {
        AdminRepairView v = withCost(RepairStatus.CANCELLED, 0.0);

        assertFalse(v.isCostAssessed());
        assertEquals("—", v.getCostPlaceholder());
        assertEquals("", v.getCostSuffix());
    }

    @Test
    void pendingAndInProgressWithNoCostStillSayToBeAssessed() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.PENDING, RepairStatus.IN_PROGRESS)) {
            AdminRepairView v = withCost(status, 0.0);

            assertFalse(v.isCostAssessed(), status.name());
            assertEquals("To be assessed", v.getCostPlaceholder(), status.name());
            assertEquals("", v.getCostSuffix(), status.name());
        }
    }

    @Test
    void completedAndReleasedShowThePlainAmountWithNoSuffix() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.COMPLETED, RepairStatus.RELEASED)) {
            AdminRepairView v = withCost(status, 2000.0);

            assertTrue(v.isCostAssessed(), status.name());
            assertEquals("", v.getCostSuffix(), status.name());
        }
    }

    @Test
    void completedAndReleasedOldZeroRowsKeepToBeAssessed() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.COMPLETED, RepairStatus.RELEASED)) {
            AdminRepairView v = withCost(status, 0.0);

            assertFalse(v.isCostAssessed(), status.name());
            assertEquals("To be assessed", v.getCostPlaceholder(), status.name());
        }
    }

    @Test
    void theAdminViewFollowsTheSameRuleAsTheCustomerView() throws Exception {
        for (RepairStatus status : RepairStatus.values()) {
            for (double cost : new double[]{0.0, 1500.0}) {
                RepairRecord r = repair(status);
                r.setCost(cost);
                RepairView customer = RepairView.from(r);
                AdminRepairView admin = AdminRepairView.from(r);

                assertEquals(customer.isCostAssessed(), admin.isCostAssessed(), status + " " + cost);
                assertEquals(customer.getCostSuffix(), admin.getCostSuffix(), status + " " + cost);
                assertEquals(customer.getCostPlaceholder(), admin.getCostPlaceholder(), status + " " + cost);
            }
        }
    }

    // ---- Detail fields and placeholders -----------------------------------------------------------

    @Test
    void emptyDetailFieldsGetTheirPlaceholders() throws Exception {
        AdminRepairView v = AdminRepairView.from(repair(RepairStatus.PENDING));

        assertEquals("Not assigned yet", v.getTechnician().getText());
        assertEquals("No warranty set", v.getWarranty().getText());
        assertEquals("Not recorded yet", v.getFix().getText());
        assertEquals("None", v.getRecommendation().getText());
        assertEquals("No remarks", v.getRemarks().getText());
        assertTrue(v.getTechnician().isPlaceholder() && v.getWarranty().isPlaceholder()
                && v.getFix().isPlaceholder() && v.getRecommendation().isPlaceholder()
                && v.getRemarks().isPlaceholder());
    }

    @Test
    void blankDetailFieldsCountAsEmpty() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setTechnician("  ");
        r.setFix("");
        r.setRecommendation(" ");
        r.setRemarks("   ");
        AdminRepairView v = AdminRepairView.from(r);

        assertTrue(v.getTechnician().isPlaceholder());
        assertTrue(v.getFix().isPlaceholder());
        assertTrue(v.getRecommendation().isPlaceholder());
        assertTrue(v.getRemarks().isPlaceholder());
    }

    @Test
    void filledDetailFieldsIncludingRemarksAreShownAsEntered() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setTechnician("Juan");
        r.setFix("Replaced the display cable");
        r.setRecommendation("Back up your files");
        r.setRemarks("Customer will pick up Friday");
        AdminRepairView v = AdminRepairView.from(r);

        assertEquals("Juan", v.getTechnician().getText());
        assertEquals("Replaced the display cable", v.getFix().getText());
        assertEquals("Back up your files", v.getRecommendation().getText());
        assertEquals("Customer will pick up Friday", v.getRemarks().getText());
        assertFalse(v.getRemarks().isPlaceholder());
    }

    // ---- Warranty (same wording as the customer's) ------------------------------------------------

    @Test
    void warrantyReadsDaysLeftTodayExpiredOrNoneSet() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        assertEquals("No warranty set", AdminRepairView.from(r).getWarranty().getText());

        r.setWarrantyEndDate(inDays(30));
        assertEquals("30 days left", AdminRepairView.from(r).getWarranty().getText());
        r.setWarrantyEndDate(inDays(1));
        assertEquals("1 day left", AdminRepairView.from(r).getWarranty().getText());
        r.setWarrantyEndDate(inDays(0));
        assertEquals("Expires today", AdminRepairView.from(r).getWarranty().getText());
        r.setWarrantyEndDate(inDays(-3));
        assertEquals("Expired", AdminRepairView.from(r).getWarranty().getText());
    }

    // ---- Photo, search and what the serialized list carries ---------------------------------------

    @Test
    void thePhotoIsPresentOnlyWithAFileName() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        assertFalse(AdminRepairView.from(r).isHasPhoto());
        r.setImageFileName("  ");
        assertFalse(AdminRepairView.from(r).isHasPhoto());
        r.setImageFileName("1_fixed.png");
        assertTrue(AdminRepairView.from(r).isHasPhoto());
    }

    @Test
    void searchTextCoversJobOrderClientDeviceAndIssueInLowerCase() throws Exception {
        String text = AdminRepairView.from(repair(RepairStatus.PENDING)).getSearchText();

        assertTrue(text.contains("jo-00014"));
        assertTrue(text.contains("nand test"));
        assertTrue(text.contains("asus dsadas"));
        assertTrue(text.contains("bsod"));
        assertEquals(text, text.toLowerCase());
    }

    @Test
    void theSerializedListKeepsRemarksAndContactOutOfThePageSource() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setRemarks("INTERNAL-NOTE-XYZ");
        r.setFix("secret fix text");
        String json = new ObjectMapper().writeValueAsString(AdminRepairView.from(r));

        assertTrue(json.contains("\"clientName\":\"Nand Test\""), json);
        assertTrue(json.contains("\"clientSortKey\""), json);
        assertFalse(json.contains("INTERNAL-NOTE-XYZ"), json);
        assertFalse(json.contains("secret fix text"), json);
        assertFalse(json.contains("0999"), json);
        assertFalse(json.contains("\"steps\""), json);
    }

    @Test
    void aBareRecordProducesNoNullText() {
        AdminRepairView v = AdminRepairView.from(new RepairRecord());

        assertEquals("", v.getJobOrderNumber());
        assertEquals("", v.getDeviceLabel());
        assertEquals("", v.getIssue());
        assertEquals("", v.getStatus());
        assertEquals("Not set", v.getDate());
        assertFalse(v.getSearchText().contains("null"));
        assertFalse(v.getRemarks().getText().contains("null"));
        assertFalse(v.getWarranty().getText().contains("null"));
    }
}
