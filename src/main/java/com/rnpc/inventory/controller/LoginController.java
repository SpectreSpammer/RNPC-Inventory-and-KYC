package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class LoginController {


    @Autowired
    private UserService userService;

    /*
    //Display login page

    @GetMapping("/login")
    public String showLoginPage(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "username", required = false) String username) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "login";
    }


     // Process login form submission

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {

        // Authenticate user
        Optional<User> userOpt = userService.authenticateUser(username, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Store user in session
            session.setAttribute("loggedInUser", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole().toString());

            // Redirect based on role
            return userService.getRedirectUrl(user);
        } else {
            // Authentication failed
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", username);
            return "login";
        }
    }


     // Handle logout

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


     // Home page redirect to login

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }


     // Admin dashboard (for testing)

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login?error=unauthorized";
        }
        model.addAttribute("user", user);
        return "admin-dashboard";
    }


      //Customer dashboard (for testing)

    @GetMapping("/{username}")
    public String customerDashboard(@PathVariable String username,
                                    HttpSession session,
                                    Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !user.getUsername().equals(username)) {
            return "redirect:/login?error=unauthorized";
        }
        model.addAttribute("user", user);
        return "customer-dashboard";
    }
    */
}
