package pe.com.eclipsesuites.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import pe.com.eclipsesuites.service.impl.AccesoRoomServiceNoPermitidoException;
import pe.com.eclipsesuites.service.impl.ChatbotLimiteExcedidoException;
import pe.com.eclipsesuites.service.impl.DatosRegistroNoDisponiblesException;
import pe.com.eclipsesuites.service.impl.LoginTemporalmenteBloqueadoException;
import pe.com.eclipsesuites.service.impl.PedidoNoEncontradoException;
import pe.com.eclipsesuites.service.impl.StockInsuficienteException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return construir(HttpStatus.UNAUTHORIZED, "No autorizado",
                "Credenciales inválidas", null, request);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiErrorDTO> manejarCredencialesInvalidas(RuntimeException ex,
                                                                    HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "No autorizado",
                "Credenciales inválidas", null, request);
    }

    @ExceptionHandler(LoginTemporalmenteBloqueadoException.class)
    public ResponseEntity<ApiErrorDTO> manejarLoginBloqueado(
            LoginTemporalmenteBloqueadoException ex, HttpServletRequest request) {
        return construir(HttpStatus.TOO_MANY_REQUESTS, "Acceso temporalmente bloqueado",
                "Credenciales inválidas. Intenta nuevamente en "
                        + ex.getSegundosRestantes() + " segundos.",
                null, request);
    }

    @ExceptionHandler(DatosRegistroNoDisponiblesException.class)
    public ResponseEntity<ApiErrorDTO> manejarDatosRegistroNoDisponibles(
            DatosRegistroNoDisponiblesException ex, HttpServletRequest request) {
        String mensaje = ex.getMessage() == null
                ? "No se pudo completar el registro; verifica la información ingresada"
                : ex.getMessage();
        return construir(HttpStatus.CONFLICT, "Conflicto", mensaje, null, request);
    }

    @ExceptionHandler(ChatbotLimiteExcedidoException.class)
    public ResponseEntity<ApiErrorDTO> manejarLimiteChatbot(
            ChatbotLimiteExcedidoException ex, HttpServletRequest request) {
        String mensaje = ex.getMessage() == null
                ? "Demasiadas consultas. Intenta en unos minutos"
                : ex.getMessage();
        return construir(HttpStatus.TOO_MANY_REQUESTS, "Límite de consultas", mensaje,
                null, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDTO> manejarArgumentoInvalido(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        String mensaje = ex.getMessage() == null ? "Solicitud inválida" : ex.getMessage();
        return construir(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje, null, request);
    }

    @ExceptionHandler(AccesoRoomServiceNoPermitidoException.class)
    public ResponseEntity<ApiErrorDTO> manejarAccesoRoomService(
            AccesoRoomServiceNoPermitidoException ex, HttpServletRequest request) {
        String mensaje = ex.getMessage() == null
                ? "Necesitas validar el check-in de tu reserva para acceder"
                : ex.getMessage();
        return construir(HttpStatus.FORBIDDEN, "Servicio bloqueado", mensaje, null, request);
    }

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<ApiErrorDTO> manejarPedidoNoEncontrado(
            PedidoNoEncontradoException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "Pedido no encontrado",
                ex.getMessage(), null, request);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ApiErrorDTO> manejarStockInsuficiente(
            StockInsuficienteException ex, HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        detalles.put("producto", ex.getNombreProducto());
        detalles.put("stockDisponible", String.valueOf(ex.getStockDisponible()));
        return construir(HttpStatus.CONFLICT, "Stock insuficiente",
                "La existencia de " + ex.getNombreProducto()
                        + " cambió; quedan " + ex.getStockDisponible() + " unidades.",
                detalles, request);
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
