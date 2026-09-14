package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "rnpc_appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String deviceType;

    // NULL on older records: those bookings used the original device-repair workflow.
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 32, columnDefinition = "VARCHAR(32)")
    private ServiceType serviceType;

    // Optional on existing appointments and on cleaning/consultation bookings.
    @Enumerated(EnumType.STRING)
    @Column(name = "device_category", length = 32, columnDefinition = "VARCHAR(32)")
    private DeviceCategory deviceCategory;

    public enum DeviceCategory {
        ANDROID("Android", "Cellphone"), IOS("iOS", "Cellphone"),
        GAMING_LAPTOP("Gaming Laptop", "Laptop"),
        REGULAR_LAPTOP("Regular Laptop", "Laptop"),
        APPLE_MACBOOK("MacBook", "Laptop"),
        AMD("AMD", "Desktop"), INTEL("Intel", "Desktop");

        private final String label;
        private final String deviceType;
        DeviceCategory(String label, String deviceType) {
            this.label = label;
            this.deviceType = deviceType;
        }
        public String getLabel() { return label; }
        public String getDeviceType() { return deviceType; }
    }

    public DeviceCategory getDeviceCategory() { return deviceCategory; }
    public void setDeviceCategory(DeviceCategory deviceCategory) { this.deviceCategory = deviceCategory; }
    public String getDeviceLabel() {
        return deviceCategory == null ? deviceType : deviceType + " - " + deviceCategory.getLabel();
    }

    public enum ServiceType {
        DESKTOP_REPAIR("Desktop Repair"),
        LAPTOP_REPAIR("Laptop Repair"),
        CELLPHONE_REPAIR("Cellphone Repair"),
        PC_LAPTOP_CLEANING("PC / Laptop Cleaning"),
        GENERAL_CONSULTATION("General Consultation");

        private final String label;
        ServiceType(String label) { this.label = label; }
        public String getLabel() { return label; }

        public boolean allowsDevice(String device) {
            if (device == null) return false;
            return switch (this) {
                case DESKTOP_REPAIR -> "Desktop".equals(device);
                case LAPTOP_REPAIR -> "Laptop".equals(device);
                case CELLPHONE_REPAIR -> "Cellphone".equals(device);
                case PC_LAPTOP_CLEANING -> "Desktop".equals(device) || "Laptop".equals(device);
                case GENERAL_CONSULTATION -> "Desktop".equals(device) || "Laptop".equals(device)
                        || "Cellphone".equals(device);
            };
        }
    }

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
    public String getServiceLabel() {
        return serviceType != null ? serviceType.getLabel() : deviceType + " Repair";
    }

    @Column(columnDefinition = "TEXT")
    private String itemDescription;

    @Column(columnDefinition = "TEXT")
    private String history;

    @Column(nullable = false)
    private LocalDate preferredDate;

    @Column(nullable = false)
    private LocalTime preferredTime;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    // A small reservation payment, collected the same way as order checkout, to discourage
    // no-shows/last-minute cancellations on a booked walk-in slot. Reuses Order.PaymentMethod
    // rather than a duplicate enum, since it's literally the same set of payment options.
    @Enumerated(EnumType.STRING)
    private Order.PaymentMethod paymentMethod;

    private String referenceNumber;

    private String receiptFileName;

    // Set when an admin confirms an appointment that has a reservation payment attached (see
    // AppointmentService.confirmAppointment) - the acting admin's employee label (see
    // User.getEmployeeLabel()), mirroring Order.verifiedByEmployeeId/verifiedAt for Mark Paid.
    private String verifiedByEmployeeId;

    private Date verifiedAt;

    // Set automatically when a cancellation happens (see AppointmentService.applyCancellation) -
    // PENDING if more than 5 hours remain before the scheduled slot, NOT_REFUNDABLE otherwise.
    // There's no payment gateway to auto-refund through, so this only tracks that a manual refund
    // is owed; an admin still has to actually send the money back and click Mark Refunded.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus = RefundStatus.NOT_APPLICABLE;

    private String refundedByEmployeeId;

    private Date refundedAt;

    private Date createdAt;

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    public enum RefundStatus {
        NOT_APPLICABLE, PENDING, NOT_REFUNDABLE, REFUNDED
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public LocalTime getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(LocalTime preferredTime) {
        this.preferredTime = preferredTime;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Order.PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Order.PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    // Not a persisted column - field access means Hibernate never sees this getter as a mapped
    // property, so it's safe to read straight from a Thymeleaf template as ${appointment.cancellableByCustomer}.
    @Transient
    public boolean isCancellableByCustomer() {
        if (status == Status.CANCELLED) {
            return false;
        }
        return LocalDateTime.of(preferredDate, preferredTime).isAfter(LocalDateTime.now().plusHours(12));
    }

    // Whether a reservation payment would still be refundable if cancelled right now - more than
    // 5 hours out from the scheduled slot. Used at the moment of cancellation (see
    // AppointmentService) to decide PENDING vs NOT_REFUNDABLE; not a persisted column.
    @Transient
    public boolean isRefundEligibleNow() {
        return LocalDateTime.of(preferredDate, preferredTime).isAfter(LocalDateTime.now().plusHours(5));
    }
}
