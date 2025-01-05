package com.rnpc.inventory.controller.LaptopController;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.rnpc.inventory.models.laptop.LaptopParts;
import com.rnpc.inventory.models.laptop.LaptopPartsDto;
import com.rnpc.inventory.services.LaptopRepository.LaptopPartsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("laptop")
public class LaptopPartsController {

	@Autowired
	private LaptopPartsRepository laptopRepo;



	@GetMapping({ "", "/" })
	public String showLaptopPartsList(Model model) {
	    List<LaptopParts> laptopPartsList = laptopRepo.findAll(Sort.by(Sort.Direction.DESC, "laptopPartId"));
	    model.addAttribute("laptop", laptopPartsList);
	    return "products/laptopParts";
	}
	

	@GetMapping("/create")
	public String showCreatePage(Model model) {
	    LaptopPartsDto laptopPartsDto = new LaptopPartsDto();
	    model.addAttribute("laptopPartsDto", laptopPartsDto);
	    return "products/createLaptopPart";
	}

	@PostMapping("/create")
	public String createLaptopParts(
	        @Valid @ModelAttribute LaptopPartsDto laptopPartsDto, 
	        BindingResult result,
	        Model model) {
	    if (laptopPartsDto.getImageFile().isEmpty()) {
	        result.addError(new FieldError("laptopPartsDto", "imageFile", "The image file is required!"));
	    }
	    if (result.hasErrors()) {
	        return "products/createLaptopPart";
	    }
	    
	    // Saving the file
	    MultipartFile image = laptopPartsDto.getImageFile();
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
	    
	    LaptopParts laptopParts = new LaptopParts();
	    laptopParts.setBrand(laptopPartsDto.getBrand());
	    laptopParts.setPartName(laptopPartsDto.getPartName());
	    laptopParts.setCategory(laptopPartsDto.getCategory());
	    laptopParts.setStorageSize(laptopPartsDto.getStorageSize());
	    laptopParts.setStocks(laptopPartsDto.getStocks());
	    laptopParts.setPrice(laptopPartsDto.getPrice());
	    laptopParts.setDescription(laptopPartsDto.getDescription());
	    laptopParts.setCreatedAt(createdAt);
	    laptopParts.setImageFileName(storageFileName);
	    
	    laptopRepo.save(laptopParts);
	     
	    return "redirect:/laptop";
	}
}
