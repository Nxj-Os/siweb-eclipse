
package pe.com.eclipsesuites.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmpleadoPortalController {

    @GetMapping("/empleado")
    public String portal(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login?tipo=empleado";
        }

        if (tieneRol(auth, "ROLE_ADMIN")) {
            return "redirect:/empleado/recepcion";
        }

        if (tieneRol(auth, "ROLE_RECEPCION")) {
            return "redirect:/empleado/recepcion";
        }

        if (tieneRol(auth, "ROLE_LIMPIEZA")) {
            return "redirect:/empleado/limpieza";
        }

        if (tieneRol(auth, "ROLE_ROOMSERVICE")) {
            return "redirect:/empleado/roomservice";
        }

        return "redirect:/login?tipo=empleado";
    }

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(rol));
    }

    @GetMapping("/empleado/admin")
    public String administrador() {
        return "empleado/admin";
    }
}
