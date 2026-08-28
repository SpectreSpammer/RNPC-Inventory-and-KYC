package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.BuildPartView;
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

@Controller
public class BuildController {

    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;

    @Autowired
    public BuildController(CpuPartsService cpuService, GpuPartsService gpuService,
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

    @GetMapping("/build")
    public String showBuildPage(Model model) {
        List<BuildPartView> parts = new ArrayList<>();
        parts.addAll(cpuService.getAllAsBuildParts());
        parts.addAll(gpuService.getAllAsBuildParts());
        parts.addAll(motherboardService.getAllAsBuildParts());
        parts.addAll(ramService.getAllAsBuildParts());
        parts.addAll(storageService.getAllAsBuildParts());
        parts.addAll(psuService.getAllAsBuildParts());
        parts.addAll(caseService.getAllAsBuildParts());
        parts.addAll(coolerService.getAllAsBuildParts());

        model.addAttribute("components", parts);
        return "build/buildPc";
    }
}
