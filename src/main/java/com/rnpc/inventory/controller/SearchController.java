package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.SearchResults;
import com.rnpc.inventory.dto.SearchSuggestion;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;

    private final SearchService searchService;
    private final NotificationService notificationService;

    @Autowired
    public SearchController(SearchService searchService, NotificationService notificationService) {
        this.searchService = searchService;
        this.notificationService = notificationService;
    }

    // Not signed in, or an admin: redirect to "/" - unlike DashboardController.showHome
    // (DashboardController.java:69-71), which forwards to /index.html because it OWNS "/" and a
    // redirect there would loop. /search has no such constraint, and forwarding here would serve
    // the landing page while the address bar and history still read /search?q=..., leaving that
    // query visible on a page the visitor was never signed in to see.
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q,
                          Authentication authentication, Model model) {
        if (!isSignedIn(authentication) || isAdmin(authentication)) {
            return "redirect:/";
        }

        String username = authentication.getName();
        String query = q == null ? "" : q.trim();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }
        model.addAttribute("query", query);

        if (query.length() >= MIN_QUERY_LENGTH) {
            SearchResults results = searchService.search(username, query);
            model.addAttribute("results", results);
        }

        // Topbar chrome - same shape as DashboardController.java:81-87.
        model.addAttribute("currentUsername", username);
        model.addAttribute("currentRole", "Customer");
        model.addAttribute("unreadNotifications", notificationService.getUnreadCountForUser(username));
        model.addAttribute("recentNotifications",
                notificationService.getForUser(username).stream().limit(15).collect(Collectors.toList()));

        return "search/searchResults";
    }

    // Topbar live-suggestion dropdown (fetch() from app-script's input handler in layout-app.html).
    // Same sign-in/admin gate as GET /search above, but returns an empty list with HTTP 200 instead
    // of a redirect - fetch() follows redirects and would otherwise hand the JS the landing page's
    // HTML in place of JSON.
    @GetMapping("/search/suggest")
    @ResponseBody
    public List<SearchSuggestion> suggest(@RequestParam(value = "q", required = false) String q,
                                           Authentication authentication) {
        if (!isSignedIn(authentication) || isAdmin(authentication)) {
            return Collections.emptyList();
        }

        String query = q == null ? "" : q.trim();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }
        if (query.length() < MIN_QUERY_LENGTH) {
            return Collections.emptyList();
        }

        return searchService.suggest(authentication.getName(), query);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
