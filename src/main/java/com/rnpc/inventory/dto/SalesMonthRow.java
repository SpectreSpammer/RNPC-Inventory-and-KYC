package com.rnpc.inventory.dto;

/**
 * One month's aggregated revenue for the Sales Report page's yearly chart/ledger - see
 * SalesReportService.buildReportData. Not a form-binding DTO; only ever serialized to JSON for
 * the page's own script to render.
 */
public class SalesMonthRow {

    private int month;
    private String label;
    private String fullLabel;
    private double ordersRev;
    private double repairsRev;
    private int ordersCount;
    private int repairsCount;

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
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
}
