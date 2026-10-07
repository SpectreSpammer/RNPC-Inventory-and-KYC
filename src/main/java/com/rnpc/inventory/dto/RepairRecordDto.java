package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/*
 * Brand is optional (a walk-in ticket can leave it blank, and an appointment-sourced repair has
 * none until an admin fills it in), and so is fix until the repair is finished: fix is required only
 * when the status is COMPLETED or RELEASED. That makes it conditional on another field, so it sits
 * in the FixRequired group and RepairRecordController validates Default plus that group only for
 * those two statuses (SmartValidator, the same way the laptop and cellphone controllers pick a
 * per-type group). A fresh ticket has neither, and must still open and save as PENDING or IN_PROGRESS.
 *
 * Cost follows the same split: once the repair is finished it must be greater than zero (missing,
 * zero and negative all give the one message below), while in every other status 0 is allowed and
 * only a negative value is refused. The two cost rules are in different groups - FixRequired and
 * NotFinished - so a negative cost on a finished repair shows the required-cost message once rather
 * than that plus "cannot be negative". Repairs batch 1b makes cost nullable; the rule must carry
 * over unchanged (null, zero and negative all rejected when finished). @Positive treats null as
 * valid, so the finished-repair rule also carries @NotNull with the same message - a no-op while
 * cost is a primitive, and what rejects a null the moment it becomes a Double.
 */
public class RepairRecordDto {

    /** Validation group: the work is done, so the fix has to say what was done and cost > 0. */
    public interface FixRequired {
    }

    /** Validation group: any other status, where cost may be 0 but never negative. */
    public interface NotFinished {
    }

    /** True for the statuses that make the fix mandatory. Null and unknown values are not. */
    public static boolean requiresFix(String status) {
        return "COMPLETED".equals(status) || "RELEASED".equals(status);
    }

    @NotNull(message = "The client is required!")
    private Long clientId;

    @NotEmpty(message = "The device type is required!")
    @Pattern(regexp = "Cellphone|Laptop|Desktop", message = "Invalid device type selected")
    private String deviceType;

    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String serialNumber;

    @NotEmpty(message = "The issue description is required!")
    @Size(max = 2000, message = "The issue description cannot exceed 2000 characters")
    private String issueDescription;

    private String technician;

    @NotNull(groups = FixRequired.class,
            message = "A cost greater than zero is required once the repair is Completed or Released.")
    @Positive(groups = FixRequired.class,
            message = "A cost greater than zero is required once the repair is Completed or Released.")
    @Min(value = 0, groups = NotFinished.class, message = "The cost cannot be negative")
    private double cost;

    @NotNull(message = "The date of repair is required!")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date repairDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date warrantyEndDate;

    @NotEmpty(message = "The status is required!")
    @Pattern(regexp = "PENDING|IN_PROGRESS|COMPLETED|RELEASED|CANCELLED", message = "Invalid status selected")
    private String status;

    @Size(max = 2000, message = "The remarks cannot exceed 2000 characters")
    private String remarks;

    @NotEmpty(groups = FixRequired.class, message = "The fix is required once the repair is Completed or Released.")
    @Size(max = 2000, message = "The fix cannot exceed 2000 characters")
    private String fix;

    @Size(max = 2000, message = "The recommendation cannot exceed 2000 characters")
    private String recommendation;

    private MultipartFile imageFile;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
