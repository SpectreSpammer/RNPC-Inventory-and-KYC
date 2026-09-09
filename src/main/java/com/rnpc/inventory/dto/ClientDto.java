package com.rnpc.inventory.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClientDto {

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

    private MultipartFile imageFile;

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

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }
}
