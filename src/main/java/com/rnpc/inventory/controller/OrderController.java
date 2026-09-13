package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.CheckoutDto;
import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.OrderService;
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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("order")
public class OrderController {

    private final OrderService orderService;
    private final ClientService clientService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public OrderController(OrderService orderService, ClientService clientService, UserService userService,
                            NotificationService notificationService) {
        this.orderService = orderService;
        this.clientService = clientService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    // Non-admin visibility mirrors AppointmentController.showAppointmentList and
    // RepairRecordController.showRepairList: a signed-in customer only ever sees Orders tied to
    // their own linked Client(s), never the full getAllOrders() list, and clientId (an
    // admin-only drill-down param) is ignored entirely for non-admins rather than trusted from
    // the query string - otherwise every order in the shop, including other customers' receipts
    // and reference numbers, was visible to anyone who loaded /order (even signed out).
    @GetMapping({"", "/"})
    public String showOrderList(@RequestParam(value = "clientId", required = false) Long clientId,
                                 Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        List<Order> orders;
        if (admin) {
            orders = clientId != null
                    ? orderService.getOrdersByClient(clientId)
                    : orderService.getAllOrders();
        } else if (isSignedIn(authentication)) {
            clientId = null;
            orders = orderService.getOrdersForUser(authentication.getName());
        } else {
            clientId = null;
            orders = List.of();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("clientId", clientId);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("currentUsername", isSignedIn(authentication) ? authentication.getName() : null);
        if (admin && clientId != null) {
            model.addAttribute("client", clientService.getClientById(clientId));
        }

        // Topbar chrome for the shared layout-app shell - same admin/customer split
        // notifyAdmin/notifyCustomer already use (NotificationService.java:51-57) and
        // RepairRecordController already follows: admin sees the shared admin inbox, a signed-in
        // customer sees their own.
        if (isSignedIn(authentication)) {
            String username = authentication.getName();
            model.addAttribute("currentRole", admin ? "Admin" : "Customer");
            model.addAttribute("unreadNotifications",
                    admin ? notificationService.getUnreadCountForAdmin() : notificationService.getUnreadCountForUser(username));
            List<Notification> recent = admin ? notificationService.getForAdmin() : notificationService.getForUser(username);
            model.addAttribute("recentNotifications", recent.stream().limit(15).collect(Collectors.toList()));
        }

        // Stat cards + Status column - shared between both templates, all derived from the
        // `orders` list already fetched above, no new repository call.
        long paidOrderCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PAID).count();
        model.addAttribute("paidOrderCount", paidOrderCount);
        long awaitingPaymentCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.AWAITING_PAYMENT).count();
        model.addAttribute("awaitingPaymentCount", awaitingPaymentCount);

        List<Order> inProgressOrders = orders.stream()
                .filter(o -> o.getBuildStage() != null && o.getBuildStage() != Order.BuildStage.COMPLETED)
                .collect(Collectors.toList());
        model.addAttribute("inProgressOrderCount", inProgressOrders.size());
        Order mostAdvanced = inProgressOrders.stream()
                .max(Comparator.comparingInt(o -> o.getBuildStage().ordinal()))
                .orElse(null);
        model.addAttribute("mostAdvancedStageLabel", mostAdvanced == null ? null
                : OrderService.buildStageLabel(mostAdvanced.getBuildStage(), mostAdvanced.getFulfilmentMethod()));

        // Per-row Status label for orders that have a build timeline - both templates show this
        // instead of the raw order status, via OrderService.buildStageLabel. Precomputed here
        // (keyed by orderId) rather than called via T(...) from either template, per the DevTools
        // stale-class failure already hit with T(...) on the admin page.
        Map<Long, String> buildStageLabels = orders.stream()
                .filter(o -> o.getBuildStage() != null)
                .collect(Collectors.toMap(Order::getOrderId,
                        o -> OrderService.buildStageLabel(o.getBuildStage(), o.getFulfilmentMethod())));
        model.addAttribute("buildStageLabels", buildStageLabels);

        if (admin) {
            // Revenue - PAID orders only, distinct from the customer page's Total Spent (all
            // non-cancelled orders, since a customer's own "spent" includes what they're still
            // waiting to have verified).
            double revenue = orders.stream()
                    .filter(o -> o.getStatus() == Order.OrderStatus.PAID)
                    .mapToDouble(Order::getTotalAmount)
                    .sum();
            model.addAttribute("revenue", revenue);

            // Build-stage radio options for the admin stage-update modal - one list per order,
            // already filtered to the stages valid for that order (ASSEMBLY_IN_PROGRESS/TESTING
            // excluded for a parts-only order, same rule updateBuildStage enforces server-side),
            // with each stage's label pre-resolved. Precomputed here rather than calling
            // OrderService.isFullBuild/buildStageLabel via T(...) from the template, same reason
            // as buildStageLabels above.
            Map<Long, List<StageOption>> stageOptionsByOrder = orders.stream()
                    .filter(o -> o.getBuildStage() != null)
                    .collect(Collectors.toMap(Order::getOrderId, o -> {
                        List<Order.BuildStage> validStages = OrderService.isFullBuild(o)
                                ? List.of(Order.BuildStage.ORDER_CONFIRMED, Order.BuildStage.COMPONENTS_RESERVED,
                                        Order.BuildStage.ASSEMBLY_IN_PROGRESS, Order.BuildStage.TESTING,
                                        Order.BuildStage.READY, Order.BuildStage.COMPLETED)
                                : List.of(Order.BuildStage.ORDER_CONFIRMED, Order.BuildStage.COMPONENTS_RESERVED,
                                        Order.BuildStage.READY, Order.BuildStage.COMPLETED);
                        return validStages.stream()
                                .map(stage -> new StageOption(stage.name(),
                                        OrderService.buildStageLabel(stage, o.getFulfilmentMethod())))
                                .collect(Collectors.toList());
                    }));
            model.addAttribute("stageOptionsByOrder", stageOptionsByOrder);
        } else {
            long activeOrderCount = orders.stream()
                    .filter(o -> o.getStatus() == Order.OrderStatus.AWAITING_PAYMENT || o.getStatus() == Order.OrderStatus.PAID)
                    .count();
            model.addAttribute("activeOrderCount", activeOrderCount);

            double totalSpent = orders.stream()
                    .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                    .mapToDouble(Order::getTotalAmount)
                    .sum();
            model.addAttribute("totalSpent", totalSpent);
        }

        return admin ? "orders/adminOrderIndex" : "orders/orderIndex";
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

    @PostMapping("/review")
    public String reviewOrder(@RequestParam(required = false) Integer cpuId,
                               @RequestParam(required = false) Integer gpuId,
                               @RequestParam(required = false) Integer motherboardId,
                               @RequestParam(required = false) Integer ramId,
                               @RequestParam(required = false) Integer storageSsdId,
                               @RequestParam(required = false) Integer storageHddId,
                               @RequestParam(required = false) Integer psuId,
                               @RequestParam(required = false) Integer caseId,
                               @RequestParam(required = false) Integer coolerId,
                               Model model) {
        Map<String, Integer> selections = buildSelections(cpuId, gpuId, motherboardId, ramId,
                storageSsdId, storageHddId, psuId, caseId, coolerId);
        if (selections.isEmpty()) {
            return "redirect:/build";
        }

        loadCheckoutModel(model, selections);
        model.addAttribute("checkoutDto", new CheckoutDto());
        return "orders/orderCheckout";
    }

    @PostMapping("/place")
    public String placeOrder(@Valid @ModelAttribute CheckoutDto checkoutDto, BindingResult result,
                              @RequestParam(required = false) Integer cpuId,
                              @RequestParam(required = false) Integer gpuId,
                              @RequestParam(required = false) Integer motherboardId,
                              @RequestParam(required = false) Integer ramId,
                              @RequestParam(required = false) Integer storageSsdId,
                              @RequestParam(required = false) Integer storageHddId,
                              @RequestParam(required = false) Integer psuId,
                              @RequestParam(required = false) Integer caseId,
                              @RequestParam(required = false) Integer coolerId,
                              Authentication authentication,
                              Model model) {
        Map<String, Integer> selections = buildSelections(cpuId, gpuId, motherboardId, ramId,
                storageSsdId, storageHddId, psuId, caseId, coolerId);

        if (selections.isEmpty()) {
            return "redirect:/build";
        }

        // Address is only truly required for Delivery - CheckoutDto itself can't express that
        // conditionally (see CheckoutDto.address), so it's checked here, after @Valid runs but
        // before checking result.hasErrors(), and rejected onto the same "address" field a plain
        // @NotEmpty would have used - orderCheckout.html's existing th:errors="*{address}" picks
        // it up with no template change needed.
        if ("DELIVERY".equals(checkoutDto.getFulfilmentMethod())
                && (checkoutDto.getAddress() == null || checkoutDto.getAddress().trim().isEmpty())) {
            result.rejectValue("address", "address.required.delivery", "The address is required for delivery orders!");
        }

        if (result.hasErrors()) {
            loadCheckoutModel(model, selections);
            return "orders/orderCheckout";
        }

        Order order = placeOrderInternal(checkoutDto, selections, authentication);
        return "redirect:/order/" + order.getOrderId();
    }

    // Xendit is temporarily out of the flow (business can't verify it yet) - a checkout submit
    // now IS the customer's payment claim, backed by the bank they picked and the reference
    // number they entered, so the order can be created directly with no external payment call.
    // An admin cross-checks the reference number manually and marks the order PAID (see markPaid).
    private Order placeOrderInternal(CheckoutDto checkoutDto, Map<String, Integer> selections,
                                      Authentication authentication) {
        Client client = clientService.findByContactNumber(checkoutDto.getContactNumber())
                .orElseGet(() -> {
                    ClientDto clientDto = new ClientDto();
                    clientDto.setFullName(checkoutDto.getFullName());
                    clientDto.setContactNumber(checkoutDto.getContactNumber());
                    clientDto.setEmail(checkoutDto.getEmail());
                    clientDto.setAddress(checkoutDto.getAddress());
                    return clientService.saveClient(clientDto);
                });

        if (isSignedIn(authentication)) {
            userService.findByUsername(authentication.getName())
                    .ifPresent(user -> clientService.linkToUser(client, user));
        }

        Order.PaymentMethod paymentMethod = Order.PaymentMethod.valueOf(checkoutDto.getPaymentMethod());
        Order.FulfilmentMethod fulfilmentMethod = Order.FulfilmentMethod.valueOf(checkoutDto.getFulfilmentMethod());
        Order order = orderService.createOrder(client, selections, paymentMethod, fulfilmentMethod,
                checkoutDto.getReferenceNumber(), checkoutDto.getReceiptFile());
        notificationService.notifyAdmin(
                client.getFullName() + " placed order " + order.getOrderNumber() + " (Php " + order.getTotalAmount() + ")",
                Notification.EntityType.ORDER, order.getOrderId());
        return order;
    }

    @GetMapping("/{id}")
    public String showOrder(@PathVariable("id") Long id, Authentication authentication, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        model.addAttribute("currentUsername", isSignedIn(authentication) ? authentication.getName() : null);
        return "orders/orderConfirmation";
    }

    // The "View" button on /order (both admin and customer) fetches this fragment via JS and
    // drops it straight into a modal, reusing the exact same markup as the full /order/{id} page
    // instead of duplicating it. Only an admin or the order's own linked account may fetch it.
    @GetMapping("/{id}/modal")
    public String showOrderModal(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Order order = orderService.getOrderById(id);
        boolean owns = isSignedIn(authentication) && order.getClient().getUser() != null
                && order.getClient().getUser().getUsername().equals(authentication.getName());
        if (!isAdmin(authentication) && !owns) {
            return "redirect:/order";
        }
        model.addAttribute("order", order);
        model.addAttribute("currentUsername", isSignedIn(authentication) ? authentication.getName() : null);
        return "orders/orderConfirmation :: orderCard";
    }

    @PutMapping("/markPaid/{id}")
    public String markPaid(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            String verifiedByEmployeeId = userService.findByUsername(authentication.getName())
                    .map(User::getEmployeeLabel)
                    .orElse(null);
            orderService.markAsPaid(id, verifiedByEmployeeId);
            Order order = orderService.getOrderById(id);
            notificationService.notifyCustomer(order.getClient().getUser(),
                    "Your order " + order.getOrderNumber() + " has been marked as paid.",
                    Notification.EntityType.ORDER, order.getOrderId());
        }
        return "redirect:/order";
    }

