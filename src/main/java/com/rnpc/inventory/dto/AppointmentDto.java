package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentDto {

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
    @Pattern(regexp = "Desktop|Laptop|Cellphone", message = "Invalid device type selected")
    private String deviceType;

    @Size(max = 1000, message = "The item description cannot exceed 1000 characters")
    private String itemDescription;

    @Size(max = 1000, message = "The history cannot exceed 1000 characters")
    private String history;

    @NotNull(message = "The preferred date is required!")
    @FutureOrPresent(message = "The preferred date cannot be in the past")
    private LocalDate preferredDate;

    @NotNull(message = "The preferred time is required!")
    private LocalTime preferredTime;

    @NotEmpty(message = "The issue description is required!")
    @Size(max = 1000, message = "The issue description cannot exceed 1000 characters")
    private String issueDescription;

    // A small reservation payment, collected the same way as order checkout, to discourage
    // no-shows/last-minute cancellations on a booked walk-in slot.
    @NotEmpty(message = "Please select a payment method!")
    @Pattern(regexp = "GCASH|MARIBANK|BPI|RCBC", message = "Invalid payment method selected")
    private String paymentMethod;

    @NotEmpty(message = "The reference number is required!")
    @Size(max = 100, message = "The reference number cannot exceed 100 characters")
    private String referenceNumber;

    private MultipartFile receiptFile;

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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public MultipartFile getReceiptFile() {
        return receiptFile;
    }

    public void setReceiptFile(MultipartFile receiptFile) {
        this.receiptFile = receiptFile;
    }
}
