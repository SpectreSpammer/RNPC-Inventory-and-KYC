package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.SalesDayRow;
import com.rnpc.inventory.dto.SalesMonthRow;
import com.rnpc.inventory.dto.SalesReportData;
import com.rnpc.inventory.dto.SalesServiceRow;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.OrderItem;
import com.rnpc.inventory.entity.RepairRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates real revenue - PAID Orders and COMPLETED/RELEASED RepairRecords - into the month/day
 * buckets the Sales Report page charts and tables render. Revenue is only counted once money has
 * actually changed hands: an Order counts on the date it was marked PAID (falling back to when it
 * was placed, for the edge case of a PAID order with no verifiedAt), and a RepairRecord counts on
 * its repair date once the job reached COMPLETED or RELEASED - not on PENDING/IN_PROGRESS/
 * CANCELLED jobs that never earned anything.
 *
 * Alongside that it derives two things the report also shows, from the same single pass over the
 * same records - no extra query, no new entity:
 *
 *  - a per-service revenue breakdown (see SalesServiceRow) that sums to exactly the same total as
 *    the revenue figures above;
 *  - the outstanding balances that are deliberately NOT revenue: orders still AWAITING_PAYMENT
 *    (owed to the shop) and cancelled orders whose refund is still PENDING (owed by the shop).
 */
