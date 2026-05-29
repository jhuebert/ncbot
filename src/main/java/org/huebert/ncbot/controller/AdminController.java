package org.huebert.ncbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Redirect /admin to /admin/index.html so Spring serves the static file
    @GetMapping
    public String dashboard() {
        return "redirect:/admin/index.html";
    }
}
