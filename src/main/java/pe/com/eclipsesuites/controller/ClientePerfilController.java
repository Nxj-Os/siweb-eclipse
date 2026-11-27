package pe.com.eclipsesuites.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.PerfilClienteUpdateDTO;
import pe.com.eclipsesuites.dto.response.BoletaReservaDTO;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.dto.response.ReservaResumenDTO;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IClientePerfilService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/client")
@RequiredArgsConstructor
public class ClientePerfilController {

    private final IClientePerfilService clientePerfilService;
    private final IClienteAuthService clienteAuthService;

    @GetMapping("/perfil")
    public PerfilClienteResponseDTO perfil(Authentication authentication) {
        return clientePerfilService.obtenerPerfil(authentication.getName());
    }

    @PutMapping("/perfil")
    public PerfilClienteResponseDTO actualizar(@Valid @RequestBody PerfilClienteUpdateDTO request,
                                               Authentication authentication) {
        clienteAuthService.actualizarPerfil(authentication.getName(), request);
        return clientePerfilService.obtenerPerfil(authentication.getName());
    }

    @GetMapping("/reservas")
    public List<ReservaResumenDTO> reservas(Authentication authentication) {
        return clientePerfilService.listarReservas(authentication.getName());
    }

    @GetMapping("/reservas/{id}/boleta")
    public BoletaReservaDTO boleta(@PathVariable Long id, Authentication authentication) {
        return clientePerfilService.obtenerBoleta(authentication.getName(), id);
    }

    @DeleteMapping("/cuenta")
    public ResponseEntity<Map<String, String>> eliminarCuenta(Authentication authentication,
                                                              HttpServletRequest request) {
        clienteAuthService.eliminarCuenta(authentication.getName());
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return ResponseEntity.ok(Map.of(
                "mensaje", "Tu cuenta fue eliminada correctamente"));
    }
}
