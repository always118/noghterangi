package com.noghterangi.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping("/admin/login")
    public String login() { return "admin/login"; }

    @GetMapping({"/admin", "/admin/"})
    public String home() { return "admin/home"; }
}
