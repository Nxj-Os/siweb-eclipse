package pe.com.eclipsesuites.controller;

import com.google.common.base.Splitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.LoginRequestDTO;
import pe.com.eclipsesuites.dto.response.LoginResponseDTO;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final char SEPARADOR_PERMISOS = ',';

    private final IEmpleadoAuthService empleadoAuthService;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        Empleado empleado = empleadoAuthService.autenticar(
                request.getUsername(), request.getPassword());

        UserDetails userDetails = empleadoAuthService.loadUserByUsername(
                empleado.getUsernameLogin());

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return construirRespuesta(empleado, "Login exitoso");
    }

    @GetMapping("/me")
    public LoginResponseDTO me(Authentication authentication) {
        Empleado empleado = empleadoAuthService.buscarPorUsername(authentication.getName());
        return construirRespuesta(empleado, "Sesión activa");
    }

    private LoginResponseDTO construirRespuesta(Empleado empleado, String mensaje) {
        return LoginResponseDTO.builder()
                .username(empleado.getUsernameLogin())
                .nombreCompleto(empleado.getNombre() + " " + empleado.getApellido())
                .rol(empleado.getRol().getNombreRol())
                .permisos(parsearPermisos(empleado.getRol().getPermisosSistema()))
                .mensaje(mensaje)
                .build();
    }

    private List<String> parsearPermisos(String permisos) {
        if (StringUtils.isBlank(permisos)) {
            return List.of();
        }
        return Splitter.on(SEPARADOR_PERMISOS)
                .trimResults()
                .omitEmptyStrings()
                .splitToList(permisos);
    }
}
