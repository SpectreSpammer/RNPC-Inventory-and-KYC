package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.Order;
import com.rnpc.inventory.entity.OrderItem;
import com.rnpc.inventory.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;

    @Autowired
    public OrderService(OrderRepository repo, CpuPartsService cpuService, GpuPartsService gpuService,
                         MotherboardPartsService motherboardService, RamPartsService ramService,
                         StoragePartsService storageService, PsuPartsService psuService,
                         CasePartsService caseService, CoolerPartsService coolerService) {
        this.repo = repo;
        this.cpuService = cpuService;
        this.gpuService = gpuService;
        this.motherboardService = motherboardService;
        this.ramService = ramService;
        this.storageService = storageService;
        this.psuService = psuService;
        this.caseService = caseService;
        this.coolerService = coolerService;
    }

    public List<Order> getAllOrders() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "orderId"));
    }

    public List<Order> getOrdersByClient(Long clientId) {
        return repo.findByClient_ClientIdOrderByOrderIdDesc(clientId);
    }

    // A signed-in customer's own Order Summary: everything tied to any Client their account is
    // linked to. Mirrors AppointmentService.getAppointmentsForUser / RepairRecordService's
    // equivalent - see OrderController.showOrderList.
    public List<Order> getOrdersForUser(String username) {
        return repo.findByClient_User_UsernameOrderByOrderIdDesc(username);
    }

    public Order getOrderById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order Id: " + id));
    }

    /**
     * Selections are trusted only for which category/componentId the customer picked.
     * Brand, model, and price are always re-read from the live catalog here so a stale
     * or tampered client-side price can never be persisted to an order.
     */
    public LinkedHashMap<String, BuildPartView> resolveSelections(Map<String, Integer> selections) {
        List<BuildPartView> allParts = new ArrayList<>();
        allParts.addAll(cpuService.getAllAsBuildParts());
        allParts.addAll(gpuService.getAllAsBuildParts());
        allParts.addAll(motherboardService.getAllAsBuildParts());
        allParts.addAll(ramService.getAllAsBuildParts());
        allParts.addAll(storageService.getAllAsBuildParts());
        allParts.addAll(psuService.getAllAsBuildParts());
        allParts.addAll(caseService.getAllAsBuildParts());
        allParts.addAll(coolerService.getAllAsBuildParts());

        LinkedHashMap<String, BuildPartView> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : selections.entrySet()) {
            String category = entry.getKey();
            // STORAGE_SSD/STORAGE_HDD are two build-picker slots over one underlying "STORAGE"
            // catalog (split client-side by each part's Category field), so the live catalog
            // lookup always uses the real category even though the selection key is the slot.
            String lookupCategory = category.startsWith("STORAGE") ? "STORAGE" : category;
            Integer componentId = entry.getValue();
            BuildPartView match = allParts.stream()
                    .filter(p -> p.getCategory().equals(lookupCategory) && p.getComponentId() == componentId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid selection: " + category + "/" + componentId));
            resolved.put(category, match);
        }
        return resolved;
    }

    public Order createOrder(Client client, Map<String, Integer> selections, Order.PaymentMethod paymentMethod,
                              Order.FulfilmentMethod fulfilmentMethod, String referenceNumber, MultipartFile receiptFile) {
        LinkedHashMap<String, BuildPartView> parts = resolveSelections(selections);

        Order order = new Order();
        order.setClient(client);
        order.setPaymentMethod(paymentMethod);
        order.setFulfilmentMethod(fulfilmentMethod);
        order.setReferenceNumber(referenceNumber);
        order.setReceiptFileName(handleFileUpload(receiptFile));
        order.setStatus(Order.OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(new Date());
        order.setOrderNumber("PENDING");

        List<OrderItem> items = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, BuildPartView> entry : parts.entrySet()) {
            BuildPartView part = entry.getValue();
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setCategory(entry.getKey());
            item.setBrand(part.getBrand());
            item.setModelName(part.getModelName());
            item.setPrice(part.getPrice());
            items.add(item);
            total += part.getPrice();
        }
        order.setItems(items);
        order.setTotalAmount(total);

        order = repo.save(order);
        order.setOrderNumber(String.format("ORD-%05d", order.getOrderId()));
        return repo.save(order);
    }

    public void attachPaymentSession(Long id, String paymentSessionId, String paymentLinkUrl) {
        Order order = getOrderById(id);
        order.setPaymentSessionId(paymentSessionId);
        order.setPaymentLinkUrl(paymentLinkUrl);
        repo.save(order);
    }

    public void markAsPaid(Long id, String verifiedByEmployeeId) {
        Order order = getOrderById(id);
        order.setStatus(Order.OrderStatus.PAID);
        order.setVerifiedByEmployeeId(verifiedByEmployeeId);
        order.setVerifiedAt(new Date());
        // Starts the build timeline here, in the same save - never resets a stage that's already
        // advanced (e.g. re-marking paid after some other flow already moved it along).
        if (order.getBuildStage() == null) {
            order.setBuildStage(Order.BuildStage.ORDER_CONFIRMED);
        }
        repo.save(order);
    }

    // Admin order-list stage control (OrderController.updateBuildStage) - just advances the stage,
    // no other field touched.
    public void updateBuildStage(Long id, Order.BuildStage buildStage) {
        Order order = getOrderById(id);
        order.setBuildStage(buildStage);
        repo.save(order);
    }

    // A refund only becomes relevant if the order had actually been verified PAID before this
    // cancellation - cancelling one still AWAITING_PAYMENT never took real money, so nothing is
    // owed back. There's no payment gateway to auto-refund through; this only tracks that a
    // manual refund is owed (see markRefunded). This immediate path is only for an admin cancelling
    // directly (the admin's own action already is the approval) or for an AWAITING_PAYMENT order
    // (no money was ever taken, so there's nothing to approve) - a customer cancelling a PAID order
    // themselves goes through requestCancellation/approveCancellation/denyCancellation instead.
    public boolean cancelOrder(Long id) {
        Order order = getOrderById(id);
        boolean refundOwed = order.getStatus() == Order.OrderStatus.PAID;
        order.setStatus(Order.OrderStatus.CANCELLED);
        if (refundOwed) {
            order.setRefundStatus(Order.RefundStatus.PENDING);
        }
        repo.save(order);
        return refundOwed;
    }

    // A customer cancelling their own PAID order doesn't cancel it outright - it only flags the
    // order for an admin to approve or deny, since real money was already taken.
    public void requestCancellation(Long id) {
        Order order = getOrderById(id);
        order.setStatus(Order.OrderStatus.CANCELLATION_REQUESTED);
        repo.save(order);
    }

    public void approveCancellation(Long id) {
        Order order = getOrderById(id);
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setRefundStatus(Order.RefundStatus.PENDING);
        repo.save(order);
    }

    public void denyCancellation(Long id) {
        Order order = getOrderById(id);
        order.setStatus(Order.OrderStatus.PAID);
        repo.save(order);
    }

    public void markRefunded(Long id, String refundedByEmployeeId) {
        Order order = getOrderById(id);
        order.setRefundStatus(Order.RefundStatus.REFUNDED);
        order.setRefundedByEmployeeId(refundedByEmployeeId);
        order.setRefundedAt(new Date());
        repo.save(order);
    }

    // Lets a customer attach (or replace) a receipt after checkout, in case they skipped it or an
    // admin asks for one to verify the reference number. A blank/no-op upload is silently ignored
    // rather than clearing the existing receipt.
    public void updateReceipt(Long id, MultipartFile receiptFile) {
        if (receiptFile == null || receiptFile.isEmpty()) {
            return;
        }
        Order order = getOrderById(id);
        deleteImageFile(order.getReceiptFileName());
        order.setReceiptFileName(handleFileUpload(receiptFile));
        repo.save(order);
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

    // The 7 required build slots (GPU optional) - one shared definition for both this class's
    // full-build check below and DashboardController.buildCompletionPercent's completion bar, so
    // neither has to keep its own copy of the category list. STORAGE is satisfied by either its
    // SSD or HDD slot, same as buildCompletionPercent already treated it. Category keys match
    // OrderItem.getCategory() (set from this class's own createOrder, see the selections map
    // above) and SavedBuildItemView's map keys (see SavedBuildService) - both come from the same
    // builder selection keys, so one set of keys serves both callers unmodified.
    private static final List<List<String>> REQUIRED_BUILD_SLOTS = List.of(
            List.of("CPU"), List.of("MOTHERBOARD"), List.of("RAM"),
            List.of("STORAGE_SSD", "STORAGE_HDD"), List.of("PSU"), List.of("CASE"), List.of("COOLER")
    );

    public static boolean hasAllBuildCategories(Set<String> categoriesPresent) {
        return REQUIRED_BUILD_SLOTS.stream().allMatch(slot -> slot.stream().anyMatch(categoriesPresent::contains));
    }

    // Used by DashboardController.buildCompletionPercent in place of its own manual per-category
    // increments - same result, one definition.
    public static int countFilledBuildSlots(Set<String> categoriesPresent) {
        return (int) REQUIRED_BUILD_SLOTS.stream().filter(slot -> slot.stream().anyMatch(categoriesPresent::contains)).count();
    }

    // Full PC build (all 7 required categories present) vs a parts-only order. Used by
    // OrderController.updateBuildStage to decide whether ASSEMBLY_IN_PROGRESS/TESTING are valid
    // for a given order, and by orderIndex.html to decide whether to offer them at all.
    public static boolean isFullBuild(Order order) {
        Set<String> categories = order.getItems().stream().map(OrderItem::getCategory).collect(Collectors.toSet());
        return hasAllBuildCategories(categories);
    }

    // Shared plain-words label for a build stage, used both by orderIndex.html's stage dropdown
    // (via Thymeleaf's T(...) static call) and by OrderController.updateBuildStage's customer
    // notification, so the two can never say something different for the same stage. READY reads
    // per fulfilmentMethod - see Order.buildStage's own comment.
    public static String buildStageLabel(Order.BuildStage stage, Order.FulfilmentMethod fulfilmentMethod) {
        switch (stage) {
            case ORDER_CONFIRMED:
                return "Order Confirmed";
            case COMPONENTS_RESERVED:
                return "Components Reserved";
            case ASSEMBLY_IN_PROGRESS:
                return "Assembly In Progress";
            case TESTING:
                return "Testing";
            case READY:
                return fulfilmentMethod == Order.FulfilmentMethod.DELIVERY ? "Out for Delivery" : "Ready for Pickup";
            case COMPLETED:
                return "Completed";
            default:
                return stage.name();
        }
    }
}
