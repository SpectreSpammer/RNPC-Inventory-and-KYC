package com.rnpc.inventory.models.laptop;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;



@Entity
@Table(name = "rnpc_laptop_parts")
public class LaptopParts {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int laptopPartId;
	
	
	private String brand;
	private String partName;
	private String category;
	private String storageSize;
	private int stocks;
	private double price;
	
	@Column(columnDefinition = "TEXT")
	private String description;
	private Date createdAt;
	private String imageFileName;
	
	
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
	
	
	
	
	
	
}
