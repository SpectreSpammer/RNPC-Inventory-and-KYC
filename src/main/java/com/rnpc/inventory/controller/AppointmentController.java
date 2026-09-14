package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.AppointmentDto;
import com.rnpc.inventory.dto.CalendarDay;
import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.AppointmentService;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("appointment")
public class AppointmentController {

    private final ClientService clientService;
    private final AppointmentService appointmentService;
    private final RepairRecordService repairRecordService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public AppointmentController(ClientService clientService, AppointmentService appointmentService,
                                  RepairRecordService repairRecordService, UserService userService,
                                  NotificationService notificationService) {
        this.clientService = clientService;
        this.appointmentService = appointmentService;
        this.repairRecordService = repairRecordService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping({"", "/"})
    public String showAppointmentList(Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        List<Appointment> appointments;
        if (admin) {
            appointments = appointmentService.getAllAppointments();
        } else if (isSignedIn(authentication)) {
            appointments = appointmentService.getAppointmentsForUser(authentication.getName());
        } else {
            appointments = List.of();
        }
        model.addAttribute("appointments", appointments);
        model.addAttribute("isAdmin", admin);
        addLayoutAttributes(authentication, model);
        model.addAttribute("confirmedCount", appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.CONFIRMED).count());
        model.addAttribute("pendingCount", appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.PENDING).count());
        model.addAttribute("cancelledCount", appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.CANCELLED).count());
        return admin ? "appointments/adminAppointmentIndex" : "appointments/appointmentIndex";
    }

    @GetMapping("/create")
    public String showCreatePage(Authentication authentication, Model model) {
        addLayoutAttributes(authentication, model);
        model.addAttribute("appointmentDto", new AppointmentDto());
        return "appointments/appointmentCreate";
    }

    @PostMapping("/create")
    public String createAppointment(@Valid @ModelAttribute AppointmentDto appointmentDto, BindingResult result,
                                     Authentication authentication, Model model) {
        if (result.hasErrors()) {
            addLayoutAttributes(authentication, model);
            return "appointments/appointmentCreate";
        }

        Client client = clientService.findByContactNumber(appointmentDto.getContactNumber())
                .orElseGet(() -> {
                    ClientDto clientDto = new ClientDto();
                    clientDto.setFullName(appointmentDto.getFullName());
                    clientDto.setContactNumber(appointmentDto.getContactNumber());
                    clientDto.setEmail(appointmentDto.getEmail());
                    clientDto.setAddress(appointmentDto.getAddress());
                    return clientService.saveClient(clientDto);
                });

        if (isSignedIn(authentication)) {
            userService.findByUsername(authentication.getName())
                    .ifPresent(user -> clientService.linkToUser(client, user));
        }

        Appointment appointment = appointmentService.createAppointment(client, appointmentDto);
        repairRecordService.createFromAppointment(client, appointmentDto, appointment);
        notificationService.notifyAdmin(
                client.getFullName() + " requested " + appointment.getServiceLabel() + " for "
                        + appointmentDto.getPreferredDate() + " " + appointmentDto.getPreferredTime(),
                Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
        return "redirect:/appointment/" + appointment.getAppointmentId();
    }

    @GetMapping("/{id}")
    public String showAppointment(@PathVariable("id") Long id, Authentication authentication, Model model) {
        model.addAttribute("appointment", appointmentService.getAppointmentById(id));
        model.addAttribute("currentUsername", isSignedIn(authentication) ? authentication.getName() : null);
        addLayoutAttributes(authentication, model);
        return "appointments/appointmentView";
    }

    // Notifications (and any other list) fetch this fragment via JS and drop it into a modal
    // instead of navigating to the full page - only an admin or the appointment's own linked
    // account may fetch it.
    @GetMapping("/{id}/modal")
    public String showAppointmentModal(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        boolean owns = isSignedIn(authentication) && appointment.getClient().getUser() != null
                && appointment.getClient().getUser().getUsername().equals(authentication.getName());
        if (!isAdmin(authentication) && !owns) {
            return "redirect:/appointment";
        }
        model.addAttribute("appointment", appointment);
        model.addAttribute("currentUsername", isSignedIn(authentication) ? authentication.getName() : null);
        return "appointments/appointmentView :: appointmentCard";
    }

    // The customer's own follow-up upload if they skipped the receipt when booking (or an admin
    // asks for one to verify the reference number) - only the appointment's own linked account can
    // use it, checked server-side regardless of whether the button is visible. Mirrors
    // OrderController.uploadReceipt.
    @PutMapping("/uploadReceipt/{id}")
    public String uploadReceipt(@PathVariable("id") Long id,
                                 @RequestParam("receiptFile") MultipartFile receiptFile,
                                 Authentication authentication) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        boolean owns = isSignedIn(authentication) && appointment.getClient().getUser() != null
                && appointment.getClient().getUser().getUsername().equals(authentication.getName());
        if (owns) {
            appointmentService.updateReceipt(id, receiptFile);
            notificationService.notifyAdmin(
                    appointment.getClient().getFullName() + " uploaded a receipt for the appointment on "
                            + appointment.getPreferredDate() + " " + appointment.getPreferredTime() + ".",
                    Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
        }
        return "redirect:/appointment";
    }

    // Admin nudge for a customer who hasn't uploaded a receipt yet - just fires a notification,
    // doesn't change appointment state. Mirrors OrderController.requestReceipt.
    @PutMapping("/requestReceipt/{id}")
    @ResponseBody
    public ResponseEntity<Void> requestReceipt(@PathVariable("id") Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        Appointment appointment = appointmentService.getAppointmentById(id);
        notificationService.notifyCustomer(appointment.getClient().getUser(),
                "Please upload your receipt for the appointment on " + appointment.getPreferredDate() + " "
                        + appointment.getPreferredTime() + " so we can verify your payment.",
                Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/confirm/{id}")
    public String confirmAppointment(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            String verifiedByEmployeeId = userService.findByUsername(authentication.getName())
                    .map(User::getEmployeeLabel)
                    .orElse(null);
            appointmentService.confirmAppointment(id, verifiedByEmployeeId);
            Appointment appointment = appointmentService.getAppointmentById(id);
            notificationService.notifyCustomer(appointment.getClient().getUser(),
                    "Your " + appointment.getServiceLabel() + " appointment on " + appointment.getPreferredDate()
                            + " " + appointment.getPreferredTime() + " has been confirmed.",
                    Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
        }
        return "redirect:/appointment";
    }

    @PutMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable("id") Long id, Authentication authentication) {
        boolean wasConfirmed = appointmentService.getAppointmentById(id).getStatus() == Appointment.Status.CONFIRMED;
        if (isAdmin(authentication)) {
            appointmentService.cancelAppointment(id);
            notifyCancellation(id, wasConfirmed);
        } else if (isSignedIn(authentication)) {
            appointmentService.cancelAppointmentAsCustomer(id, authentication.getName());
            if (appointmentService.getAppointmentById(id).getStatus() == Appointment.Status.CANCELLED) {
                notifyCancellation(id, wasConfirmed);
            }
        }
        return "redirect:/appointment";
    }

    // wasConfirmed is captured before the cancellation runs (see cancelAppointment above) so the
    // NOT_REFUNDABLE message can explain the actual reason - the appointment's own status is
    // already CANCELLED by the time this reads it back, so that context would otherwise be lost.
    // A customer's own self-cancel is only ever allowed >12h out (isCancellableByCustomer), so a
    // still-PENDING appointment always lands in the refundable branch below; NOT_REFUNDABLE only
    // happens once it was already CONFIRMED, or (for an admin cancelling a PENDING one) within 5h
    // of the slot.
    private void notifyCancellation(Long id, boolean wasConfirmed) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        String when = appointment.getPreferredDate() + " " + appointment.getPreferredTime();
        String message;
        if (appointment.getRefundStatus() == Appointment.RefundStatus.PENDING) {
            message = "Your appointment on " + when + " was cancelled. You're eligible for a refund of your "
                    + "reservation payment - we'll process it soon.";
        } else if (appointment.getRefundStatus() == Appointment.RefundStatus.NOT_REFUNDABLE) {
            message = wasConfirmed
                    ? "Your appointment on " + when + " was cancelled. Since the schedule was already confirmed, "
                            + "the reservation payment is not refundable."
                    : "Your appointment on " + when + " was cancelled. Since this was within 5 hours of the "
                            + "scheduled time, the reservation payment is not refundable.";
        } else {
            message = "Your appointment on " + when + " has been cancelled.";
        }
        notificationService.notifyCustomer(appointment.getClient().getUser(), message,
                Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
    }

    // Lets either an admin (full rights, no restrictions) or the appointment's own linked customer
    // (see AppointmentService.rescheduleAsCustomer for the ownership/12h eligibility rules) pick a
    // new date/time instead of cancelling outright. Either way it sends the appointment back to
    // PENDING; who gets notified about the change depends on who made it.
    @PutMapping("/reschedule/{id}")
    public String reschedule(@PathVariable("id") Long id,
                              @RequestParam("preferredDate") LocalDate preferredDate,
                              @RequestParam("preferredTime") LocalTime preferredTime,
                              Authentication authentication) {
        boolean admin = isAdmin(authentication);
        boolean rescheduled = admin
                ? appointmentService.rescheduleAsAdmin(id, preferredDate, preferredTime)
                : isSignedIn(authentication) && appointmentService.rescheduleAsCustomer(id, authentication.getName(),
                        preferredDate, preferredTime);

        if (rescheduled) {
            Appointment appointment = appointmentService.getAppointmentById(id);
            if (admin) {
                notificationService.notifyCustomer(appointment.getClient().getUser(),
                        "Your appointment has been rescheduled by the shop to " + preferredDate + " " + preferredTime
                                + " - awaiting confirmation.",
                        Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
            } else {
                notificationService.notifyAdmin(
                        appointment.getClient().getFullName() + " requested to reschedule their appointment to "
                                + preferredDate + " " + preferredTime + " - please re-confirm.",
                        Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
            }
        }
        return "redirect:/appointment";
    }

    @PutMapping("/markRefunded/{id}")
    public String markRefunded(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            String refundedByEmployeeId = userService.findByUsername(authentication.getName())
                    .map(User::getEmployeeLabel)
                    .orElse(null);
            appointmentService.markRefunded(id, refundedByEmployeeId);
            Appointment appointment = appointmentService.getAppointmentById(id);
            notificationService.notifyCustomer(appointment.getClient().getUser(),
                    "Your refund for the appointment on " + appointment.getPreferredDate() + " "
                            + appointment.getPreferredTime() + " has been processed.",
                    Notification.EntityType.APPOINTMENT, appointment.getAppointmentId());
        }
        return "redirect:/appointment";
    }

    // Shared app shell: the admin audience and the customer inbox follow OrderController.
    // This supplies presentation data only, including the booking validation-error response.
    private void addLayoutAttributes(Authentication authentication, Model model) {
        model.addAttribute("deviceCategories", Appointment.DeviceCategory.values());
        boolean signedIn = isSignedIn(authentication);
        boolean admin = isAdmin(authentication);
        String username = signedIn ? authentication.getName() : null;
        model.addAttribute("currentUsername", username);
        model.addAttribute("currentRole", admin ? "Admin" : (signedIn ? "Customer" : "Guest"));
        model.addAttribute("unreadNotifications", !signedIn ? 0 : (admin
                ? notificationService.getUnreadCountForAdmin()
                : notificationService.getUnreadCountForUser(username)));
        List<Notification> recent = !signedIn ? List.of() : (admin
                ? notificationService.getForAdmin() : notificationService.getForUser(username));
        model.addAttribute("recentNotifications", recent.stream().limit(15).toList());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @GetMapping("/calendar")
    public String showCalendar(@RequestParam(value = "year", required = false) Integer year,
                                @RequestParam(value = "month", required = false) Integer month,
                                Authentication authentication, Model model) {
        addLayoutAttributes(authentication, model);
        LocalDate today = LocalDate.now();
        YearMonth current = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.of(today.getYear(), today.getMonth());

        model.addAttribute("weeks", partitionIntoWeeks(
                appointmentService.getMonthGrid(current.getYear(), current.getMonthValue())));
        model.addAttribute("monthLabel", current.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + current.getYear());
        model.addAttribute("year", current.getYear());
        model.addAttribute("month", current.getMonthValue());

        YearMonth prev = current.minusMonths(1);
        YearMonth next = current.plusMonths(1);
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        model.addAttribute("todayYear", today.getYear());
        model.addAttribute("todayMonth", today.getMonthValue());
        return "appointments/appointmentCalendar";
    }

    private List<List<CalendarDay>> partitionIntoWeeks(List<CalendarDay> days) {
        List<List<CalendarDay>> weeks = new ArrayList<>();
        for (int i = 0; i < days.size(); i += 7) {
            weeks.add(days.subList(i, Math.min(i + 7, days.size())));
        }
        return weeks;
    }
}
