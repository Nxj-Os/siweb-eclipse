package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.com.eclipsesuites.service.IReservaService;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final IReservaService reservaService;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @GetMapping("/inicio")
    public String inicio(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        model.addAttribute("tiposHabitacion", reservaService.listarTiposHabitacion());
        model.addAttribute("tarifas", reservaService.listarTarifas());
        model.addAttribute("demoMode", demoMode);
        return "inicio";
    }
}
