package com.rnpc.inventory.service;

import com.rnpc.inventory.entity.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Read-only aggregation through existing services; called only after the admin controller guard. */
@Service
public class AdminDashboardService {
    public static final int LOW_STOCK_LIMIT = 5;
    private final OrderService orders;
    private final AppointmentService appointments;
    private final RepairRecordService repairs;
    private final CpuPartsService cpu;
    private final GpuPartsService gpu;
    private final MotherboardPartsService motherboard;
    private final RamPartsService ram;
    private final StoragePartsService storage;
    private final PsuPartsService psu;
    private final CasePartsService cases;
    private final CoolerPartsService cooler;
    private final LaptopPartsService laptop;
    private final CellphonePartsService cellphone;
    public AdminDashboardService(OrderService orders, AppointmentService appointments, RepairRecordService repairs, CpuPartsService cpu, GpuPartsService gpu, MotherboardPartsService motherboard, RamPartsService ram, StoragePartsService storage, PsuPartsService psu, CasePartsService cases, CoolerPartsService cooler, LaptopPartsService laptop, CellphonePartsService cellphone) {
        this.orders = orders;
        this.appointments = appointments;
        this.repairs = repairs;
        this.cpu = cpu;
        this.gpu = gpu;
        this.motherboard = motherboard;
        this.ram = ram;
        this.storage = storage;
        this.psu = psu;
        this.cases = cases;
        this.cooler = cooler;
        this.laptop = laptop;
        this.cellphone = cellphone;
    }

    public Map<String,Object> load(LocalDateTime now, int days) {
        days = days == 30 ? 30 : 7;
        LocalDate today = now.toLocalDate();
        List<Order> allOrders = orders.getAllOrders();
        List<Appointment> allAppointments = appointments.getAllAppointments();
        List<RepairRecord> allRepairs = repairs.getAllRepairRecords();
        Map<String,Object> model = new LinkedHashMap<>();
        model.put("totalOrders", allOrders.size());
        model.put("appointmentsToday", allAppointments.stream()
                .filter(a -> a.getStatus() != Appointment.Status.CANCELLED)
                .filter(a -> today.equals(a.getPreferredDate())).count());
        List<RepairRecord> active = allRepairs.stream()
                .filter(a -> a.getStatus() == RepairRecord.RepairStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(RepairRecord::getRepairId).reversed()).toList();
        model.put("activeRepairCount", active.size());
        model.put("openTicketCount", allRepairs.stream()
                .filter(a -> a.getStatus() == RepairRecord.RepairStatus.PENDING).count());
        model.put("activeRepairs", active.stream().limit(5).toList());
        model.put("recentOrders", allOrders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Order::getOrderId, Comparator.reverseOrder())).limit(5).toList());
        model.put("upcomingAppointments", allAppointments.stream()
                .filter(a -> a.getStatus() != Appointment.Status.CANCELLED)
                .filter(a -> a.getPreferredDate() != null && a.getPreferredTime() != null)
                .filter(a -> !LocalDateTime.of(a.getPreferredDate(), a.getPreferredTime()).isBefore(now))
                .sorted(Comparator.comparing(Appointment::getPreferredDate).thenComparing(Appointment::getPreferredTime))
                .limit(5).toList());
        List<Map<String,Object>> chart = new ArrayList<>();
        LocalDate first = today.minusDays(days - 1);
        Map<LocalDate,Long> counts = new HashMap<>();
        for (Order order : allOrders) {
            if (order.getCreatedAt() == null) continue;
            LocalDate date = Instant.ofEpochMilli(order.getCreatedAt().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.isBefore(first) && !date.isAfter(today)) counts.merge(date, 1L, Long::sum);
        }
        for (int i=0;i<days;i++) {
            LocalDate date = first.plusDays(i);
            chart.add(Map.of("label", date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)),
                    "count", counts.getOrDefault(date,0L)));
        }
        model.put("chartDays", days);
        model.put("chartData", chart);
        model.put("chartTotal", counts.values().stream().mapToLong(Long::longValue).sum());
        List<Map<String,Object>> stock = new ArrayList<>();
        cpu.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Cpu"));
        gpu.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Gpu"));
        motherboard.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Motherboard"));
        ram.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Ram"));
        storage.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Storage"));
        psu.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Psu"));
        cases.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Case"));
        cooler.getAllParts().forEach(p -> addStock(stock, p.getBrand(), p.getModelName(), p.getStocks(), "/computer", "Cooler"));
        laptop.getAllLaptopParts().forEach(p -> addStock(stock, p.getBrand(), p.getPartName(), p.getStocks(), "/laptop", "Laptop"));
        cellphone.getAllCellphoneParts().forEach(p -> addStock(stock, p.getBrand(), p.getPartName(), p.getStocks(), "/cellphone", "Cellphone"));
        stock.sort(Comparator.<Map<String,Object>>comparingInt(p -> (Integer)p.get("stocks"))
                .thenComparing(p -> (String)p.get("name")));
        model.put("lowStockCount", stock.size());
        model.put("allLowStockParts", stock);
        model.put("lowStockParts", stock.stream().limit(5).toList());
        model.put("lowStockLimit", LOW_STOCK_LIMIT);
        return model;
    }

    private static void addStock(List<Map<String,Object>> rows, String brand, String name, int stocks, String url, String category) {
        if (stocks > LOW_STOCK_LIMIT) return;
        String label = ((brand == null ? "" : brand) + " " + (name == null ? "" : name)).trim();
        rows.add(Map.of("name", label.isBlank() ? category + " part" : label, "stocks", stocks,
                "url", url, "category", category, "status", stocks <= 0 ? "Out of Stock" : "Low Stock"));
    }
}
