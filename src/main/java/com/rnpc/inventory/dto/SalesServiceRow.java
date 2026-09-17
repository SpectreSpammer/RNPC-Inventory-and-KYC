package com.rnpc.inventory.dto;

/**
 * One service line's revenue on a single day, for the Sales Report page's "Top 5 Services by
 * Revenue" card - see SalesReportService.buildReportData. Carried per day (on SalesDayRow) rather
 * than pre-ranked per year, so the page's own script can re-rank it against whatever date range,
 * month and source the admin has filtered to, exactly the way it already re-aggregates revenue.
 *
 * There is no service catalog in this app, so a "service" is derived from what an order or repair
 * already records: a full PC build order is one line ("Custom PC Build"), a parts-only order
 * contributes one line per OrderItem category, and a completed repair contributes one line per
 * device type. Summed across every line, this equals total revenue for the same period - nothing
 * is invented and nothing is double-counted.
 *
 * Not a form-binding DTO; only ever serialized to JSON for the page's own script to render.
 */
public class SalesServiceRow {

    private String name;

    // "orders" or "repairs" - matches the page's Source filter values so a line can be dropped
    // when the admin narrows to one source.
    private String source;

    private double revenue;

    // Number of orders (or repair jobs) that contributed to this line. An order can hold at most
    // one item per category, so this never counts the same order twice for the same service.
    private int count;

    public SalesServiceRow() {
    }

    public SalesServiceRow(String name, String source, double revenue, int count) {
        this.name = name;
        this.source = source;
        this.revenue = revenue;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
