package pe.com.eclipsesuites.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("tipoLogin", "cliente".equalsIgnoreCase(tipo) ? "cliente" : "empleado");
        model.addAttribute("loginError", error != null);
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroPage() {
        return "auth/registro";
    }
}