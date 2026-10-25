package pe.com.eclipsesuites.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.com.eclipsesuites.dto.response.ApiErrorDTO;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> manejarValidacion(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return construir(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Datos inválidos", detalles, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> manejarJsonInvalido(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo de la petición no es un JSON válido", null, request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorDTO> manejarCuentaInactiva(DisabledException ex,
                                                             HttpServletRequest request) {
        log.warn("Login rechazado por cuenta inactiva: {}", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "No autorizado",
                "La cuenta del empleado está inactiva", null, request);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiErrorDTO> manejarCredencialesInvalidas(RuntimeException ex,
                                                                    HttpServletRequest request) {
        log.warn("Login rechazado: {}", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, "No autorizado",
                "Credenciales inválidas", null, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDTO> manejarArgumentoInvalido(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        String mensaje = ex.getMessage() == null ? "Solicitud inválida" : ex.getMessage();
        if (mensaje.toLowerCase().contains("ya existe")) {
            return construir(HttpStatus.CONFLICT, "Conflicto", mensaje, null, request);
        }
        return construir(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje, null, request);
    }

    private ResponseEntity<ApiErrorDTO> construir(HttpStatus status, String error, String mensaje,
                                                  Map<String, String> detalles,
                                                  HttpServletRequest request) {
        ApiErrorDTO body = ApiErrorDTO.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(error)
                .mensaje(mensaje)
                .detalles(detalles)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
