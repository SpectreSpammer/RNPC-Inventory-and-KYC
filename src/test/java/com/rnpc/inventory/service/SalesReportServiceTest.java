package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.SalesDayRow;
import com.rnpc.inventory.dto.SalesReportData;
import com.rnpc.inventory.dto.SalesServiceRow;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.OrderItem;
import com.rnpc.inventory.entity.RepairRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the service breakdown and the outstanding balances that back the Sales Report page's
 * "Top 5 Services by Revenue" and "Unpaid / Outstanding" cards. Plain unit test - the two
 * collaborators are mocked, so this needs no Spring context and no database.
 */
class SalesReportServiceTest {

    private static final LocalDate DAY = LocalDate.now();

    private SalesDayRow reportFor(List<Order> orders, List<RepairRecord> repairs) {
        OrderService orderService = mock(OrderService.class);
        RepairRecordService repairRecordService = mock(RepairRecordService.class);
        when(orderService.getAllOrders()).thenReturn(orders);
        when(repairRecordService.getAllRepairRecords()).thenReturn(repairs);

        SalesReportData data = new SalesReportService(orderService, repairRecordService).buildReportData();
        return data.getDaily().get(DAY.getYear()).get(DAY.getMonthValue() - 1).get(DAY.getDayOfMonth() - 1);
    }

    private Map<String, SalesServiceRow> servicesByName(SalesDayRow row) {
        return row.getServices().stream().collect(Collectors.toMap(SalesServiceRow::getName, Function.identity()));
    }

    private static Date today() {
        return Date.from(DAY.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Order order(Order.OrderStatus status, double total, String... categories) {
        Order order = new Order();
        order.setStatus(status);
        order.setTotalAmount(total);
        order.setCreatedAt(today());
        if (status == Order.OrderStatus.PAID) {
            order.setVerifiedAt(today());
        }
        List<OrderItem> items = new ArrayList<>();
        for (String category : categories) {
            OrderItem item = new OrderItem();
            item.setCategory(category);
            item.setPrice(total / categories.length);
            items.add(item);
        }
        order.setItems(items);
        return order;
    }

    private static RepairRecord repair(RepairRecord.RepairStatus status, String deviceType, double cost) {
        RepairRecord repair = new RepairRecord();
        repair.setStatus(status);
        repair.setDeviceType(deviceType);
        repair.setCost(cost);
        repair.setRepairDate(today());
        return repair;
    }

    // The 7 slots OrderService.isFullBuild requires - anything less is a parts-only order.
    private static final String[] FULL_BUILD =
            {"CPU", "MOTHERBOARD", "RAM", "STORAGE_SSD", "PSU", "CASE", "COOLER"};

    @Test
    void fullBuildOrderBecomesOneServiceLineForTheWholeOrder() {
        SalesDayRow row = reportFor(List.of(order(Order.OrderStatus.PAID, 70000, FULL_BUILD)), List.of());

        assertEquals(1, row.getServices().size());
        SalesServiceRow service = row.getServices().get(0);
        assertEquals("Custom PC Build", service.getName());
        assertEquals("orders", service.getSource());
        assertEquals(70000, service.getRevenue(), 0.001);
        assertEquals(1, service.getCount());
    }

    @Test
    void partsOnlyOrderBecomesOneServiceLinePerItemCategory() {
        SalesDayRow row = reportFor(List.of(order(Order.OrderStatus.PAID, 30000, "CPU", "GPU")), List.of());

        Map<String, SalesServiceRow> services = servicesByName(row);
        assertEquals(2, services.size());
        assertEquals(15000, services.get("Processor (CPU)").getRevenue(), 0.001);
        assertEquals(15000, services.get("Graphics Card").getRevenue(), 0.001);
    }

    @Test
    void completedRepairsGroupByDeviceTypeAndMergeAcrossJobs() {
        SalesDayRow row = reportFor(List.of(), Arrays.asList(
                repair(RepairRecord.RepairStatus.COMPLETED, "Laptop", 800),
                repair(RepairRecord.RepairStatus.RELEASED, "Laptop", 1200),
                repair(RepairRecord.RepairStatus.COMPLETED, "Cellphone", 500)));

        Map<String, SalesServiceRow> services = servicesByName(row);
        assertEquals(2, services.size());
        assertEquals(2000, services.get("Laptop Repair").getRevenue(), 0.001);
        assertEquals(2, services.get("Laptop Repair").getCount());
        assertEquals("repairs", services.get("Laptop Repair").getSource());
        assertEquals(500, services.get("Cellphone Repair").getRevenue(), 0.001);
    }

    @Test
    void unfinishedRepairsEarnNothingAndProduceNoServiceLine() {
        SalesDayRow row = reportFor(List.of(), Arrays.asList(
                repair(RepairRecord.RepairStatus.PENDING, "Laptop", 800),
                repair(RepairRecord.RepairStatus.IN_PROGRESS, "Desktop", 900),
                repair(RepairRecord.RepairStatus.CANCELLED, "Cellphone", 500)));

        assertEquals(0, row.getRepairsRev(), 0.001);
        assertTrue(row.getServices().isEmpty());
    }

    @Test
    void awaitingPaymentOrdersAreOutstandingAndNeverRevenue() {
        SalesDayRow row = reportFor(List.of(order(Order.OrderStatus.AWAITING_PAYMENT, 25000, "CPU")), List.of());

        assertEquals(25000, row.getUnpaidRev(), 0.001);
        assertEquals(1, row.getUnpaidCount());
        assertEquals(0, row.getOrdersRev(), 0.001);
        assertEquals(0, row.getOrdersCount());
        assertTrue(row.getServices().isEmpty());
    }

    @Test
    void cancelledOrderWithPendingRefundIsOwedBackAndNeverRevenue() {
        Order cancelled = order(Order.OrderStatus.CANCELLED, 18000, "GPU");
        cancelled.setVerifiedAt(today());
        cancelled.setRefundStatus(Order.RefundStatus.PENDING);

        SalesDayRow row = reportFor(List.of(cancelled), List.of());

        assertEquals(18000, row.getRefundRev(), 0.001);
        assertEquals(1, row.getRefundCount());
        assertEquals(0, row.getOrdersRev(), 0.001);
        assertEquals(0, row.getUnpaidRev(), 0.001);
        assertTrue(row.getServices().isEmpty());
    }

    /**
     * The card can only be trusted if its lines add up to the same revenue the rest of the page
     * reports - a build counted as both one build AND eight components, or an unpaid order leaking
     * into a service line, would show up here.
     */
    @Test
    void serviceRevenueSumsToExactlyTheDaysTotalRevenue() {
        Order awaitingPayment = order(Order.OrderStatus.AWAITING_PAYMENT, 25000, "CPU");
        Order refundOwed = order(Order.OrderStatus.CANCELLED, 18000, "GPU");
        refundOwed.setRefundStatus(Order.RefundStatus.PENDING);

        SalesDayRow row = reportFor(
                Arrays.asList(order(Order.OrderStatus.PAID, 70000, FULL_BUILD),
                        order(Order.OrderStatus.PAID, 30000, "CPU", "GPU"),
                        awaitingPayment, refundOwed),
                List.of(repair(RepairRecord.RepairStatus.COMPLETED, "Laptop", 800)));

        double serviceTotal = row.getServices().stream().mapToDouble(SalesServiceRow::getRevenue).sum();
        assertEquals(row.getOrdersRev() + row.getRepairsRev(), serviceTotal, 0.001);
        assertEquals(100800, serviceTotal, 0.001);
    }
}
