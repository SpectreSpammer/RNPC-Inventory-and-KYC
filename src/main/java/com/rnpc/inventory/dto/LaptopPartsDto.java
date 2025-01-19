package com.rnpc.inventory.dto;

import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


	public class LaptopPartsDto {
	    
	    private int laptopPartId;
	    
	    @NotEmpty(message = "The brand is required!")
	    @Pattern(regexp = "Asus|Acer|Dell|Hp|Lenovo|MSI|", message = "Invalid brand selected")
	    private String brand;
	    
	    @NotEmpty(message = "The part name is required!")
	    @Pattern(regexp = "LCD|Keyboard|Trackpad|Ram|SSD|M.2", message = "Invalid part name selected")
	    private String partName;
	    
	    @NotEmpty(message = "The category is required!")
	    @Pattern(regexp = "Convertible|Netbook|Notebook|Gaming Laptop|Macbook", message = "Invalid category selected")
	    private String category;
	    
	    @NotEmpty(message = "The storage size is required!")
	    @Pattern(regexp = "None Application|120 GB|240 GB|500 GB|1 TB|2 TB", message = "Invalid storage size selected")
	    private String storageSize;
	    
	    @Min(value = 1, message = "The stocks must be greater than 0!")
	    private int stocks;
	    
	    @Min(0)
	    private double price;
	    
	    @Size(min = 10, message = "The description should be at least 10 characters")
	    @Size(max = 2000, message = "The description cannot exceed 2000 characters")
	    private String description;
	    
	    private Date createdAt;
	    
	    private String imageFileName;
	    
	    private MultipartFile imageFile;
	    

		public int getLaptopPartId() {
			return laptopPartId;
		}

		public void setLaptopPartId(int laptopPartId) {
			this.laptopPartId = laptopPartId;
		}

		public String getBrand() {
			return brand;
		}

		public void setBrand(String brand) {
			this.brand = brand;
		}

		public String getPartName() {
			return partName;
		}

		public void setPartName(String partName) {
			this.partName = partName;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getStorageSize() {
			return storageSize;
		}

		public void setStorageSize(String storageSize) {
			this.storageSize = storageSize;
		}

		public int getStocks() {
			return stocks;
		}

		public void setStocks(int stocks) {
			this.stocks = stocks;
		}

		public double getPrice() {
			return price;
		}

		public void setPrice(double price) {
			this.price = price;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
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

		public MultipartFile getImageFile() {
			return imageFile;
		}

		public void setImageFile(MultipartFile imageFile) {
			this.imageFile = imageFile;
		}



	    
	}
	


