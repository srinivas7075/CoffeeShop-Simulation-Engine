package com.coffeeshop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "☕ Coffee Shop Backend is Running! Access the frontend at http://localhost:5173";
    }
}
