package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.BuildPartView;
import com.rnpc.inventory.dto.SavedBuildItemView;
import com.rnpc.inventory.dto.SavedBuildLoadResult;
import com.rnpc.inventory.entity.SavedBuild;
import com.rnpc.inventory.entity.SavedBuildItem;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.SavedBuildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SavedBuildService {

    private final SavedBuildRepository repo;
    private final CpuPartsService cpuService;
    private final GpuPartsService gpuService;
    private final MotherboardPartsService motherboardService;
    private final RamPartsService ramService;
    private final StoragePartsService storageService;
    private final PsuPartsService psuService;
    private final CasePartsService caseService;
    private final CoolerPartsService coolerService;

    @Autowired
    public SavedBuildService(SavedBuildRepository repo, CpuPartsService cpuService, GpuPartsService gpuService,
                              MotherboardPartsService motherboardService, RamPartsService ramService,
                              StoragePartsService storageService, PsuPartsService psuService,
                              CasePartsService caseService, CoolerPartsService coolerService) {
        this.repo = repo;
        this.cpuService = cpuService;
        this.gpuService = gpuService;
        this.motherboardService = motherboardService;
        this.ramService = ramService;
        this.storageService = storageService;
        this.psuService = psuService;
        this.caseService = caseService;
        this.coolerService = coolerService;
    }

    public List<SavedBuild> getSavedBuildsForUser(String username) {
        return repo.findByUser_UsernameOrderBySavedBuildIdDesc(username);
    }

    public SavedBuild getSavedBuildById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid saved build Id: " + id));
    }

    /**
     * Same "trust only category/componentId, always re-read brand/model/price live" rule as
     * OrderService.createOrder - a saved build's price snapshot can't be spoofed from the client
     * any more than an order's can.
     */
    public SavedBuild createSavedBuild(User user, String name, Map<String, Integer> selections) {
        if (selections.isEmpty()) {
            throw new IllegalArgumentException("A saved build needs at least one selected part.");
        }

        SavedBuild build = new SavedBuild();
        build.setUser(user);
        build.setName((name == null || name.isBlank()) ? "My Build" : name.trim());
        build.setCreatedAt(new Date());

        List<SavedBuildItem> items = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, Integer> entry : selections.entrySet()) {
            String category = entry.getKey();
            int componentId = entry.getValue();
            BuildPartView part = findLivePart(category, componentId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid selection: " + category + "/" + componentId));

            SavedBuildItem item = new SavedBuildItem();
            item.setSavedBuild(build);
            item.setCategory(category);
            item.setComponentId(componentId);
            item.setBrand(part.getBrand());
            item.setModelName(part.getModelName());
            item.setPrice(part.getPrice());
            items.add(item);
            total += part.getPrice();
        }
        build.setItems(items);
        build.setTotalPrice(total);

        return repo.save(build);
    }

    public void deleteSavedBuild(Long id, String username) {
        SavedBuild build = getSavedBuildById(id);
        if (!build.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not your saved build.");
        }
        repo.delete(build);
    }

    // Straight copy of the persisted snapshot rows under a new name/id - deliberately NOT a
    // re-save (no live re-resolution), so a duplicate of a build with an already-unavailable item
    // stays unavailable too, exactly mirroring the original rather than silently fixing it up.
    public SavedBuild duplicateSavedBuild(Long id, String username) {
        SavedBuild original = getSavedBuildById(id);
        if (!original.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not your saved build.");
        }

        SavedBuild copy = new SavedBuild();
        copy.setUser(original.getUser());
        copy.setName(original.getName() + " (Copy)");
        copy.setCreatedAt(new Date());
        copy.setTotalPrice(original.getTotalPrice());

        List<SavedBuildItem> items = new ArrayList<>();
        for (SavedBuildItem source : original.getItems()) {
            SavedBuildItem item = new SavedBuildItem();
            item.setSavedBuild(copy);
            item.setCategory(source.getCategory());
            item.setComponentId(source.getComponentId());
            item.setBrand(source.getBrand());
            item.setModelName(source.getModelName());
            item.setPrice(source.getPrice());
            items.add(item);
        }
        copy.setItems(items);

        return repo.save(copy);
    }

    /**
     * Rendering-ready view of a saved build's items: category/brand/modelName/price always come
     * straight from the persisted snapshot (never a live re-read, so a deleted part never breaks
     * this page - see chat), with a separately-computed `available` flag per item purely to drive
     * the "Unavailable" badge and gate that item's "Load into Builder" eligibility.
     */
    public List<SavedBuildItemView> viewItems(SavedBuild build) {
        List<SavedBuildItemView> views = new ArrayList<>();
        for (SavedBuildItem item : build.getItems()) {
            boolean available = findLivePart(item.getCategory(), item.getComponentId()).isPresent();
            views.add(new SavedBuildItemView(item.getCategory(), item.getBrand(), item.getModelName(),
                    item.getPrice(), item.getComponentId(), available));
        }
        return views;
    }

    // The card grid only ever shows these 5 rows (CPU/GPU/RAM/Storage/PSU) - Motherboard/Case/
    // Cooler are intentionally left off the visible spec list (see chat); Storage collapses
    // STORAGE_SSD and STORAGE_HDD into one row (SSD preferred if a build somehow has both) since
    // the card only has room for a single "Storage" line.
    public LinkedHashMap<String, SavedBuildItemView> specRows(SavedBuild build) {
        Map<String, SavedBuildItemView> byCategory = new LinkedHashMap<>();
        for (SavedBuildItemView view : viewItems(build)) {
            byCategory.put(view.getCategory(), view);
        }

        LinkedHashMap<String, SavedBuildItemView> rows = new LinkedHashMap<>();
        rows.put("CPU", byCategory.get("CPU"));
        rows.put("GPU", byCategory.get("GPU"));
        rows.put("RAM", byCategory.get("RAM"));
        rows.put("Storage", byCategory.containsKey("STORAGE_SSD") ? byCategory.get("STORAGE_SSD") : byCategory.get("STORAGE_HDD"));
        rows.put("PSU", byCategory.get("PSU"));
        return rows;
    }

    // CaseParts.imageFileName (CaseParts.java:26, getter CaseParts.java:93-95) is surfaced onto
    // BuildPartView.imageFileName by CasePartsService.getAllAsBuildParts (CasePartsService.java:236)
    // - a live catalog field, never snapshotted onto SavedBuildItem, so this is always a fresh
    // lookup. Returns null (caller falls back to the neutral placeholder) when no CASE item was
    // saved, or its componentId no longer resolves live (deleted since saving).
    public String getCaseImageFileName(SavedBuild build) {
        return build.getItems().stream()
                .filter(item -> "CASE".equals(item.getCategory()))
                .findFirst()
                .flatMap(item -> findLivePart(item.getCategory(), item.getComponentId()))
                .map(BuildPartView::getImageFileName)
                .orElse(null);
    }

    /**
     * "Load into Builder": only the items that still resolve in the live catalog are handed back
     * as selections (category -> componentId) for the picker to reselect; anything deleted since
     * this build was saved is named in unavailableLabels instead of silently dropped or failing
     * the whole load.
     */
    public SavedBuildLoadResult loadSelections(SavedBuild build) {
        LinkedHashMap<String, Integer> selections = new LinkedHashMap<>();
        List<String> unavailableLabels = new ArrayList<>();
        for (SavedBuildItem item : build.getItems()) {
            if (findLivePart(item.getCategory(), item.getComponentId()).isPresent()) {
                selections.put(item.getCategory(), item.getComponentId());
            } else {
                unavailableLabels.add(item.getCategory() + ": " + item.getBrand() + " " + item.getModelName());
            }
        }
        return new SavedBuildLoadResult(selections, unavailableLabels);
    }

    // Same aggregation OrderService.resolveSelections/BuildController.showBuildPage each build
    // independently - see chat for why this isn't pulled into one shared helper (matches existing
    // codebase convention rather than introducing a new shared service for a 3rd caller).
    private Optional<BuildPartView> findLivePart(String category, int componentId) {
        // STORAGE_SSD/STORAGE_HDD are two build-picker slots over one underlying "STORAGE" catalog
        // (see OrderService.resolveSelections) - same rule applies here.
        String lookupCategory = category.startsWith("STORAGE") ? "STORAGE" : category;
        List<BuildPartView> allParts = new ArrayList<>();
        allParts.addAll(cpuService.getAllAsBuildParts());
        allParts.addAll(gpuService.getAllAsBuildParts());
        allParts.addAll(motherboardService.getAllAsBuildParts());
        allParts.addAll(ramService.getAllAsBuildParts());
        allParts.addAll(storageService.getAllAsBuildParts());
        allParts.addAll(psuService.getAllAsBuildParts());
        allParts.addAll(caseService.getAllAsBuildParts());
        allParts.addAll(coolerService.getAllAsBuildParts());
        return allParts.stream()
                .filter(p -> p.getCategory().equals(lookupCategory) && p.getComponentId() == componentId)
                .findFirst();
    }
}
