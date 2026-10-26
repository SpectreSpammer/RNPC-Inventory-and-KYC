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
 * RepairView is what the customer repair pages read (repairs batch U1): every display string is
 * decided here, so these tests are the contract for the wording, the progress steps, the cost rule
 * and the "never print null" guarantee. No Spring context.
 */
class RepairViewTest {

    private static Date date(String iso) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(iso);
    }

    private static Date inDays(int days) {
        return Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static RepairRecord repair(RepairStatus status) throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        client.setContactNumber("0999 123 4567");
        RepairRecord r = new RepairRecord();
        r.setRepairId(7L);
        r.setJobOrderNumber("JO-00007");
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setBrand("Acer");
        r.setModelName("Aspire 5");
        r.setIssueDescription("No display");
        r.setRepairDate(date("2026-09-18"));
        r.setStatus(status);
        return r;
    }

    // ---- Status wording, sentence and progress ----------------------------------------------------

    @Test
    void statusLabelsAreReadableAndNeverTheRawEnumName() {
        assertEquals("Pending", RepairView.statusLabel(RepairStatus.PENDING));
        assertEquals("In Progress", RepairView.statusLabel(RepairStatus.IN_PROGRESS));
        assertEquals("Completed", RepairView.statusLabel(RepairStatus.COMPLETED));
        assertEquals("Released", RepairView.statusLabel(RepairStatus.RELEASED));
        assertEquals("Cancelled", RepairView.statusLabel(RepairStatus.CANCELLED));
        assertEquals("", RepairView.statusLabel(null));
    }

    @Test
    void everyStatusHasItsPlainWordsSentence() {
        assertEquals("We've received your device. It's waiting to be looked at.",
                RepairView.statusSentence(RepairStatus.PENDING));
        assertEquals("A technician is working on your device.", RepairView.statusSentence(RepairStatus.IN_PROGRESS));
        assertEquals("The work is done. Your device is ready to pick up.", RepairView.statusSentence(RepairStatus.COMPLETED));
        assertEquals("Your device has been handed back to you.", RepairView.statusSentence(RepairStatus.RELEASED));
        assertEquals("This repair was cancelled.", RepairView.statusSentence(RepairStatus.CANCELLED));
        assertEquals("", RepairView.statusSentence(null));
    }

    @Test
    void progressFillsOneToFourStepsAndCancelledFillsNone() {
        assertEquals(1, RepairView.stepsFilled(RepairStatus.PENDING));
        assertEquals(2, RepairView.stepsFilled(RepairStatus.IN_PROGRESS));
        assertEquals(3, RepairView.stepsFilled(RepairStatus.COMPLETED));
        assertEquals(4, RepairView.stepsFilled(RepairStatus.RELEASED));
        assertEquals(0, RepairView.stepsFilled(RepairStatus.CANCELLED));
        assertEquals(0, RepairView.stepsFilled(null));
    }

    @Test
    void theFourStepsAreLabelledInOrderAndMarkedDoneFromTheLeft() throws Exception {
        List<RepairView.Step> steps = RepairView.from(repair(RepairStatus.COMPLETED)).getSteps();

        assertEquals(List.of("Received", "In progress", "Completed", "Released"),
                steps.stream().map(RepairView.Step::getLabel).collect(Collectors.toList()));
        assertEquals(List.of(true, true, true, false),
                steps.stream().map(RepairView.Step::isDone).collect(Collectors.toList()));
    }

    @Test
    void aCancelledRepairHasFourStepsNoneOfThemDone() throws Exception {
        List<RepairView.Step> steps = RepairView.from(repair(RepairStatus.CANCELLED)).getSteps();

        assertEquals(4, steps.size());
        assertTrue(steps.stream().noneMatch(RepairView.Step::isDone));
    }

    @Test
    void statusRankFollowsTheEnumOrderForSortByStatus() throws Exception {
        assertEquals(0, RepairView.from(repair(RepairStatus.PENDING)).getStatusRank());
        assertEquals(1, RepairView.from(repair(RepairStatus.IN_PROGRESS)).getStatusRank());
        assertEquals(4, RepairView.from(repair(RepairStatus.CANCELLED)).getStatusRank());
    }

    // ---- Date, device and issue -------------------------------------------------------------------

    @Test
    void theDateReadsLikeSep18AndKeepsAnIsoFormForFiltering() throws Exception {
        RepairView v = RepairView.from(repair(RepairStatus.PENDING));

        assertEquals("Sep 18, 2026", v.getDate());
        assertEquals("2026-09-18", v.getDateIso());
        assertEquals(date("2026-09-18").getTime(), v.getDateEpoch());
    }

    @Test
    void aMissingDateShowsNotSetAndSortsAsZero() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setRepairDate(null);
        RepairView v = RepairView.from(r);

        assertEquals("Not set", v.getDate());
        assertEquals("", v.getDateIso());
        assertEquals(0L, v.getDateEpoch());
    }

    @Test
    void theDeviceIsTheSharedLabel() throws Exception {
        assertEquals("Laptop (Acer Aspire 5)", RepairView.from(repair(RepairStatus.PENDING)).getDeviceLabel());
    }

    @Test
    void aMissingBrandAndModelLeaveJustTheDeviceTypeWithoutTheWordNull() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setBrand(null);
        r.setModelName(null);
        RepairView v = RepairView.from(r);

        assertEquals("Laptop", v.getDeviceLabel());
        assertFalse(v.getSearchText().contains("null"));
    }

    @Test
    void aBlankBrandIsSkippedWithoutADanglingGap() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setBrand("  ");

        assertEquals("Laptop (Aspire 5)", RepairView.from(r).getDeviceLabel());
    }

    // ---- Cost -------------------------------------------------------------------------------------

    @Test
    void aPositiveCostIsAssessed() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setCost(2000.0);
        RepairView v = RepairView.from(r);

        assertTrue(v.isCostAssessed());
        assertEquals(2000.0, v.getCost());
    }

    @Test
    void aZeroCostIsNotAssessedSoThePageSaysToBeAssessedInsteadOfFree() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setCost(0.0);

        assertFalse(RepairView.from(r).isCostAssessed());
    }

    // ---- Cost text on a cancelled repair ----------------------------------------------------------

    private static RepairView withCost(RepairStatus status, double cost) throws Exception {
        RepairRecord r = repair(status);
        r.setCost(cost);
        return RepairView.from(r);
    }

    @Test
    void cancelledWithACostShowsTheAmountAndTheDiagnosticFeeSuffix() throws Exception {
        RepairView v = withCost(RepairStatus.CANCELLED, 1500.0);

        assertTrue(v.isCostAssessed());
        assertEquals(1500.0, v.getCost());
        assertEquals("(diagnostic fee)", v.getCostSuffix());
    }

    @Test
    void cancelledWithNoCostShowsADashAndNoSuffix() throws Exception {
        RepairView v = withCost(RepairStatus.CANCELLED, 0.0);

        assertFalse(v.isCostAssessed());
        assertEquals("—", v.getCostPlaceholder());
        assertEquals("", v.getCostSuffix());
    }

    @Test
    void pendingAndInProgressWithNoCostStillSayToBeAssessed() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.PENDING, RepairStatus.IN_PROGRESS)) {
            RepairView v = withCost(status, 0.0);

            assertFalse(v.isCostAssessed(), status.name());
            assertEquals("To be assessed", v.getCostPlaceholder(), status.name());
            assertEquals("", v.getCostSuffix(), status.name());
        }
    }

    @Test
    void completedAndReleasedShowThePlainAmountWithNoSuffix() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.COMPLETED, RepairStatus.RELEASED)) {
            RepairView v = withCost(status, 2000.0);

            assertTrue(v.isCostAssessed(), status.name());
            assertEquals("", v.getCostSuffix(), status.name());
        }
    }

    @Test
    void completedAndReleasedOldZeroRowsKeepToBeAssessed() throws Exception {
        for (RepairStatus status : List.of(RepairStatus.COMPLETED, RepairStatus.RELEASED)) {
            RepairView v = withCost(status, 0.0);

            assertFalse(v.isCostAssessed(), status.name());
            assertEquals("To be assessed", v.getCostPlaceholder(), status.name());
        }
    }

    @Test
    void aCostOnAnUnfinishedRepairNeverGetsTheFeeSuffix() throws Exception {
        assertEquals("", withCost(RepairStatus.PENDING, 500.0).getCostSuffix());
        assertEquals("", withCost(RepairStatus.IN_PROGRESS, 500.0).getCostSuffix());
    }

    @Test
    void aNullCostOnACancelledRepairWouldShowTheDash() {
        // cost is a primitive today; the helpers take a boxed Double so a nullable cost needs no change
        assertFalse(RepairView.hasAmount(null));
        assertEquals("", RepairView.costSuffix(RepairStatus.CANCELLED, null));
        assertEquals("—", RepairView.costPlaceholder(RepairStatus.CANCELLED));
        assertEquals("To be assessed", RepairView.costPlaceholder(RepairStatus.PENDING));
    }

    @Test
    void theCostTextIsNotSerializedIntoTheListPage() throws Exception {
        String json = new ObjectMapper().writeValueAsString(withCost(RepairStatus.CANCELLED, 1500.0));

        assertFalse(json.contains("diagnostic fee"), json);
        assertFalse(json.contains("costSuffix"), json);
    }

    // ---- The detail fields and their placeholders -------------------------------------------------

    @Test
    void emptyDetailFieldsGetTheirPlaceholdersFlaggedAsSuch() throws Exception {
        RepairView v = RepairView.from(repair(RepairStatus.PENDING));

        assertEquals("Not assigned yet", v.getTechnician().getText());
        assertTrue(v.getTechnician().isPlaceholder());
        assertEquals("No warranty set", v.getWarranty().getText());
        assertTrue(v.getWarranty().isPlaceholder());
        assertEquals("Not recorded yet", v.getFix().getText());
        assertTrue(v.getFix().isPlaceholder());
        assertEquals("None", v.getRecommendation().getText());
        assertTrue(v.getRecommendation().isPlaceholder());
    }

    @Test
    void blankDetailFieldsCountAsEmptyToo() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setTechnician("   ");
        r.setFix("");
        r.setRecommendation("  ");
        RepairView v = RepairView.from(r);

        assertTrue(v.getTechnician().isPlaceholder());
        assertTrue(v.getFix().isPlaceholder());
        assertTrue(v.getRecommendation().isPlaceholder());
    }

    @Test
    void filledDetailFieldsAreShownAsEnteredAndNotMuted() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setTechnician("Juan");
        r.setFix("Replaced the display cable");
        r.setRecommendation("Back up your files");
        RepairView v = RepairView.from(r);

        assertEquals("Juan", v.getTechnician().getText());
        assertFalse(v.getTechnician().isPlaceholder());
        assertEquals("Replaced the display cable", v.getFix().getText());
        assertFalse(v.getFix().isPlaceholder());
        assertEquals("Back up your files", v.getRecommendation().getText());
        assertFalse(v.getRecommendation().isPlaceholder());
    }

    // ---- Warranty ---------------------------------------------------------------------------------

    @Test
    void warrantyCountsDaysLeft() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setWarrantyEndDate(inDays(30));

        assertEquals("30 days left", RepairView.from(r).getWarranty().getText());
        assertFalse(RepairView.from(r).getWarranty().isPlaceholder());
    }

    @Test
    void warrantyUsesTheSingularForOneDay() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setWarrantyEndDate(inDays(1));

        assertEquals("1 day left", RepairView.from(r).getWarranty().getText());
    }

    @Test
    void warrantyEndingTodayAndExpiredAreWorded() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        r.setWarrantyEndDate(inDays(0));
        assertEquals("Expires today", RepairView.from(r).getWarranty().getText());

        r.setWarrantyEndDate(inDays(-3));
        assertEquals("Expired", RepairView.from(r).getWarranty().getText());
    }

    @Test
    void noWarrantyEndDateReadsNoWarrantySet() throws Exception {
        assertEquals("No warranty set", RepairView.from(repair(RepairStatus.COMPLETED)).getWarranty().getText());
    }

    // ---- Photo and search -------------------------------------------------------------------------

    @Test
    void thePhotoIsOnlyPresentWhenAFileNameIsSet() throws Exception {
        RepairRecord r = repair(RepairStatus.COMPLETED);
        assertFalse(RepairView.from(r).isHasPhoto());

        r.setImageFileName("   ");
        assertFalse(RepairView.from(r).isHasPhoto());

        r.setImageFileName("1_fixed.png");
        assertTrue(RepairView.from(r).isHasPhoto());
        assertEquals("1_fixed.png", RepairView.from(r).getPhotoFile());
    }

    @Test
    void searchTextCoversJobOrderDeviceAndIssueInLowerCase() throws Exception {
        String text = RepairView.from(repair(RepairStatus.PENDING)).getSearchText();

        assertTrue(text.contains("jo-00007"));
        assertTrue(text.contains("acer aspire 5"));
        assertTrue(text.contains("no display"));
        assertEquals(text, text.toLowerCase());
    }

    // ---- What must never reach a customer ---------------------------------------------------------

    @Test
    void remarksAndClientDetailsAreNotCarriedAnywhereOnTheView() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setRemarks("INTERNAL: customer was rude, do not discount");
        r.setTechnician("Juan");
        RepairView v = RepairView.from(r);

        String everything = new ObjectMapper().writeValueAsString(v) + v.getStatusSentence()
                + v.getTechnician().getText() + v.getWarranty().getText() + v.getFix().getText()
                + v.getRecommendation().getText() + v.getPhotoFile();

        assertFalse(everything.contains("INTERNAL"), everything);
        assertFalse(everything.contains("rude"), everything);
        assertFalse(everything.contains("Nand Test"), everything);
        assertFalse(everything.contains("0999"), everything);
        assertFalse(everything.contains("null"), everything);
    }

    @Test
    void theSerializedListCarriesOnlyWhatTheFilterNeeds() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setFix("secret fix text");
        String json = new ObjectMapper().writeValueAsString(RepairView.from(r));

        assertTrue(json.contains("\"searchText\""));
        assertTrue(json.contains("\"statusRank\""));
        assertFalse(json.contains("secret fix text"), json);
        assertFalse(json.contains("\"steps\""), json);
        assertFalse(json.contains("\"statusEnum\""), json);
    }

    @Test
    void aBareRecordWithEverythingNullStillProducesNoNullText() {
        RepairView v = RepairView.from(new RepairRecord());

        assertEquals("", v.getJobOrderNumber());
        assertEquals("", v.getDeviceLabel());
        assertEquals("", v.getIssue());
        assertEquals("", v.getStatus());
        assertEquals("Not set", v.getDate());
        assertFalse(v.getSearchText().contains("null"));
        assertFalse(v.getTechnician().getText().contains("null"));
        assertFalse(v.getWarranty().getText().contains("null"));
    }
}
