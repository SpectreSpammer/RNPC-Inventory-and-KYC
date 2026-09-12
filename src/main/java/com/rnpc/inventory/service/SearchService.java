package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.SearchResults;
import com.rnpc.inventory.dto.SearchSuggestion;
import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.OrderItem;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.SavedBuild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Backs the topbar search (SearchController). Every record considered here comes from the same
 * per-user finder each type's own list page already calls - OrderService.getOrdersForUser
 * (OrderService.java:64), RepairRecordService.getRepairRecordsForUser (RepairRecordService.java:123),
 * AppointmentService.getAppointmentsForUser (AppointmentService.java:129), and
 * SavedBuildService.getSavedBuildsForUser (SavedBuildService.java:49) - never a get-all method,
 * never a repository directly. This class only filters those already-scoped lists in memory by a
 * case-insensitive "contains" match; it adds no new query anywhere.
 */
@Service
public class SearchService {

    private static final int MAX_RESULTS_PER_TYPE = 20;
    private static final int MAX_SUGGESTIONS = 6;

    private final OrderService orderService;
    private final RepairRecordService repairRecordService;
    private final AppointmentService appointmentService;
    private final SavedBuildService savedBuildService;

    @Autowired
    public SearchService(OrderService orderService, RepairRecordService repairRecordService,
                          AppointmentService appointmentService, SavedBuildService savedBuildService) {
        this.orderService = orderService;
        this.repairRecordService = repairRecordService;
        this.appointmentService = appointmentService;
        this.savedBuildService = savedBuildService;
    }

    // `query` is expected already trimmed/length-capped by the controller - this method just
    // matches it, case-insensitively, against each type's own set of searchable fields.
    public SearchResults search(String username, String query) {
        MatchedRecords matches = match(username, query);

        return new SearchResults(
                cap(matches.orders), matches.orders.size(),
                cap(matches.repairs), matches.repairs.size(),
                cap(matches.appointments), matches.appointments.size(),
                cap(matches.savedBuilds), matches.savedBuilds.size()
        );
    }

    // Topbar live-suggestion dropdown (SearchController's GET /search/suggest). Uses the exact
    // same matched, already-per-user-scoped lists as search() above (via match()) - this only
    // differs in how many of each type it keeps, so the two can never disagree on which records
    // match a query.
    public List<SearchSuggestion> suggest(String username, String query) {
        MatchedRecords matches = match(username, query);

        List<SearchSuggestion> suggestions = new ArrayList<>();
        int round = 0;
        while (suggestions.size() < MAX_SUGGESTIONS
                && (round < matches.orders.size() || round < matches.repairs.size()
                        || round < matches.appointments.size() || round < matches.savedBuilds.size())) {
            if (round < matches.orders.size() && suggestions.size() < MAX_SUGGESTIONS) {
                suggestions.add(toSuggestion(matches.orders.get(round)));
            }
            if (round < matches.repairs.size() && suggestions.size() < MAX_SUGGESTIONS) {
                suggestions.add(toSuggestion(matches.repairs.get(round)));
            }
            if (round < matches.appointments.size() && suggestions.size() < MAX_SUGGESTIONS) {
                suggestions.add(toSuggestion(matches.appointments.get(round)));
            }
            if (round < matches.savedBuilds.size() && suggestions.size() < MAX_SUGGESTIONS) {
                suggestions.add(toSuggestion(matches.savedBuilds.get(round)));
            }
            round++;
        }
        return suggestions;
    }

    private MatchedRecords match(String username, String query) {
        String needle = query.toLowerCase(Locale.ROOT);

        List<Order> matchingOrders = orderService.getOrdersForUser(username).stream()
                .filter(order -> orderMatches(order, needle))
                .collect(Collectors.toList());

        List<RepairRecord> matchingRepairs = repairRecordService.getRepairRecordsForUser(username).stream()
                .filter(repair -> repairMatches(repair, needle))
                .collect(Collectors.toList());

        List<Appointment> matchingAppointments = appointmentService.getAppointmentsForUser(username).stream()
                .filter(appointment -> appointmentMatches(appointment, needle))
                .collect(Collectors.toList());

        List<SavedBuild> matchingSavedBuilds = savedBuildService.getSavedBuildsForUser(username).stream()
                .filter(build -> buildMatches(build, needle))
                .collect(Collectors.toList());

        return new MatchedRecords(matchingOrders, matchingRepairs, matchingAppointments, matchingSavedBuilds);
    }

