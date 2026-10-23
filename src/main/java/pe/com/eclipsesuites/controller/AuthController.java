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
import pe.com.eclipsesuites.dto.request.ClienteLoginRequestDTO;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.dto.response.LoginResponseDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final char SEPARADOR_PERMISOS = ',';

    private final IEmpleadoAuthService empleadoAuthService;
    private final IClienteAuthService clienteAuthService;
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

    @PostMapping("/client/register")
    public LoginResponseDTO registrarCliente(@Valid @RequestBody ClienteRegistroRequestDTO request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        Cliente cliente = clienteAuthService.registrar(request);
        iniciarSesionCliente(cliente, httpRequest, httpResponse);
        return construirRespuesta(cliente, "Cuenta creada e inicio de sesión exitoso");
    }

    @PostMapping("/client/login")
    public LoginResponseDTO loginCliente(@Valid @RequestBody ClienteLoginRequestDTO request,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse httpResponse) {
        Cliente cliente = clienteAuthService.autenticar(
                request.getCorreoElectronico(), request.getPassword());
        iniciarSesionCliente(cliente, httpRequest, httpResponse);
        return construirRespuesta(cliente, "Login exitoso");
    }

    @GetMapping("/client/me")
    public LoginResponseDTO meCliente(Authentication authentication) {
        Cliente cliente = clienteAuthService.buscarPorCorreo(authentication.getName());
        return construirRespuesta(cliente, "Sesión activa");
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

    private void iniciarSesionCliente(Cliente cliente, HttpServletRequest request,
                                      HttpServletResponse response) {
        UserDetails userDetails = clienteAuthService.crearUserDetails(cliente);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private LoginResponseDTO construirRespuesta(Cliente cliente, String mensaje) {
        return LoginResponseDTO.builder()
                .username(cliente.getCorreoElectronico())
                .nombreCompleto(cliente.getNombre() + " " + cliente.getApellido())
                .rol("CLIENTE")
                .permisos(List.of())
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
