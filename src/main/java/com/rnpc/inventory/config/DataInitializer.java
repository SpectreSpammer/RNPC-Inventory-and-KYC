package com.rnpc.inventory.config;

import com.rnpc.inventory.service.CasePartsService;
import com.rnpc.inventory.service.CoolerPartsService;
import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.GpuPartsService;
import com.rnpc.inventory.service.MotherboardPartsService;
import com.rnpc.inventory.service.PsuPartsService;
import com.rnpc.inventory.service.RamPartsService;
import com.rnpc.inventory.service.StoragePartsService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize demo data on application startup
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private CpuPartsService cpuPartsService;

    @Autowired
    private GpuPartsService gpuPartsService;

    @Autowired
    private MotherboardPartsService motherboardPartsService;

    @Autowired
    private RamPartsService ramPartsService;

    @Autowired
    private StoragePartsService storagePartsService;

    @Autowired
    private PsuPartsService psuPartsService;

    @Autowired
    private CasePartsService casePartsService;

    @Autowired
    private CoolerPartsService coolerPartsService;

    @Override
    public void run(String... args) throws Exception {
        // Pre-registered as ADMIN so signing in with this Google account reuses that role
        // instead of defaulting a first-time sign-in to CUSTOMER.
        userService.ensureAdminEmail("rnpcandcellphonerepairshop@gmail.com");

        // Seed the Build-a-PC component catalogs (one table per category)
        cpuPartsService.seedReferenceCatalogIfEmpty();
        gpuPartsService.seedReferenceCatalogIfEmpty();
        motherboardPartsService.seedReferenceCatalogIfEmpty();
        ramPartsService.seedReferenceCatalogIfEmpty();
        storagePartsService.seedReferenceCatalogIfEmpty();
        psuPartsService.seedReferenceCatalogIfEmpty();
        casePartsService.seedReferenceCatalogIfEmpty();
        coolerPartsService.seedReferenceCatalogIfEmpty();
    }
}
