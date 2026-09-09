package com.rnpc.inventory.dto;

import java.util.List;
import java.util.Map;

/**
 * Whole payload the Sales Report page's script needs, built once per request by
 * SalesReportService.buildReportData and serialized straight to JSON (see
 * SalesReportController) - the page's JS then just filters/renders this, it never re-fetches or
 * re-aggregates anything itself.
 */
public class SalesReportData {

    private List<Integer> years;
    private int currentYear;
    private int currentMonth;
    private String asOfLabel;
    private Map<Integer, List<SalesMonthRow>> monthly;
    private Map<Integer, Map<Integer, List<SalesDayRow>>> daily;

    public List<Integer> getYears() {
        return years;
    }

    public void setYears(List<Integer> years) {
        this.years = years;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }

    public int getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public String getAsOfLabel() {
        return asOfLabel;
    }

    public void setAsOfLabel(String asOfLabel) {
        this.asOfLabel = asOfLabel;
    }

    public Map<Integer, List<SalesMonthRow>> getMonthly() {
        return monthly;
    }

    public void setMonthly(Map<Integer, List<SalesMonthRow>> monthly) {
        this.monthly = monthly;
    }

    public Map<Integer, Map<Integer, List<SalesDayRow>>> getDaily() {
        return daily;
    }

    public void setDaily(Map<Integer, Map<Integer, List<SalesDayRow>>> daily) {
        this.daily = daily;
    }
}
