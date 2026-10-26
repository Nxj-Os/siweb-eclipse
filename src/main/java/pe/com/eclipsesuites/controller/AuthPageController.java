package pe.com.eclipsesuites.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;

@Controller
public class AuthPageController {

    @Value("${app.auth.email.dominios-permitidos:gmail.com,hotmail.com,outlook.com,yahoo.com}")
    private String dominiosCorreoConfigurados;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("tipoLogin", "cliente".equalsIgnoreCase(tipo) ? "cliente" : "empleado");
        model.addAttribute("loginError", error != null);
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroPage(Model model) {
        List<String> dominios = Arrays.stream(dominiosCorreoConfigurados.split(","))
                .map(String::trim)
                .filter(dominio -> !dominio.isEmpty())
                .toList();
        model.addAttribute("dominiosCorreoPermitidos", String.join(",", dominios));
        return "auth/registro";
    }
}