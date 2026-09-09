package com.rnpc.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CheckoutDto {

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

    @NotEmpty(message = "Please select a payment method!")
    @Pattern(regexp = "GCASH|MARIBANK|BPI|RCBC", message = "Invalid payment method selected")
    private String paymentMethod;

    @NotEmpty(message = "The reference number is required!")
    @Size(max = 100, message = "The reference number cannot exceed 100 characters")
    private String referenceNumber;

    // Optional - not every customer will have a screenshot handy at checkout time.
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
