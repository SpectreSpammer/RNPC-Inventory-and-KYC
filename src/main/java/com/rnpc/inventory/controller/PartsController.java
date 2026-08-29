package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.PartsListView;
import com.rnpc.inventory.service.CasePartsService;
import com.rnpc.inventory.service.CoolerPartsService;
import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.GpuPartsService;
import com.rnpc.inventory.service.MotherboardPartsService;
import com.rnpc.inventory.service.PsuPartsService;
import com.rnpc.inventory.service.RamPartsService;
import com.rnpc.inventory.service.StoragePartsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class PartsController {

    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = Map.of(
            "CPU", "CPU",
            "GPU", "GPU",
            "MOTHERBOARD", "Motherboard",
            "RAM", "RAM",
            "STORAGE", "Storage",
            "PSU", "PSU",
            "CASE", "Case",
            "COOLER", "CPU Cooler"
    );

    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;

    @Autowired
    public PartsController(CpuPartsService cpuService, GpuPartsService gpuService,
                            MotherboardPartsService motherboardService, RamPartsService ramService,
                            StoragePartsService storageService, PsuPartsService psuService,
                            CasePartsService caseService, CoolerPartsService coolerService) {
        this.cpuService = cpuService;
        this.gpuService = gpuService;
        this.motherboardService = motherboardService;
        this.ramService = ramService;
        this.storageService = storageService;
        this.psuService = psuService;
        this.caseService = caseService;
        this.coolerService = coolerService;
    }

    @GetMapping("/computer")
    public String showPartsList(Model model) {
        List<BuildPartView> allParts = new ArrayList<>();
        allParts.addAll(cpuService.getAllAsBuildParts());
        allParts.addAll(gpuService.getAllAsBuildParts());
        allParts.addAll(motherboardService.getAllAsBuildParts());
        allParts.addAll(ramService.getAllAsBuildParts());
        allParts.addAll(storageService.getAllAsBuildParts());
        allParts.addAll(psuService.getAllAsBuildParts());
        allParts.addAll(caseService.getAllAsBuildParts());
        allParts.addAll(coolerService.getAllAsBuildParts());

        List<PartsListView> parts = new ArrayList<>();
        for (BuildPartView p : allParts) {
            parts.add(toListView(p));
        }

        model.addAttribute("parts", parts);
        model.addAttribute("categories", CATEGORY_DISPLAY_NAMES.values());
        return "products/allParts";
    }

    private PartsListView toListView(BuildPartView p) {
        String routePrefix = p.getCategory().toLowerCase();
        String displayName = CATEGORY_DISPLAY_NAMES.getOrDefault(p.getCategory(), p.getCategory());
        return new PartsListView(p.getComponentId(), displayName, routePrefix, p.getBrand(), p.getModelName(),
                p.getPrice(), p.getStocks(), p.getImageFileName(), p.getFields());
    }
}
