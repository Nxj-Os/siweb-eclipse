package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

@Controller
@RequiredArgsConstructor
public class AsistentePageController {

    private static final String ROL_CLIENTE = "ROLE_CLIENTE";
    private static final String INVITADO = "Usuario";

    private final IClienteAuthService clienteAuthService;
    private final IEmpleadoAuthService empleadoAuthService;

    @GetMapping("/asistente")
    public String asistente(Model model, Authentication authentication) {
        model.addAttribute("nombreUsuario", nombreDe(authentication));
        model.addAttribute("esCliente", esCliente(authentication));
        return "asistente";
    }

    private boolean esCliente(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(autoridad -> ROL_CLIENTE.equals(autoridad.getAuthority()));
    }

    private String nombreDe(Authentication authentication) {
        if (!esCliente(authentication)) {
            boolean esEmpleado = authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal());
            if (!esEmpleado) {
                return INVITADO;
            }
            try {
                Empleado empleado = empleadoAuthService.buscarPorUsername(
                        authentication.getName());
                return empleado.getNombre();
            } catch (RuntimeException ex) {
                return INVITADO;
            }
        }
        try {
            Cliente cliente = clienteAuthService.buscarPorCorreo(authentication.getName());
            return cliente.getNombre();
        } catch (RuntimeException ex) {
            return INVITADO;
        }
    }
}
