package pe.com.eclipsesuites.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.ActualizarEstadoPedidoRequestDTO;
import pe.com.eclipsesuites.dto.request.CrearPedidoRequestDTO;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.service.IRoomServiceService;

/**
 * API de la carta de room service para el huésped. Los endpoints de
 * carta, creación y consulta funcionan con el token de check-in (sin
 * sesión) y quedan habilitados en SecurityConfig; el PATCH de estado
 * exige empleado autenticado (ROOMSERVICE o ADMIN).
 */
@RestController
@RequestMapping("/api/roomservice")
@RequiredArgsConstructor
public class RoomServiceController {

    private final IRoomServiceService roomServiceService;

    @GetMapping("/menu")
    public CartaResponseDTO obtenerCarta(@RequestParam String token,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) Integer idCategoria) {
        return roomServiceService.obtenerCarta(token, q, idCategoria);
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> crearPedido(
            @Valid @RequestBody CrearPedidoRequestDTO request) {
        PedidoResponseDTO pedido = roomServiceService.crearPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/pedidos/{codigo}")
    public PedidoResponseDTO obtenerPedido(@PathVariable String codigo,
                                           @RequestParam String token) {
        return roomServiceService.obtenerPedido(token, codigo);
    }

    @PatchMapping("/pedidos/{codigo}/estado")
    public PedidoResponseDTO actualizarEstado(
            @PathVariable String codigo,
            @Valid @RequestBody ActualizarEstadoPedidoRequestDTO request) {
        return roomServiceService.actualizarEstadoPedido(codigo, request.getEstado());
    }
}
