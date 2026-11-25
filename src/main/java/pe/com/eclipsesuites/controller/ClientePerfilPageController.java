package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.com.eclipsesuites.service.IClientePerfilService;

@Controller
@RequiredArgsConstructor
public class ClientePerfilPageController {

    private final IClientePerfilService clientePerfilService;

    @GetMapping("/cliente/perfil")
    public String perfil(Authentication authentication, Model model) {
        String correo = authentication.getName();
        model.addAttribute("perfil", clientePerfilService.obtenerPerfil(correo));
        model.addAttribute("reservas", clientePerfilService.listarReservas(correo));
        return "cliente/perfil";
    }
}
