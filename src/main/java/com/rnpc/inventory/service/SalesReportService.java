package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.SalesDayRow;
import com.rnpc.inventory.dto.SalesMonthRow;
import com.rnpc.inventory.dto.SalesReportData;
import com.rnpc.inventory.entity.Order;
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
    }

    public SalesReportData buildReportData() {
        Map<LocalDate, DayAgg> byDay = new HashMap<>();

        for (Order order : orderService.getAllOrders()) {
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

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
