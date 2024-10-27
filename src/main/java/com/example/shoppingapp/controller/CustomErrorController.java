package com.example.shoppingapp.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        // pobranie statusu błędu
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // Sprawdzanie kodu błędu 404 i redirect home
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if (statusCode == 404) {
                return "redirect:/home";
            }
        }

        // inne bledy (w toku, ale bedzie duzo...)
        return "error";
    }
}
