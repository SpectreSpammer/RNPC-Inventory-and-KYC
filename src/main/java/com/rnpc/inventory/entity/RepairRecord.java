package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "rnpc_repair_records")
public class RepairRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repairId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Set only when this record was auto-created from a customer's scheduled appointment (see
    // RepairRecordService.createFromAppointment) - null for records an admin entered directly
    // (e.g. a walk-in ticket). Drives isVisible() below: nobody (admin included) sees it in
    // Repair History until an admin has actually confirmed the appointment.
    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false)
    private String jobOrderNumber;

    private String deviceType;
    private String brand;
    private String modelName;
    private String serialNumber;

    @Column(columnDefinition = "TEXT")
    private String issueDescription;

    private String technician;
    private double cost;

    private Date repairDate;
    private Date warrantyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String fix;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    private Date createdAt;
    private String imageFileName;

    public enum RepairStatus {
        PENDING, IN_PROGRESS, COMPLETED, RELEASED, CANCELLED
    }

    public Long getRepairId() {
        return repairId;
    }

    public void setRepairId(Long repairId) {
        this.repairId = repairId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getJobOrderNumber() {
        return jobOrderNumber;
    }

    public void setJobOrderNumber(String jobOrderNumber) {
        this.jobOrderNumber = jobOrderNumber;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public Date getRepairDate() {
        return repairDate;
    }

    public void setRepairDate(Date repairDate) {
        this.repairDate = repairDate;
    }

    public Date getWarrantyEndDate() {
        return warrantyEndDate;
    }

    public void setWarrantyEndDate(Date warrantyEndDate) {
        this.warrantyEndDate = warrantyEndDate;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getFix() {
        return fix;
    }

    public void setFix(String fix) {
        this.fix = fix;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    // An appointment-sourced record stays hidden from Repair History - for the admin too, not
    // just the customer - until an admin actually confirms the appointment it came from; a
    // walk-in record entered directly by an admin (appointment == null) is visible right away.
    // Not a persisted column (see the @Transient note on Appointment.isCancellableByCustomer).
    @Transient
    public boolean isVisible() {
        return appointment == null || appointment.getStatus() == Appointment.Status.CONFIRMED;
    }
}
