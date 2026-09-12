package com.rnpc.inventory.dto;

/**
 * One row of the topbar live-suggestion dropdown (SearchController's GET /search/suggest).
 * Serialized straight to JSON by @ResponseBody - field names below are exactly the {type, id,
 * label, sub, url} shape the frontend expects.
 */
public class SearchSuggestion {

    private final String type;
    private final Long id;
    private final String label;
    private final String sub;
    private final String url;

    public SearchSuggestion(String type, Long id, String label, String sub, String url) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.sub = sub;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getSub() {
        return sub;
    }

    public String getUrl() {
        return url;
    }
}
