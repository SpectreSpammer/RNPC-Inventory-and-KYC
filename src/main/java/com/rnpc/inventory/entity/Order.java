package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "rnpc_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false)
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private double totalAmount;

    private Date createdAt;

    private String paymentSessionId;

    @Column(length = 1000)
    private String paymentLinkUrl;

    // Customer-entered proof of a manual bank/e-wallet transfer - collected at checkout since
    // Xendit is temporarily out of the flow (see OrderController) and payment isn't verified
    // automatically; an admin cross-checks this against their own records before marking PAID.
    private String referenceNumber;

    // Optional screenshot of the payment the customer uploaded at checkout, so an admin can
    // cross-check it against the reference number before marking the order PAID.
    private String receiptFileName;

    // Set when an admin clicks "Mark Paid" (see OrderController.markPaid) - the acting admin's
    // employee id (their Google account id, or their internal User.id if they've never signed in
    // with Google - see User.getEmployeeId()).
    private String verifiedByEmployeeId;

    private Date verifiedAt;

    // Set automatically when a PAID order is cancelled (see OrderService.cancelOrder) - there's
    // no payment gateway to auto-refund through, so this only tracks that a manual refund is
    // owed; an admin still has to actually send the money back and then click Mark Refunded.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus = RefundStatus.NOT_APPLICABLE;

    private String refundedByEmployeeId;

    private Date refundedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // NULL means no build timeline yet - set to ORDER_CONFIRMED when an admin marks the order paid
    // (see OrderService.markAsPaid), then only ever advanced from the admin order list, never reset
    // backwards. READY renders as "Ready for Pickup" or "Out for Delivery" depending on
    // fulfilmentMethod below - see OrderService.buildStageLabel.
    @Enumerated(EnumType.STRING)
    private BuildStage buildStage;

    // Set at checkout (see OrderService.createOrder) - null only for orders placed before this
    // column existed.
    @Enumerated(EnumType.STRING)
    private FulfilmentMethod fulfilmentMethod;

    public enum PaymentMethod {
        GCASH, MARIBANK, BPI, RCBC
    }

    public enum OrderStatus {
        AWAITING_PAYMENT, PAID, CANCELLATION_REQUESTED, CANCELLED
    }

    public enum RefundStatus {
        NOT_APPLICABLE, PENDING, REFUNDED
    }

    public enum BuildStage {
        ORDER_CONFIRMED, COMPONENTS_RESERVED, ASSEMBLY_IN_PROGRESS, TESTING, READY, COMPLETED
    }

    public enum FulfilmentMethod {
        PICKUP, DELIVERY
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getPaymentSessionId() {
        return paymentSessionId;
    }

    public void setPaymentSessionId(String paymentSessionId) {
        this.paymentSessionId = paymentSessionId;
    }

    public String getPaymentLinkUrl() {
        return paymentLinkUrl;
    }

    public void setPaymentLinkUrl(String paymentLinkUrl) {
        this.paymentLinkUrl = paymentLinkUrl;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReceiptFileName() {
        return receiptFileName;
    }

    public void setReceiptFileName(String receiptFileName) {
        this.receiptFileName = receiptFileName;
    }

    public String getVerifiedByEmployeeId() {
        return verifiedByEmployeeId;
    }

    public void setVerifiedByEmployeeId(String verifiedByEmployeeId) {
        this.verifiedByEmployeeId = verifiedByEmployeeId;
    }

    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(RefundStatus refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getRefundedByEmployeeId() {
        return refundedByEmployeeId;
    }

    public void setRefundedByEmployeeId(String refundedByEmployeeId) {
        this.refundedByEmployeeId = refundedByEmployeeId;
    }

    public Date getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Date refundedAt) {
        this.refundedAt = refundedAt;
    }

    public BuildStage getBuildStage() {
        return buildStage;
    }

    public void setBuildStage(BuildStage buildStage) {
        this.buildStage = buildStage;
    }

    public FulfilmentMethod getFulfilmentMethod() {
        return fulfilmentMethod;
    }

    public void setFulfilmentMethod(FulfilmentMethod fulfilmentMethod) {
        this.fulfilmentMethod = fulfilmentMethod;
    }

    // Cancelling (and any warranty-style claim that goes with it) is only allowed within 7 days of
    // the order being placed - not a persisted column, so this is safe to read straight from a
    // Thymeleaf template as ${order.withinCancellationWindow}.
    @Transient
    public boolean isWithinCancellationWindow() {
        if (createdAt == null) {
            return false;
        }
        long sevenDaysMillis = 7L * 24 * 60 * 60 * 1000;
        return System.currentTimeMillis() - createdAt.getTime() <= sevenDaysMillis;
    }
}
