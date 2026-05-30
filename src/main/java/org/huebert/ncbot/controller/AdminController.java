package org.huebert.ncbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {

    // Redirect /admin to /admin/index.html so Spring serves the static file
    @GetMapping
    public String dashboard() {
        log.debug("admin dashboard accessed");
        return "redirect:/admin/index.html";
    }
}
