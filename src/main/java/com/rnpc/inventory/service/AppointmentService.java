package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.AppointmentDto;
import com.rnpc.inventory.dto.CalendarDay;
import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository repo;

    @Autowired
    public AppointmentService(AppointmentRepository repo) {
        this.repo = repo;
    }

    public List<Appointment> getAllAppointments() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "appointmentId"));
    }

    public Appointment getAppointmentById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));
    }

    public Appointment createAppointment(Client client, AppointmentDto dto) {
        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setDeviceType(dto.getDeviceType());
        appointment.setItemDescription(dto.getItemDescription());
        appointment.setHistory(dto.getHistory());
        appointment.setPreferredDate(dto.getPreferredDate());
        appointment.setPreferredTime(dto.getPreferredTime());
        appointment.setIssueDescription(dto.getIssueDescription());
        appointment.setPaymentMethod(com.rnpc.inventory.entity.Order.PaymentMethod.valueOf(dto.getPaymentMethod()));
        appointment.setReferenceNumber(dto.getReferenceNumber());
        appointment.setReceiptFileName(handleFileUpload(dto.getReceiptFile()));
        appointment.setStatus(Appointment.Status.PENDING);
        appointment.setCreatedAt(new Date());
        return repo.save(appointment);
    }

    // The customer's own follow-up upload if they skipped the receipt when booking (or an admin
    // asks for one to verify the reference number) - mirrors OrderService.updateReceipt.
    public void updateReceipt(Long id, MultipartFile receiptFile) {
        if (receiptFile == null || receiptFile.isEmpty()) {
            return;
        }
        Appointment appointment = getAppointmentById(id);
        deleteImageFile(appointment.getReceiptFileName());
        appointment.setReceiptFileName(handleFileUpload(receiptFile));
        repo.save(appointment);
    }

    private void deleteImageFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get("public/images/" + fileName));
            } catch (Exception ex) {
                System.out.println("Error deleting file: " + ex.getMessage());
            }
        }
    }

    private String handleFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = new Date().getTime() + "_" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file: " + ex.getMessage());
        }
    }

    // Confirming an appointment that has a reservation payment attached also stands in for
    // validating that payment (there's no separate "Mark Paid" step for appointments the way
    // there is for orders) - so it gets the same verifiedByEmployeeId/verifiedAt attribution.
    public void confirmAppointment(Long id, String verifiedByEmployeeId) {
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus(Appointment.Status.CONFIRMED);
        if (appointment.getPaymentMethod() != null) {
            appointment.setVerifiedByEmployeeId(verifiedByEmployeeId);
            appointment.setVerifiedAt(new Date());
        }
        repo.save(appointment);
    }

    public void cancelAppointment(Long id) {
        Appointment appointment = getAppointmentById(id);
        applyCancellation(appointment);
        repo.save(appointment);
    }

    public List<Appointment> getAppointmentsForUser(String username) {
        return repo.findByClient_User_UsernameOrderByPreferredDateDescPreferredTimeDesc(username);
    }

    // Customer-triggered cancel: only allowed on an appointment their own account is linked to,
    // and only outside the 12-hour cutoff (see Appointment.isCancellableByCustomer) - anything
    // else is silently ignored rather than erroring, matching how the rest of this app handles
    // an ineligible action (the button simply isn't shown for it in the template either).
    public void cancelAppointmentAsCustomer(Long id, String username) {
        Appointment appointment = getAppointmentById(id);
        boolean owns = appointment.getClient().getUser() != null
                && username.equals(appointment.getClient().getUser().getUsername());
        if (owns && appointment.isCancellableByCustomer()) {
            applyCancellation(appointment);
            repo.save(appointment);
        }
    }

    // Sets CANCELLED and, if a reservation payment was ever attached, decides refund eligibility.
    // Once the shop has confirmed a slot it's committed the resources for it, so a CONFIRMED
    // appointment is never refundable regardless of how far out it still is. A PENDING appointment
    // (never confirmed) still follows the 5-hour timer: more than 5 hours out -> PENDING (owed a
    // manual refund - see markRefunded); 5 hours or less -> NOT_REFUNDABLE.
    private void applyCancellation(Appointment appointment) {
        boolean hadPayment = appointment.getPaymentMethod() != null;
        boolean refundEligible = appointment.getStatus() != Appointment.Status.CONFIRMED
                && appointment.isRefundEligibleNow();
        appointment.setStatus(Appointment.Status.CANCELLED);
        if (hadPayment) {
            appointment.setRefundStatus(refundEligible
                    ? Appointment.RefundStatus.PENDING
                    : Appointment.RefundStatus.NOT_REFUNDABLE);
        }
    }

    // A customer picking a new date/time for their own appointment - only allowed under the same
    // conditions as a self-cancel (owns it, not already cancelled, still outside the 12h cutoff),
    // since rescheduling is really "cancel this slot, ask for a different one" from the shop's side.
    // Sends it back to PENDING so an admin re-confirms the new slot; any reservation payment already
    // made carries over untouched.
    public boolean rescheduleAsCustomer(Long id, String username, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = getAppointmentById(id);
        boolean owns = appointment.getClient().getUser() != null
                && username.equals(appointment.getClient().getUser().getUsername());
        if (owns && appointment.getStatus() != Appointment.Status.CANCELLED
                && appointment.isCancellableByCustomer() && !newDate.isBefore(LocalDate.now())) {
            appointment.setPreferredDate(newDate);
            appointment.setPreferredTime(newTime);
            appointment.setStatus(Appointment.Status.PENDING);
            repo.save(appointment);
            return true;
        }
        return false;
    }

    // Admin-triggered reschedule: no ownership check and no 12h cutoff (see rescheduleAsCustomer) -
    // an admin has full rights to move any non-cancelled appointment to a new slot. Also sends it
    // back to PENDING so the change is visible as needing a fresh confirmation, same as the
    // customer path.
    public boolean rescheduleAsAdmin(Long id, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = getAppointmentById(id);
        if (appointment.getStatus() != Appointment.Status.CANCELLED && !newDate.isBefore(LocalDate.now())) {
            appointment.setPreferredDate(newDate);
            appointment.setPreferredTime(newTime);
            appointment.setStatus(Appointment.Status.PENDING);
            repo.save(appointment);
            return true;
        }
        return false;
    }

    public void markRefunded(Long id, String refundedByEmployeeId) {
        Appointment appointment = getAppointmentById(id);
        appointment.setRefundStatus(Appointment.RefundStatus.REFUNDED);
        appointment.setRefundedByEmployeeId(refundedByEmployeeId);
        appointment.setRefundedAt(new Date());
        repo.save(appointment);
    }

    // Builds a full 6x7 month grid (Sun-Sat), borrowing leading/trailing days from the adjacent
    // months so every row is complete - the mock-up calendar renders this directly, one cell per
    // day, so it never has to special-case a short first/last week.
    public List<CalendarDay> getMonthGrid(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstOfMonth = yearMonth.atDay(1);
        LocalDate lastOfMonth = yearMonth.atEndOfMonth();

        LocalDate gridStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek() == DayOfWeek.SUNDAY
                ? 0 : firstOfMonth.getDayOfWeek().getValue());
        LocalDate gridEnd = gridStart.plusDays(41); // 6 weeks - 1 day

        Map<LocalDate, List<Appointment>> byDate = repo
                .findByPreferredDateBetweenOrderByPreferredDateAscPreferredTimeAsc(gridStart, gridEnd)
                .stream()
                .collect(Collectors.groupingBy(Appointment::getPreferredDate));

        LocalDate today = LocalDate.now();
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            boolean inMonth = !date.isBefore(firstOfMonth) && !date.isAfter(lastOfMonth);
            days.add(new CalendarDay(date, inMonth, date.equals(today),
                    byDate.getOrDefault(date, List.of())));
        }
        return days;
    }
}
