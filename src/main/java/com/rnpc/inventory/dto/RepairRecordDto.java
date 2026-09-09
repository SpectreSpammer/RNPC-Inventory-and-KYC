package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

public class RepairRecordDto {

    @NotNull(message = "The client is required!")
    private Long clientId;

    @NotEmpty(message = "The device type is required!")
    @Pattern(regexp = "Cellphone|Laptop|Desktop", message = "Invalid device type selected")
    private String deviceType;

    @NotEmpty(message = "The brand is required!")
    private String brand;

    @NotEmpty(message = "The model name is required!")
    private String modelName;

    private String serialNumber;

    @NotEmpty(message = "The issue description is required!")
    @Size(max = 2000, message = "The issue description cannot exceed 2000 characters")
    private String issueDescription;

    private String technician;

    @Min(value = 0, message = "The cost cannot be negative")
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

    @NotEmpty(message = "The fix is required!")
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
