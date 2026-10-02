package com.rnpc.inventory.dto;

import com.rnpc.inventory.util.PhoneNumbers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 * Full name, contact number, device type, model name and issue description are required; email,
 * address and brand are optional (batch 5) - matching batch 4's treatment of ClientDto. @Email and
 * @Size are satisfied by a null or blank value on their own (neither implies presence the way
 * @NotEmpty does), so removing @NotEmpty is the whole change for email and address: a given email
 * is still checked for a valid format, and a given address is still capped at 500 characters. brand
 * carries no format constraint at all, so removing its @NotEmpty leaves nothing else to relax.
 */
public class TicketDto {

    @NotEmpty(message = "The full name is required!")
    private String fullName;

    @NotEmpty(message = "The contact number is required!")
    @Pattern(regexp = PhoneNumbers.PATTERN, message = "Invalid contact number")
    private String contactNumber;

    @Email(message = "Invalid email address")
    private String email;

    @Size(max = 500, message = "The address cannot exceed 500 characters")
    private String address;

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
