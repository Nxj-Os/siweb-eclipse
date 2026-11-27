package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.service.IClientePerfilService;
import pe.com.eclipsesuites.service.IReservaService;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String ROL_CLIENTE = "ROLE_CLIENTE";

    private final IReservaService reservaService;
    private final IClientePerfilService clientePerfilService;

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
        model.addAttribute("esCliente", tieneRol(authentication, ROL_CLIENTE));
        model.addAttribute("perfilCliente", perfilSiEsCliente(authentication));
        return "inicio";
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream()
                .anyMatch(autoridad -> rol.equals(autoridad.getAuthority()));
    }

    private PerfilClienteResponseDTO perfilSiEsCliente(Authentication authentication) {
        if (!tieneRol(authentication, ROL_CLIENTE)) {
            return null;
        }
        try {
            return clientePerfilService.obtenerPerfil(authentication.getName());
        } catch (RuntimeException excepcion) {
            return null;
        }
    }
}
