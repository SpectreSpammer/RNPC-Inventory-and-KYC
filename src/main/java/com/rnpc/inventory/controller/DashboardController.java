package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.SavedBuildItemView;
import com.rnpc.inventory.entity.Appointment;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.SavedBuild;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.AppointmentService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.OrderService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.SavedBuildService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// Renders the customer dashboard (converted from the approved gpt-preview.html mockup - see
// fragments/layout-app.html) at the app's root, replacing static/index.html for signed-in
// customers only. Admins and anonymous visitors still get exactly today's existing experience
// (static/index.html) - only the CUSTOMER home page is being converted this round; nothing about
// the admin/anonymous path changes.
@Controller
public class DashboardController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final OrderService orderService;
    private final AppointmentService appointmentService;
    private final RepairRecordService repairRecordService;
    private final SavedBuildService savedBuildService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public DashboardController(OrderService orderService, AppointmentService appointmentService,
                                RepairRecordService repairRecordService, SavedBuildService savedBuildService,
                                UserService userService, NotificationService notificationService) {
        this.orderService = orderService;
        this.appointmentService = appointmentService;
        this.repairRecordService = repairRecordService;
        this.savedBuildService = savedBuildService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping({"", "/"})
    public String showHome(Authentication authentication, Model model) {
        if (!isSignedIn(authentication) || isAdmin(authentication)) {
            return "forward:/index.html";
        }

        String username = authentication.getName();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<Order> orders = orderService.getOrdersForUser(username);
        List<Appointment> appointments = appointmentService.getAppointmentsForUser(username);
        List<RepairRecord> repairs = repairRecordService.getRepairRecordsForUser(username);

        model.addAttribute("currentUsername", resolveDisplayName(authentication, username));
        model.addAttribute("currentRole", "Customer");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForUser(username));
        // Topbar notification dropdown - NotificationService.java:55-57 (getForUser) already
        // returns newest-first; just trims to a short list for the dropdown, no new query.
        model.addAttribute("recentNotifications",
                notificationService.getForUser(username).stream().limit(15).collect(Collectors.toList()));

        model.addAttribute("greeting", greetingFor(now.toLocalTime()));
        model.addAttribute("todayLabel", today.format(DATE_FMT));
        model.addAttribute("todayDayOfWeek", today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        model.addAttribute("nowLabel", now.toLocalTime().format(TIME_FMT));

        // Stat cards: real counts only - no invented week-over-week trend percentages (this app
        // has no such metric anywhere yet), so trend is omitted rather than fabricated.
        model.addAttribute("totalOrders", orders.size());

        // Total Orders footer - not CANCELLED/CANCELLATION_REQUESTED, from the same already-scoped
        // `orders` list above, no new repository call.
        long activeOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.AWAITING_PAYMENT || o.getStatus() == Order.OrderStatus.PAID)
                .count();
        model.addAttribute("activeOrders", activeOrders);

        // Scheduled Appointments footer reuses this same count directly (see `upcoming` below,
        // filtered identically) - no second count computed for it.
        long scheduledAppointments = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.Status.CANCELLED)
                .filter(a -> !a.getPreferredDate().isBefore(today))
                .count();
        model.addAttribute("scheduledAppointments", scheduledAppointments);

        long inRepair = repairs.stream()
                .filter(r -> r.getStatus() == RepairRecord.RepairStatus.IN_PROGRESS)
                .count();
        model.addAttribute("inRepairCount", inRepair);

        // Completed Services footer - lifetime total, no date filter, same already-scoped
        // `repairs` list above, no new repository call.
        long completedServices = repairs.stream()
                .filter(r -> r.getStatus() == RepairRecord.RepairStatus.COMPLETED
                        || r.getStatus() == RepairRecord.RepairStatus.RELEASED)
                .count();
        model.addAttribute("completedServices", completedServices);

        // Upcoming Appointments: not cancelled, today or later, soonest first, capped at 5 rows.
        List<Appointment> upcoming = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.Status.CANCELLED)
                .filter(a -> !a.getPreferredDate().isBefore(today))
                .sorted(Comparator.comparing(Appointment::getPreferredDate).thenComparing(Appointment::getPreferredTime))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("upcomingAppointments", upcoming);

        // My Repairs: most recently updated repair records, capped at 5 rows.
        List<RepairRecord> myRepairs = repairs.stream()
                .sorted(Comparator.comparing(RepairRecord::getRepairId).reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("myRepairs", myRepairs);

        // Recent Orders: most recent 5 (getOrdersForUser already orders by orderId desc - see
        // OrderRepository.findByClient_User_UsernameOrderByOrderIdDesc).
        model.addAttribute("recentOrders", orders.stream().limit(5).collect(Collectors.toList()));

        // Warranty Tracker: only repairs with a real warrantyEndDate (RepairRecord.java:42) -
        // most urgent (soonest expiring, expired last) first, capped at 5 rows.
        List<RepairRecord> withWarranty = repairs.stream()
                .filter(r -> r.getWarrantyEndDate() != null)
                .sorted(Comparator.comparing(r -> toLocalDate(r.getWarrantyEndDate())))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("warrantyRepairs", withWarranty);

        // Build Progress: the user's most recently saved build (SavedBuildRepository.java:10-11 -
        // findByUser_UsernameOrderBySavedBuildIdDesc - already DESC by id, so index 0 is newest),
        // reusing SavedBuildService.viewItems (SavedBuildService.java:142-150) rather than any new
        // query. null when the user has never saved one - see "No saved builds" branch below.
        List<SavedBuild> savedBuilds = savedBuildService.getSavedBuildsForUser(username);
        SavedBuild latestSavedBuild = savedBuilds.isEmpty() ? null : savedBuilds.get(0);
        model.addAttribute("latestSavedBuild", latestSavedBuild);

        if (latestSavedBuild != null) {
            Map<String, SavedBuildItemView> byCategory = new LinkedHashMap<>();
            for (SavedBuildItemView view : savedBuildService.viewItems(latestSavedBuild)) {
                byCategory.put(view.getCategory(), view);
            }

            // Full 8-row spec list for this card (unlike My Builds' 5-row summary card) - Storage
            // shows whichever of STORAGE_SSD/STORAGE_HDD is present (SSD preferred if a build
            // somehow has both), same collapsing rule SavedBuildService.specRows uses.
            LinkedHashMap<String, SavedBuildItemView> specRows = new LinkedHashMap<>();
            specRows.put("CPU", byCategory.get("CPU"));
            specRows.put("Motherboard", byCategory.get("MOTHERBOARD"));
            specRows.put("RAM", byCategory.get("RAM"));
            specRows.put("Storage", byCategory.containsKey("STORAGE_SSD") ? byCategory.get("STORAGE_SSD") : byCategory.get("STORAGE_HDD"));
            specRows.put("PSU", byCategory.get("PSU"));
            specRows.put("Case", byCategory.get("CASE"));
            specRows.put("Cooler", byCategory.get("COOLER"));
            specRows.put("GPU", byCategory.get("GPU"));
            model.addAttribute("latestBuildSpecRows", specRows);

            model.addAttribute("latestBuildCompletionPercent", buildCompletionPercent(byCategory));
        }

        // Build Progress card, state 1: the most recent order (orders is already orderId DESC -
        // see getOrdersForUser) with a build timeline started but not yet handed over to the
        // customer. Priority order below (ACTIVE_ORDER > SAVED_BUILD > EMPTY) is resolved here,
        // as one attribute, so dashboard.html branches once via th:switch instead of testing
        // latestSavedBuild/activeBuildOrder nulls itself.
        Order activeBuildOrder = orders.stream()
                .filter(o -> o.getBuildStage() != null && o.getBuildStage() != Order.BuildStage.COMPLETED)
                .findFirst()
                .orElse(null);

        String buildProgressState;
        if (activeBuildOrder != null) {
            buildProgressState = "ACTIVE_ORDER";
            model.addAttribute("activeBuildOrder", activeBuildOrder);

            // Parts orders never see ASSEMBLY_IN_PROGRESS/TESTING - same rule
            // OrderController.updateBuildStage enforces server-side.
            List<Order.BuildStage> stageOrder = OrderService.isFullBuild(activeBuildOrder)
                    ? List.of(Order.BuildStage.ORDER_CONFIRMED, Order.BuildStage.COMPONENTS_RESERVED,
                            Order.BuildStage.ASSEMBLY_IN_PROGRESS, Order.BuildStage.TESTING, Order.BuildStage.READY)
                    : List.of(Order.BuildStage.ORDER_CONFIRMED, Order.BuildStage.COMPONENTS_RESERVED, Order.BuildStage.READY);
            int currentIndex = stageOrder.indexOf(activeBuildOrder.getBuildStage());

            List<BuildStageRow> stageRows = new ArrayList<>();
            for (int i = 0; i < stageOrder.size(); i++) {
                Order.BuildStage stage = stageOrder.get(i);
                String rowStatus = i < currentIndex ? "COMPLETED" : (i == currentIndex ? "IN_PROGRESS" : "PENDING");
                // Every row (not just READY) goes through buildStageLabel, so READY reads "Ready
                // for Pickup"/"Out for Delivery" per the order - same helper OrderController's
                // notification and adminOrderIndex.html's dropdown already use.
                stageRows.add(new BuildStageRow(stage.name(),
                        OrderService.buildStageLabel(stage, activeBuildOrder.getFulfilmentMethod()), rowStatus));
            }
            model.addAttribute("activeBuildStages", stageRows);
            model.addAttribute("activeBuildStagesDone", currentIndex);
            model.addAttribute("activeBuildStagesTotal", stageOrder.size());
            model.addAttribute("activeBuildCurrentStageLabel",
                    OrderService.buildStageLabel(activeBuildOrder.getBuildStage(), activeBuildOrder.getFulfilmentMethod()));
        } else if (latestSavedBuild != null) {
            buildProgressState = "SAVED_BUILD";
        } else {
            buildProgressState = "EMPTY";
        }
        model.addAttribute("buildProgressState", buildProgressState);

        return "dashboard";
    }

    // Build Progress card, state 1 row (see showHome above) - stageName is the raw BuildStage enum
    // name (unused by the template today, kept for parity with how every other stage identifier
    // in this codebase is carried), label is already resolved via OrderService.buildStageLabel,
    // status is one of COMPLETED/IN_PROGRESS/PENDING.
    private record BuildStageRow(String stageName, String label, String status) {
    }

    // Warranty urgency + bar percentage - both derived from real RepairRecord.repairDate
    // (RepairRecord.java:41) and RepairRecord.warrantyEndDate (RepairRecord.java:42), no
    // invented fields. Static so the Thymeleaf template can call them directly via
    // T(com.rnpc.inventory.controller.DashboardController).warrantyUrgency(...).
    public static String warrantyUrgency(RepairRecord repair) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(repair.getWarrantyEndDate()));
        if (daysRemaining < 0) return "expired";
        return daysRemaining <= 30 ? "amber" : "green";
    }

    public static long warrantyDaysRemaining(RepairRecord repair) {
        return ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(repair.getWarrantyEndDate()));
    }

    // Bar fill floor (out of 100) - a warranty with only a sliver of real time left would
    // otherwise round to a rect too thin to see next to its own rx=3.5 corner radius (see
    // dashboard.html's warranty-bar-fill). Only applied while genuinely still active; a truly
    // expired warranty (0% remaining) still renders fully empty, matching "drains to empty".
    private static final int WARRANTY_BAR_MIN_PERCENT = 8;

    // Percent of the warranty period still REMAINING (100 when fresh, draining toward 0 at
    // expiry) - the bar shows time left, not time used, so a brand-new warranty reads as a full
    // bar instead of an invisible one. Renamed from warrantyElapsedPercent (which returned the
    // inverse - percent elapsed - and made a fresh warranty's fill nearly invisible since elapsed
    // starts near 0); same two fallback cases below still mean "treat as worst case; no time
    // left" (0), just inverted along with everything else. Single call site: dashboard.html.
    public static int warrantyRemainingPercent(RepairRecord repair) {
        if (repair.getRepairDate() == null) return 0;
        LocalDate start = toLocalDate(repair.getRepairDate());
        LocalDate end = toLocalDate(repair.getWarrantyEndDate());
        long totalDays = ChronoUnit.DAYS.between(start, end);
        if (totalDays <= 0) return 0;
        long elapsed = ChronoUnit.DAYS.between(start, LocalDate.now());
        int elapsedPercent = Math.max(0, Math.min(100, (int) Math.round((elapsed * 100.0) / totalDays)));
        int remainingPercent = 100 - elapsedPercent;
        if (remainingPercent > 0 && remainingPercent < WARRANTY_BAR_MIN_PERCENT) {
            remainingPercent = WARRANTY_BAR_MIN_PERCENT;
        }
        return remainingPercent;
    }

    // The 7 categories a build needs before it's "complete" - GPU and a second storage slot
    // (STORAGE_SSD/STORAGE_HDD) are real, save-able categories (see buildPc.html's CATEGORY_ORDER)
    // but optional, so they're shown in the spec list without counting toward this percentage.
    private static int buildCompletionPercent(Map<String, SavedBuildItemView> byCategory) {
        // Shared with OrderService.isFullBuild's full-build-vs-parts-order check - one definition
        // of the 7 required categories, see OrderService.countFilledBuildSlots.
        int filled = OrderService.countFilledBuildSlots(byCategory.keySet());
        return (int) Math.round((filled * 100.0) / 7);
    }

    private static LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String greetingFor(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) return "Good morning,";
        if (time.isBefore(LocalTime.of(18, 0))) return "Good afternoon,";
        return "Good evening,";
    }

    // Same priority order as LoginController.authStatus (profile fullName > OAuth2 given_name >
    // raw username) - replicated here rather than calling into LoginController, since touching
    // an existing controller for a different page is out of scope for this pass.
    private String resolveDisplayName(Authentication authentication, String username) {
        String displayName = username;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String givenName = oAuth2User.getAttribute("given_name");
            if (givenName != null && !givenName.isBlank()) {
                displayName = givenName;
            }
        }
        String profileName = userService.findByUsername(username).map(User::getFullName).orElse(null);
        if (profileName != null && !profileName.isBlank()) {
            displayName = profileName;
        }
        return displayName;
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
}
