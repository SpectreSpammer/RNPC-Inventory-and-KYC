package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.SavedBuild;

import java.util.List;

/**
 * Per-type capped result lists (SearchService caps each at 20) plus each type's real total match
 * count, so the page can say "Showing 20 of 47" when there's more than fits. Every list here was
 * already scoped to one signed-in customer's own records before SearchService ever saw it - see
 * SearchService.search.
 */
public class SearchResults {

    private final List<Order> orders;
    private final int orderTotal;
    private final List<RepairRecord> repairs;
    private final int repairTotal;
    private final List<Appointment> appointments;
    private final int appointmentTotal;
    private final List<SavedBuild> savedBuilds;
    private final int savedBuildTotal;

    public SearchResults(List<Order> orders, int orderTotal,
                          List<RepairRecord> repairs, int repairTotal,
                          List<Appointment> appointments, int appointmentTotal,
                          List<SavedBuild> savedBuilds, int savedBuildTotal) {
        this.orders = orders;
        this.orderTotal = orderTotal;
        this.repairs = repairs;
        this.repairTotal = repairTotal;
        this.appointments = appointments;
        this.appointmentTotal = appointmentTotal;
        this.savedBuilds = savedBuilds;
        this.savedBuildTotal = savedBuildTotal;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public int getOrderTotal() {
        return orderTotal;
    }

    public List<RepairRecord> getRepairs() {
        return repairs;
    }

    public int getRepairTotal() {
        return repairTotal;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public int getAppointmentTotal() {
        return appointmentTotal;
    }

    public List<SavedBuild> getSavedBuilds() {
        return savedBuilds;
    }

    public int getSavedBuildTotal() {
        return savedBuildTotal;
    }

    public int getTotalCount() {
        return orderTotal + repairTotal + appointmentTotal + savedBuildTotal;
    }

    public boolean isEmpty() {
        return getTotalCount() == 0;
    }
}