@Service
public class SalesReportService {

    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] MONTHS_FULL = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    // Plain-words names for the build-slot keys OrderItem.getCategory() stores (set by
    // OrderService.createOrder from the builder's own selection keys - see its REQUIRED_BUILD_SLOTS
    // comment). Only used for display on the Top 5 Services card; an unrecognised key falls back to
    // the raw category so a newly added slot still shows up instead of silently vanishing.
    private static final Map<String, String> PART_SERVICE_NAMES = Map.of(
            "CPU", "Processor (CPU)",
            "GPU", "Graphics Card",
            "MOTHERBOARD", "Motherboard",
            "RAM", "Memory (RAM)",
            "STORAGE_SSD", "Storage (SSD)",
            "STORAGE_HDD", "Storage (HDD)",
            "PSU", "Power Supply",
            "CASE", "Case",
            "COOLER", "CPU Cooler"
    );

    private static final String SOURCE_ORDERS = "orders";
    private static final String SOURCE_REPAIRS = "repairs";

    private final OrderService orderService;
    private final RepairRecordService repairRecordService;

    @Autowired
    public SalesReportService(OrderService orderService, RepairRecordService repairRecordService) {
        this.orderService = orderService;
        this.repairRecordService = repairRecordService;
    }

    private static class DayAgg {
        double ordersRev;
        double repairsRev;
        int ordersCount;
        int repairsCount;

        double unpaidRev;
        int unpaidCount;
        double refundRev;
        int refundCount;

        // Keyed by service name - one entry per name per day, so the same name from two different
        // orders on one day merges instead of producing duplicate lines.
        final Map<String, SalesServiceRow> services = new LinkedHashMap<>();

        void addService(String name, String source, double revenue) {
            SalesServiceRow row = services.computeIfAbsent(name, n -> new SalesServiceRow(n, source, 0, 0));
            row.setRevenue(row.getRevenue() + revenue);
            row.setCount(row.getCount() + 1);
        }
    }

    public SalesReportData buildReportData() {
        Map<LocalDate, DayAgg> byDay = new HashMap<>();

        for (Order order : orderService.getAllOrders()) {
            // A cancelled order that was already paid still owes the customer a refund, whatever
            // its own date bucket - checked before the status switch below because a CANCELLED
            // order never reaches either of those branches. Bucketed on the day the money was
            // actually taken (verifiedAt), since that's the transaction being reversed; there's no
            // cancelledAt column to use instead.
            if (order.getRefundStatus() == Order.RefundStatus.PENDING) {
                Date refundAt = order.getVerifiedAt() != null ? order.getVerifiedAt() : order.getCreatedAt();
                if (refundAt != null) {
                    DayAgg agg = byDay.computeIfAbsent(toLocalDate(refundAt), d -> new DayAgg());
                    agg.refundRev += order.getTotalAmount();
                    agg.refundCount++;
                }
            }

            if (order.getStatus() == Order.OrderStatus.AWAITING_PAYMENT) {
                // Owed TO the shop: the customer has claimed payment but no admin has verified the
                // reference number yet (see OrderController.markPaid). Bucketed on the day it was
                // placed - an unverified order has no verifiedAt by definition.
                if (order.getCreatedAt() != null) {
                    DayAgg agg = byDay.computeIfAbsent(toLocalDate(order.getCreatedAt()), d -> new DayAgg());
                    agg.unpaidRev += order.getTotalAmount();
                    agg.unpaidCount++;
                }
                continue;
            }

            if (order.getStatus() != Order.OrderStatus.PAID) {
                continue;
            }
            Date at = order.getVerifiedAt() != null ? order.getVerifiedAt() : order.getCreatedAt();
            if (at == null) {
                continue;
            }
            DayAgg agg = byDay.computeIfAbsent(toLocalDate(at), d -> new DayAgg());
            agg.ordersRev += order.getTotalAmount();
            agg.ordersCount++;
            addOrderServices(agg, order);
        }

        for (RepairRecord repair : repairRecordService.getAllRepairRecords()) {
            if (repair.getStatus() != RepairRecord.RepairStatus.COMPLETED
                    && repair.getStatus() != RepairRecord.RepairStatus.RELEASED) {
                continue;
            }
            Date at = repair.getRepairDate() != null ? repair.getRepairDate() : repair.getCreatedAt();
            if (at == null) {
                continue;
            }
            DayAgg agg = byDay.computeIfAbsent(toLocalDate(at), d -> new DayAgg());
            agg.repairsRev += repair.getCost();
            agg.repairsCount++;
            // Device type is the only service-shaped field a repair records, and it's a fixed
            // allow-list (Cellphone/Laptop/Desktop - see RepairRecordDto), so it groups cleanly.
            String deviceType = repair.getDeviceType();
            String serviceName = deviceType == null || deviceType.isBlank()
                    ? "Repair Service"
                    : deviceType.trim() + " Repair";
            agg.addService(serviceName, SOURCE_REPAIRS, repair.getCost());
        }

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonthIdx = today.getMonthValue() - 1;

        // The report's year/month range has to cover every transaction date that actually exists,
        // not just "up to today" - a repair or order can legitimately be dated in the future (e.g.
        // a back-office data entry error, or a scheduled/warranty date), and that revenue must
        // still show up somewhere rather than silently vanishing from every view. So the upper
        // bound is today unioned with the latest real transaction date, not today alone.
        int minYear = currentYear;
        int maxYear = currentYear;
        for (LocalDate date : byDay.keySet()) {
            minYear = Math.min(minYear, date.getYear());
            maxYear = Math.max(maxYear, date.getYear());
        }

        List<Integer> years = new ArrayList<>();
        for (int y = minYear; y <= maxYear; y++) {
            years.add(y);
        }

        Map<Integer, List<SalesMonthRow>> monthly = new LinkedHashMap<>();
        Map<Integer, Map<Integer, List<SalesDayRow>>> daily = new LinkedHashMap<>();

        for (int year : years) {
            int monthCount = year < currentYear ? 12 : currentMonthIdx + 1;
            for (LocalDate date : byDay.keySet()) {
                if (date.getYear() == year) {
                    monthCount = Math.max(monthCount, date.getMonthValue());
                }
            }
            List<SalesMonthRow> monthRows = new ArrayList<>();
            Map<Integer, List<SalesDayRow>> monthsOfDays = new LinkedHashMap<>();

            for (int m = 0; m < monthCount; m++) {
                YearMonth ym = YearMonth.of(year, m + 1);
                int lastDay = ym.lengthOfMonth();

                SalesMonthRow monthRow = new SalesMonthRow();
                monthRow.setMonth(m);
                monthRow.setLabel(MONTHS[m]);
                monthRow.setFullLabel(MONTHS_FULL[m] + " " + year);

                List<SalesDayRow> dayRows = new ArrayList<>();
                for (int d = 1; d <= lastDay; d++) {
                    DayAgg agg = byDay.get(LocalDate.of(year, m + 1, d));

                    SalesDayRow dayRow = new SalesDayRow();
                    dayRow.setDay(d);
                    dayRow.setLabel(String.valueOf(d));
                    dayRow.setFullLabel(MONTHS_FULL[m] + " " + d + ", " + year);
                    if (agg != null) {
                        dayRow.setOrdersRev(agg.ordersRev);
                        dayRow.setRepairsRev(agg.repairsRev);
                        dayRow.setOrdersCount(agg.ordersCount);
                        dayRow.setRepairsCount(agg.repairsCount);
                        dayRow.setUnpaidRev(agg.unpaidRev);
                        dayRow.setUnpaidCount(agg.unpaidCount);
                        dayRow.setRefundRev(agg.refundRev);
                        dayRow.setRefundCount(agg.refundCount);
                        dayRow.setServices(new ArrayList<>(agg.services.values()));
                    }
                    dayRows.add(dayRow);

                    monthRow.setOrdersRev(monthRow.getOrdersRev() + dayRow.getOrdersRev());
                    monthRow.setRepairsRev(monthRow.getRepairsRev() + dayRow.getRepairsRev());
                    monthRow.setOrdersCount(monthRow.getOrdersCount() + dayRow.getOrdersCount());
                    monthRow.setRepairsCount(monthRow.getRepairsCount() + dayRow.getRepairsCount());
                }

                monthRows.add(monthRow);
                monthsOfDays.put(m, dayRows);
            }

            monthly.put(year, monthRows);
            daily.put(year, monthsOfDays);
        }

        SalesReportData data = new SalesReportData();
        data.setYears(years);
        data.setCurrentYear(currentYear);
        data.setCurrentMonth(currentMonthIdx);
        data.setAsOfLabel("As of " + MONTHS_FULL[currentMonthIdx] + " " + today.getDayOfMonth() + ", " + currentYear);
        data.setMonthly(monthly);
        data.setDaily(daily);
        return data;
    }

    /**
     * Splits one paid order into the service line(s) it earned. A full build is deliberately ONE
     * line for the whole order rather than eight component lines - that's what the shop actually
     * sold, and it's the same full-build-vs-parts distinction OrderService.isFullBuild already
     * draws for the build timeline. A parts-only order contributes one line per item instead,
     * because there's no single service to name it. Either way the amounts added here sum to the
     * order's own total, so the services card can never disagree with Orders Revenue.
     */
    private void addOrderServices(DayAgg agg, Order order) {
        if (OrderService.isFullBuild(order)) {
            agg.addService("Custom PC Build", SOURCE_ORDERS, order.getTotalAmount());
            return;
        }
        for (OrderItem item : order.getItems()) {
            String category = item.getCategory();
            String name = category == null || category.isBlank()
                    ? "Other Parts"
                    : PART_SERVICE_NAMES.getOrDefault(category, category);
            agg.addService(name, SOURCE_ORDERS, item.getPrice());
        }
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
