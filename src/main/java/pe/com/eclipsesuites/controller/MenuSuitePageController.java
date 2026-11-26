package pe.com.eclipsesuites.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página pública de la carta de room service (Figma C-09). No requiere
 * sesión: el token de check-in llega en la URL (?token=...) y la
 * validación real la hace la API de /api/roomservice. Si el huésped
 * tiene sesión iniciada, el enlace "Inicio" de la barra inferior lo
 * devuelve al catálogo; si no, al landing público.
 */
@Controller
public class MenuSuitePageController {

    @GetMapping("/menu-suite")
    public String menuSuite(Authentication authentication, Model model) {
        boolean logueado = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        model.addAttribute("logueado", logueado);
        return "roomservice/menu-suite";
    }
}
