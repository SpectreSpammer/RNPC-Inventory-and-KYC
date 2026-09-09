package com.rnpc.inventory.dto;

import com.rnpc.inventory.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

// One cell of the month grid rendered by appointmentCalendar.html - always carries a real date
// (including the leading/trailing days borrowed from the previous/next month to fill the grid),
// with inMonth telling the template whether to grey it out.
public class CalendarDay {

    private final LocalDate date;
    private final boolean inMonth;
    private final boolean today;
    private final List<Appointment> appointments;

    public CalendarDay(LocalDate date, boolean inMonth, boolean today, List<Appointment> appointments) {
        this.date = date;
        this.inMonth = inMonth;
        this.today = today;
        this.appointments = appointments;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isInMonth() {
        return inMonth;
    }

    public boolean isToday() {
        return today;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public boolean isBusy() {
        return appointments.stream().anyMatch(a -> a.getStatus() != Appointment.Status.CANCELLED);
    }
}