    private <T> List<T> cap(List<T> list) {
        return list.size() > MAX_RESULTS_PER_TYPE ? list.subList(0, MAX_RESULTS_PER_TYPE) : list;
    }

    // Order.java:91 getOrderNumber, :44/getReferenceNumber, :115 getStatus.
    private SearchSuggestion toSuggestion(Order order) {
        String sub = order.getReferenceNumber() != null
                ? "Ref: " + order.getReferenceNumber()
                : (order.getStatus() != null ? order.getStatus().name() : null);
        return new SearchSuggestion("ORDER", order.getOrderId(), order.getOrderNumber(), sub, "/order");
    }

    // RepairRecord.java:80 getJobOrderNumber, :88/:96 getBrand/getModelName.
    private SearchSuggestion toSuggestion(RepairRecord repair) {
        String sub = (repair.getBrand() != null ? repair.getBrand() : "")
                + " " + (repair.getModelName() != null ? repair.getModelName() : "");
        return new SearchSuggestion("REPAIR", repair.getRepairId(), repair.getJobOrderNumber(), sub.trim(), "/repair");
    }

    // Appointment.java:99 getDeviceType, :107 getItemDescription.
    private SearchSuggestion toSuggestion(Appointment appointment) {
        return new SearchSuggestion("APPOINTMENT", appointment.getAppointmentId(),
                appointment.getDeviceType(), appointment.getItemDescription(), "/appointment");
    }

    // SavedBuild.java:34 getSavedBuildId, :50 getName.
    private SearchSuggestion toSuggestion(SavedBuild build) {
        return new SearchSuggestion("BUILD", build.getSavedBuildId(), build.getName(), "Saved build", "/build/my-builds");
    }

    private static final class MatchedRecords {
        final List<Order> orders;
        final List<RepairRecord> repairs;
        final List<Appointment> appointments;
        final List<SavedBuild> savedBuilds;

        MatchedRecords(List<Order> orders, List<RepairRecord> repairs,
                       List<Appointment> appointments, List<SavedBuild> savedBuilds) {
            this.orders = orders;
            this.repairs = repairs;
            this.appointments = appointments;
            this.savedBuilds = savedBuilds;
        }
    }

    // Order.java:18 orderNumber, :44 referenceNumber, :30 status; OrderItem.java:18-19 brand/modelName.
    private boolean orderMatches(Order order, String needle) {
        if (contains(order.getOrderNumber(), needle)) {
            return true;
        }
        if (contains(order.getReferenceNumber(), needle)) {
            return true;
        }
        if (order.getStatus() != null && contains(order.getStatus().name(), needle)) {
            return true;
        }
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (contains(item.getBrand(), needle) || contains(item.getModelName(), needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    // RepairRecord.java:28 jobOrderNumber, :30-33 deviceType/brand/modelName/serialNumber,
    // :36 issueDescription, :46 status.
    private boolean repairMatches(RepairRecord repair, String needle) {
        return contains(repair.getJobOrderNumber(), needle)
                || contains(repair.getDeviceType(), needle)
                || contains(repair.getBrand(), needle)
                || contains(repair.getModelName(), needle)
                || contains(repair.getSerialNumber(), needle)
                || contains(repair.getIssueDescription(), needle)
                || (repair.getStatus() != null && contains(repair.getStatus().name(), needle));
    }

    // Appointment.java:23 deviceType, :26 itemDescription, :38 issueDescription, status.
    private boolean appointmentMatches(Appointment appointment, String needle) {
        return contains(appointment.getDeviceType(), needle)
                || contains(appointment.getItemDescription(), needle)
                || contains(appointment.getIssueDescription(), needle)
                || (appointment.getStatus() != null && contains(appointment.getStatus().name(), needle));
    }

    // SavedBuild.java:25 name.
    private boolean buildMatches(SavedBuild build, String needle) {
        return contains(build.getName(), needle);
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}
