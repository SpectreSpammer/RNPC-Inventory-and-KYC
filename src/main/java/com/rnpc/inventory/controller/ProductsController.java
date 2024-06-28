package com.rnpc.inventory.controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.models.ProductDto;
import com.rnpc.inventory.models.Products;
import com.rnpc.inventory.services.ProductsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("products")
public class ProductsController {
	
	@Autowired
	private ProductsRepository repo;

	
	@GetMapping({"","/"})
	public String showProductList(Model model) {
		List<Products> productsList = repo.findAll(Sort.by(Sort.Direction.DESC,"productId"));
		model.addAttribute("products",productsList);
		return "products/index";
	}
	
	
	@GetMapping("/create")
	public String showCreatePage(Model model) {
		ProductDto productDto = new ProductDto();
		model.addAttribute("productDto",productDto);
		return "products/createProduct";
	}
	
	
	@PostMapping("/create")
	public String createProduct(
	        @Valid @ModelAttribute ProductDto productDto, 
	        BindingResult result) {

	    if (productDto.getImageFile().isEmpty()) {
	        result.addError(new FieldError("productDto", "imageFile", "The image file is required!"));
	    }

	    if (result.hasErrors()) {
	        return "products/createProduct";
	    }

	    //saving a file
	    MultipartFile image = productDto.getImageFile();
	    Date createdAt = new Date();
	    String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

	    try {
	        String uploadDir = "public/images/";
	        Path uploadPath = Paths.get(uploadDir);

	        if (!Files.exists(uploadPath)) {
	            Files.createDirectories(uploadPath);
	        }

	        try (InputStream inputStream = image.getInputStream()) {
	            Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
	        } catch (Exception ex) {
	            System.out.println("Exception: " + ex.getMessage());
	        }
	    } catch (Exception ex) {
	        System.out.println("Exception: " + ex.getMessage());
	    }
	    
	    Products products = new Products();
	    products.setBrand(productDto.getBrand());
	    products.setModelName(productDto.getModelName());
	    products.setCategory(productDto.getCategory());
	    products.setStorageSize(productDto.getStorageSize());
	    products.setStocks(productDto.getStocks());
	    products.setPrice(productDto.getPrice());
	    products.setDescription(productDto.getDescription());
	    products.setCreatedAt(createdAt);
	    products.setImageFileName(storageFileName);
	    
	    
	    repo.save(products);
	     

	    return "redirect:/products";
	}
	
	
	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Model model) {
	    Products product = repo.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
	    
	    ProductDto productDto = new ProductDto();
	    productDto.setBrand(product.getBrand());
	    productDto.setModelName(product.getModelName());
	    productDto.setCategory(product.getCategory());
	    productDto.setStorageSize(product.getStorageSize());
	    productDto.setStocks(product.getStocks());
	    productDto.setPrice(product.getPrice());
	    productDto.setDescription(product.getDescription());
	    
	    model.addAttribute("productDto", productDto);
	    model.addAttribute("productId", id);
	    return "products/editProduct";
	}
	
	@PostMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id, 
	                            @Valid @ModelAttribute ProductDto productDto, 
	                            BindingResult result, 
	                            Model model) {
	    if (result.hasErrors()) {
	        model.addAttribute("productId", id);
	        return "products/editProduct";
	    }

	    Products product = repo.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));

	    product.setBrand(productDto.getBrand());
	    product.setModelName(productDto.getModelName());
	    product.setCategory(productDto.getCategory());
	    product.setStorageSize(productDto.getStorageSize());
	    product.setStocks(productDto.getStocks());
	    product.setPrice(productDto.getPrice());
	    product.setDescription(productDto.getDescription());

	    // Handle file upload
	    if (productDto.getImageFile() != null && !productDto.getImageFile().isEmpty()) {
	        MultipartFile image = productDto.getImageFile();
	        String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();

	        try {
	            String uploadDir = "public/images/";
	            Path uploadPath = Paths.get(uploadDir);

	            if (!Files.exists(uploadPath)) {
	                Files.createDirectories(uploadPath);
	            }

	            try (InputStream inputStream = image.getInputStream()) {
	                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
	            }

	            product.setImageFileName(storageFileName);
	        } catch (Exception ex) {
	            System.out.println("Exception: " + ex.getMessage());
	        }
	    }

	    repo.save(product);
	    return "redirect:/products";
	}

	
	@GetMapping("/delete/{id}")
	public String deleteProduct(@PathVariable("id") int id, Model model) {
	    Products product = repo.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
	    
	    // Delete the associated image file
	    if (product.getImageFileName() != null && !product.getImageFileName().isEmpty()) {
	        try {
	            String uploadDir = "public/images/";
	            Path filePath = Paths.get(uploadDir + product.getImageFileName());
	            Files.deleteIfExists(filePath);
	        } catch (Exception e) {
	            System.out.println("Error deleting file: " + e.getMessage());
	        }
	    }
	    
	    repo.delete(product);
	    return "redirect:/products";
	}
	
}