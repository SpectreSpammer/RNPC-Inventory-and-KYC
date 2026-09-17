package com.rnpc.inventory.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * One day's aggregated revenue for the Sales Report page's daily chart/ledger - see
 * SalesReportService.buildReportData. Not a form-binding DTO; only ever serialized to JSON for
 * the page's own script to render.
 *
 * The service breakdown and the unpaid/refund figures below live on the DAY row (and deliberately
 * not on SalesMonthRow) because the page's script always re-aggregates from day rows for both the
 * yearly and monthly views - see filteredRows() in salesReport.html. Keeping one source of truth
 * means a month can never disagree with the days inside it.
 */
public class SalesDayRow {

    private int day;
    private String label;
    private String fullLabel;
    private double ordersRev;
    private double repairsRev;
    private int ordersCount;
    private int repairsCount;

    // Only the lines that actually earned something on this day - empty for the (many) days with
    // no sales, so the serialized payload stays roughly the size it was before.
    private List<SalesServiceRow> services = new ArrayList<>();

    // Money still owed TO the shop: orders sitting in AWAITING_PAYMENT, bucketed on the day they
    // were placed. Not revenue - deliberately kept out of every total above.
    private double unpaidRev;
    private int unpaidCount;

    // Money the shop owes BACK: cancelled orders whose refundStatus is still PENDING. Also not
    // revenue, and never double-counted against unpaidRev - a cancelled order is no longer
    // awaiting payment.
    private double refundRev;
    private int refundCount;

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFullLabel() {
        return fullLabel;
    }

    public void setFullLabel(String fullLabel) {
        this.fullLabel = fullLabel;
    }

    public double getOrdersRev() {
        return ordersRev;
    }

    public void setOrdersRev(double ordersRev) {
        this.ordersRev = ordersRev;
    }

    public double getRepairsRev() {
        return repairsRev;
    }

    public void setRepairsRev(double repairsRev) {
        this.repairsRev = repairsRev;
    }

    public int getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(int ordersCount) {
        this.ordersCount = ordersCount;
    }

    public int getRepairsCount() {
        return repairsCount;
    }

    public void setRepairsCount(int repairsCount) {
        this.repairsCount = repairsCount;
    }

    public List<SalesServiceRow> getServices() {
        return services;
    }

    public void setServices(List<SalesServiceRow> services) {
        this.services = services;
    }

    public double getUnpaidRev() {
        return unpaidRev;
    }

    public void setUnpaidRev(double unpaidRev) {
        this.unpaidRev = unpaidRev;
    }

    public int getUnpaidCount() {
        return unpaidCount;
    }

    public void setUnpaidCount(int unpaidCount) {
        this.unpaidCount = unpaidCount;
    }

    public double getRefundRev() {
        return refundRev;
    }

    public void setRefundRev(double refundRev) {
        this.refundRev = refundRev;
    }

    public int getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(int refundCount) {
        this.refundCount = refundCount;
    }
}
