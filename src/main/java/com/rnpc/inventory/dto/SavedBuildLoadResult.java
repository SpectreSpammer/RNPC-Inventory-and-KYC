package com.rnpc.inventory.dto;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Response shape for "Load into Builder" (see SavedBuildController/buildPc.html JS).
 * {@code selections} carries only the items that still resolve in the live catalog (category ->
 * componentId, matching /order/review's param naming so the picker can reuse ALL_PARTS lookups);
 * {@code unavailableLabels} names whatever was skipped so the customer isn't left wondering why
 * their build came back incomplete.
 */
public class SavedBuildLoadResult {

    private final LinkedHashMap<String, Integer> selections;
    private final List<String> unavailableLabels;

    public SavedBuildLoadResult(LinkedHashMap<String, Integer> selections, List<String> unavailableLabels) {
        this.selections = selections;
        this.unavailableLabels = unavailableLabels;
    }

    public LinkedHashMap<String, Integer> getSelections() {
        return selections;
    }

    public List<String> getUnavailableLabels() {
        return unavailableLabels;
    }
}
