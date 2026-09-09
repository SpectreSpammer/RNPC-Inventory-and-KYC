package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TicketDto {

    @NotEmpty(message = "The full name is required!")
    private String fullName;

    @NotEmpty(message = "The contact number is required!")
    @Pattern(regexp = "^[0-9+\\-() ]{7,15}$", message = "Invalid contact number")
    private String contactNumber;

    @NotEmpty(message = "The email is required!")
    @Email(message = "Invalid email address")
    private String email;

    @NotEmpty(message = "The address is required!")
    @Size(max = 500, message = "The address cannot exceed 500 characters")
    private String address;

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

    @Min(value = 0, message = "The estimated cost cannot be negative")
    private double estimatedCost;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}
