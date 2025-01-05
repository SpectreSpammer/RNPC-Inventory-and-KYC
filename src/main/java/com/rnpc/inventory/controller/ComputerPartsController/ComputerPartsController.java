package com.rnpc.inventory.controller.ComputerPartsController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import com.rnpc.inventory.models.computer.ComputerDto;
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
import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.models.computer.Computers;
import com.rnpc.inventory.services.ProductsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("computer")
public class ComputerPartsController {
	
	@Autowired
	private ProductsRepository repo;

	
	@GetMapping({"","/"})
	public String showProductList(Model model) {
		List<Computers> computersList = repo.findAll(Sort.by(Sort.Direction.DESC,"productId"));
		model.addAttribute("computer", computersList);
		return "products/computerParts";
	}
	
	
	@GetMapping("/create")
	public String showCreatePage(Model model) {
		ComputerDto computerDto = new ComputerDto();
		model.addAttribute("computerDto", computerDto);
		return "products/createComputerParts";
	}
	
	
	@PostMapping("/create")
	public String createProduct(
	        @Valid @ModelAttribute ComputerDto computerDto,
	        BindingResult result) {

	    if (computerDto.getImageFile().isEmpty()) {
	        result.addError(new FieldError("computerDto", "imageFile", "The image file is required!"));
	    }

	    if (result.hasErrors()) {
	        return "computers/createComputerParts";
	    }

	    //saving a file
	    MultipartFile image = computerDto.getImageFile();
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
	    
	    Computers computers = new Computers();
	    computers.setBrand(computerDto.getBrand());
	    computers.setModelName(computerDto.getModelName());
	    computers.setCategory(computerDto.getCategory());
	    computers.setStorageSize(computerDto.getStorageSize());
	    computers.setStocks(computerDto.getStocks());
	    computers.setPrice(computerDto.getPrice());
	    computers.setDescription(computerDto.getDescription());
	    computers.setCreatedAt(createdAt);
	    computers.setImageFileName(storageFileName);
	    
	    
	    repo.save(computers);
	     

	    return "redirect:/computer";
	}
	
	
	@GetMapping("/edit/{id}")
	public String showEditProductForm(@PathVariable("id") int id, Model model) {
	    Computers product = repo.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
	    
	    ComputerDto computerDto = new ComputerDto();
	    computerDto.setBrand(product.getBrand());
	    computerDto.setModelName(product.getModelName());
	    computerDto.setCategory(product.getCategory());
	    computerDto.setStorageSize(product.getStorageSize());
	    computerDto.setStocks(product.getStocks());
	    computerDto.setPrice(product.getPrice());
	    computerDto.setDescription(product.getDescription());
	    
	    model.addAttribute("computerDto", computerDto);
	    model.addAttribute("productId", id);
	    return "products/editComputerParts";
	}
	
	@PostMapping("/update/{id}")
	public String updateProduct(@PathVariable("id") int id, 
	                            @Valid @ModelAttribute ComputerDto computerDto,
	                            BindingResult result, 
	                            Model model) {
	    if (result.hasErrors()) {
	        model.addAttribute("productId", id);
	        return "products/editComputerParts";
	    }

	    Computers product = repo.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));

	    product.setBrand(computerDto.getBrand());
	    product.setModelName(computerDto.getModelName());
	    product.setCategory(computerDto.getCategory());
	    product.setStorageSize(computerDto.getStorageSize());
	    product.setStocks(computerDto.getStocks());
	    product.setPrice(computerDto.getPrice());
	    product.setDescription(computerDto.getDescription());

	    // Handle file upload
	    if (computerDto.getImageFile() != null && !computerDto.getImageFile().isEmpty()) {
	        MultipartFile image = computerDto.getImageFile();
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
	    return "redirect:/computer";
	}

	
	@GetMapping("/delete/{id}")
	public String deleteProduct(@PathVariable("id") int id, Model model) {
	    Computers product = repo.findById(id)
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
	    return "redirect:/computer";
	}
	
}