    // Admin-only build-stage control on the order list (adminOrderIndex.html's stage <select>).
    // ASSEMBLY_IN_PROGRESS/TESTING are rejected server-side for a parts-only order even though the
    // UI already hides them, same defensive re-check style as approveCancellation/denyCancellation
    // re-verifying order status rather than trusting the button that was visible.
    @PutMapping("/updateStage/{id}")
    public String updateBuildStage(@PathVariable("id") Long id,
                                    @RequestParam("buildStage") String buildStage,
                                    Authentication authentication) {
        if (isAdmin(authentication)) {
            Order.BuildStage stage = Order.BuildStage.valueOf(buildStage);
            Order order = orderService.getOrderById(id);
            boolean fullBuildOnlyStage = stage == Order.BuildStage.ASSEMBLY_IN_PROGRESS || stage == Order.BuildStage.TESTING;
            if (!fullBuildOnlyStage || OrderService.isFullBuild(order)) {
                orderService.updateBuildStage(id, stage);
                notificationService.notifyCustomer(order.getClient().getUser(),
                        "Your order " + order.getOrderNumber() + " is now: "
                                + OrderService.buildStageLabel(stage, order.getFulfilmentMethod()) + ".",
                        Notification.EntityType.ORDER, order.getOrderId());
            }
        }
        return "redirect:/order";
    }

// Cancelling an order still AWAITING_PAYMENT is open to anyone (guest checkout never creates a
    // User account to check ownership against - see placeOrderInternal) and takes effect right away,
    // since no money was ever taken. Once an order is PAID, an admin cancelling it directly is itself
    // the approval and still takes effect right away; but the order's own customer cancelling it
    // themselves only files a request (see requestCancellation) that an admin must approve or deny
    // before any refund is owed - real money was already taken, so a customer can't unilaterally
    // finalize that. Either way, cancellation acts as the warranty-claim window, so it's only allowed
    // within 7 days of the order being placed (see Order.isWithinCancellationWindow) - both for admin
    // and for the customer.
    @PutMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Long id, Authentication authentication) {
        Order order = orderService.getOrderById(id);
        if (!order.isWithinCancellationWindow()) {
            return "redirect:/order";
        }
        boolean admin = isAdmin(authentication);
        boolean owns = isSignedIn(authentication) && order.getClient().getUser() != null
                && order.getClient().getUser().getUsername().equals(authentication.getName());

        if (order.getStatus() == Order.OrderStatus.PAID && !admin && owns) {
            orderService.requestCancellation(id);
            notificationService.notifyAdmin(
                    order.getClient().getFullName() + " requested cancellation of order " + order.getOrderNumber()
                            + " (Php " + order.getTotalAmount() + ") - a refund approval is needed.",
                    Notification.EntityType.ORDER, order.getOrderId());
            notificationService.notifyCustomer(order.getClient().getUser(),
                    "Your cancellation request for order " + order.getOrderNumber()
                            + " has been submitted and is awaiting admin approval.",
                    Notification.EntityType.ORDER, order.getOrderId());
            return "redirect:/order";
        }

        if (order.getStatus() == Order.OrderStatus.PAID && !admin) {
            return "redirect:/order";
        }

        boolean refundOwed = orderService.cancelOrder(id);
        String message = refundOwed
                ? "Your order " + order.getOrderNumber() + " was cancelled. Since it was already paid, "
                        + "you're eligible for a refund - we'll process it soon."
                : "Your order " + order.getOrderNumber() + " has been cancelled.";
        notificationService.notifyCustomer(order.getClient().getUser(), message,
                Notification.EntityType.ORDER, order.getOrderId());
        return "redirect:/order";
    }

    @PutMapping("/approveCancellation/{id}")
    public String approveCancellation(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            Order order = orderService.getOrderById(id);
            if (order.getStatus() == Order.OrderStatus.CANCELLATION_REQUESTED) {
                orderService.approveCancellation(id);
                notificationService.notifyCustomer(order.getClient().getUser(),
                        "Your cancellation request for order " + order.getOrderNumber() + " was approved. "
                                + "You're eligible for a refund - we'll process it soon.",
                        Notification.EntityType.ORDER, order.getOrderId());
            }
        }
        return "redirect:/order";
    }

    @PutMapping("/denyCancellation/{id}")
    public String denyCancellation(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            Order order = orderService.getOrderById(id);
            if (order.getStatus() == Order.OrderStatus.CANCELLATION_REQUESTED) {
                orderService.denyCancellation(id);
                notificationService.notifyCustomer(order.getClient().getUser(),
                        "Your cancellation request for order " + order.getOrderNumber()
                                + " was denied. The order remains active.",
                        Notification.EntityType.ORDER, order.getOrderId());
            }
        }
        return "redirect:/order";
    }

    // Admin nudge for a customer who hasn't uploaded a receipt yet (or whose reference number
    // couldn't be cross-checked) - just fires a notification, doesn't change order state. Called
    // via fetch from adminOrderIndex.html so the page can pop a confirmation modal without a reload.
    @PutMapping("/requestReceipt/{id}")
    @ResponseBody
    public ResponseEntity<Void> requestReceipt(@PathVariable("id") Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        Order order = orderService.getOrderById(id);
        notificationService.notifyCustomer(order.getClient().getUser(),
                "Please upload your receipt for order " + order.getOrderNumber() + " so we can verify your payment.",
                Notification.EntityType.ORDER, order.getOrderId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/markRefunded/{id}")
    public String markRefunded(@PathVariable("id") Long id, Authentication authentication) {
        if (isAdmin(authentication)) {
            String refundedByEmployeeId = userService.findByUsername(authentication.getName())
                    .map(User::getEmployeeLabel)
                    .orElse(null);
            orderService.markRefunded(id, refundedByEmployeeId);
            Order order = orderService.getOrderById(id);
            notificationService.notifyCustomer(order.getClient().getUser(),
                    "Your refund for order " + order.getOrderNumber() + " has been processed.",
                    Notification.EntityType.ORDER, order.getOrderId());
        }
        return "redirect:/order";
    }

    // The customer's own follow-up upload if they skipped the receipt at checkout (or an admin
    // asks for one to verify the reference number) - only the order's own linked account can use
    // it, checked server-side regardless of whether the button is visible.
    @PutMapping("/uploadReceipt/{id}")
    public String uploadReceipt(@PathVariable("id") Long id,
                                 @RequestParam("receiptFile") MultipartFile receiptFile,
                                 Authentication authentication) {
        Order order = orderService.getOrderById(id);
        boolean owns = isSignedIn(authentication) && order.getClient().getUser() != null
                && order.getClient().getUser().getUsername().equals(authentication.getName());
        if (owns) {
            orderService.updateReceipt(id, receiptFile);
            notificationService.notifyAdmin(
                    order.getClient().getFullName() + " uploaded a receipt for order " + order.getOrderNumber() + ".",
                    Notification.EntityType.ORDER, order.getOrderId());
        }
        return "redirect:/order";
    }

    private void loadCheckoutModel(Model model, Map<String, Integer> selections) {
        LinkedHashMap<String, BuildPartView> resolved = orderService.resolveSelections(selections);
        double total = resolved.values().stream().mapToDouble(BuildPartView::getPrice).sum();

        model.addAttribute("parts", resolved.entrySet());
        model.addAttribute("total", total);
        model.addAttribute("cpuId", selections.get("CPU"));
        model.addAttribute("gpuId", selections.get("GPU"));
        model.addAttribute("motherboardId", selections.get("MOTHERBOARD"));
        model.addAttribute("ramId", selections.get("RAM"));
        model.addAttribute("storageSsdId", selections.get("STORAGE_SSD"));
        model.addAttribute("storageHddId", selections.get("STORAGE_HDD"));
        model.addAttribute("psuId", selections.get("PSU"));
        model.addAttribute("caseId", selections.get("CASE"));
        model.addAttribute("coolerId", selections.get("COOLER"));
    }

    private Map<String, Integer> buildSelections(Integer cpuId, Integer gpuId, Integer motherboardId,
                                                  Integer ramId, Integer storageSsdId, Integer storageHddId,
                                                  Integer psuId, Integer caseId, Integer coolerId) {
        LinkedHashMap<String, Integer> selections = new LinkedHashMap<>();
        if (cpuId != null) selections.put("CPU", cpuId);
        if (gpuId != null) selections.put("GPU", gpuId);
        if (motherboardId != null) selections.put("MOTHERBOARD", motherboardId);
        if (ramId != null) selections.put("RAM", ramId);
        if (storageSsdId != null) selections.put("STORAGE_SSD", storageSsdId);
        if (storageHddId != null) selections.put("STORAGE_HDD", storageHddId);
        if (psuId != null) selections.put("PSU", psuId);
        if (caseId != null) selections.put("CASE", caseId);
        if (coolerId != null) selections.put("COOLER", coolerId);
        return selections;
    }

    // One radio option in the admin stage-update modal (adminOrderIndex.html) - stageName is the
    // raw BuildStage enum name (the radio's value, and what /order/updateStage/{id} expects back),
    // label is already resolved via OrderService.buildStageLabel.
    private record StageOption(String stageName, String label) {
    }
}
