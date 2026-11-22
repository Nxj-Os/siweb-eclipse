package pe.com.eclipsesuites.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página pública de la carta de room service (Figma C-09). No requiere
 * sesión: el token de check-in llega en la URL (?token=...) y la
 * validación real la hace la API de /api/roomservice.
 */
@Controller
public class MenuSuitePageController {

    @GetMapping("/menu-suite")
    public String menuSuite() {
        return "roomservice/menu-suite";
    }
}
