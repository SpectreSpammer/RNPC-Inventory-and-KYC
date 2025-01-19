package com.rnpc.inventory.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ComputerDto {
	
	@NotEmpty(message = "The brand is required!")
	  @Pattern(regexp = "WD|Kingston|Samsung|ADATA|Crucial", message = "Invalid brand selected")

	private String brand;
	
	@NotEmpty(message = "The model name is required!")
	private String modelName;
	
	@NotEmpty(message = "The category is required!")
	@Pattern(regexp = "HDD|SSD|M.2|Nvme", message = "Invalid category selected")
	private String category;
	
	@NotEmpty(message = "The storage size is required!")
	 @Pattern(regexp = "120 GB|240 GB|500 GB|1 TB|2 TB", message = "Invalid storage size selected")

	private String storageSize;
	
	@Min(value = 1, message = "The stocks must be greater than 0!")
	private int stocks;
	
	@Min(0)
	private double price;
	
	@Size(min = 10, message = "The description should be atleast 10 characters")
	@Size(max = 2000, message = "The description cannot be exceed 2000 characters")
	private String description;
	
	
	private MultipartFile imageFile;


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


	public MultipartFile getImageFile() {
		return imageFile;
	}


	public void setImageFile(MultipartFile imageFile) {
		this.imageFile = imageFile;
	}
	
	

}
