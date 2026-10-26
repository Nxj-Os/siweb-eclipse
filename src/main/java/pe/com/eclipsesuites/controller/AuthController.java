package pe.com.eclipsesuites.controller;

import com.google.common.base.Splitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.ClienteLoginRequestDTO;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.dto.request.LoginRequestDTO;
import pe.com.eclipsesuites.dto.response.DisponibilidadResponseDTO;
import pe.com.eclipsesuites.dto.response.LoginResponseDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.impl.DisponibilidadRateLimiter;
import pe.com.eclipsesuites.service.impl.IntentosLoginService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final char SEPARADOR_PERMISOS = ',';

    private final IEmpleadoAuthService empleadoAuthService;
    private final IClienteAuthService clienteAuthService;
    private final SecurityContextRepository securityContextRepository;
    private final DisponibilidadRateLimiter disponibilidadRateLimiter;
    private final IntentosLoginService intentosLoginService;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSegura;

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> obtenerTokenCsrf(CsrfToken csrfToken,
                                                                 HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", csrfToken.getToken())
                .httpOnly(false)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("token", csrfToken.getToken()));
    }

    @GetMapping("/client/disponibilidad")
    public ResponseEntity<DisponibilidadResponseDTO> verificarDisponibilidad(
            @RequestParam String campo,
            @RequestParam String valor,
            @RequestParam(required = false) String tipoDocumento,
            HttpServletRequest request) {
        if (!disponibilidadRateLimiter.permitir(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .cacheControl(CacheControl.noStore())
                    .body(new DisponibilidadResponseDTO(false,
                            "Demasiadas consultas. Espera un minuto e inténtalo nuevamente."));
        }

        try {
            boolean disponible = clienteAuthService.estaDisponible(campo, valor, tipoDocumento);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(new DisponibilidadResponseDTO(disponible,
                            disponible ? "Disponible" : "Ya registrado"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .cacheControl(CacheControl.noStore())
                    .body(new DisponibilidadResponseDTO(false, ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        String identidad = request.getUsername();
        intentosLoginService.verificarPermitido(ip, identidad);
        Empleado empleado;
        try {
            empleado = empleadoAuthService.autenticar(identidad, request.getPassword());
        } catch (AuthenticationException ex) {
            intentosLoginService.registrarFallo(ip, identidad);
            intentosLoginService.verificarPermitido(ip, identidad);
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas");
        }
        intentosLoginService.registrarExito(ip, identidad);

        UserDetails userDetails = empleadoAuthService.loadUserByUsername(
                empleado.getUsernameLogin());

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        rotarSesion(httpRequest);
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
        String ip = httpRequest.getRemoteAddr();
        String identidad = request.getCorreoElectronico();
        intentosLoginService.verificarPermitido(ip, identidad);
        Cliente cliente;
        try {
            cliente = clienteAuthService.autenticar(identidad, request.getPassword());
        } catch (AuthenticationException ex) {
            intentosLoginService.registrarFallo(ip, identidad);
            intentosLoginService.verificarPermitido(ip, identidad);
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas");
        }
        intentosLoginService.registrarExito(ip, identidad);
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

    private void iniciarSesionCliente(Cliente cliente, HttpServletRequest request,
                                       HttpServletResponse response) {
        UserDetails userDetails = clienteAuthService.crearUserDetails(cliente);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        rotarSesion(request);
        securityContextRepository.saveContext(context, request, response);
    }

    private void rotarSesion(HttpServletRequest request) {
        request.getSession(true);
        request.changeSessionId();
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
