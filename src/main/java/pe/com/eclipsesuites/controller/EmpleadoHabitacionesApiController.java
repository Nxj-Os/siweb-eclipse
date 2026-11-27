package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.BloqueoHabitacionRequestDTO;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.service.IReservaService;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/empleado/api/habitaciones")
@RequiredArgsConstructor
public class EmpleadoHabitacionesApiController {
    private final HabitacionRepository habitacionRepository;
    private final IReservaService reservaService;

    public record HabitacionVista(Integer id, String numero, Short piso,
                                  String tipo, String estado, String motivoBloqueo,
                                  java.time.OffsetDateTime bloqueoHasta) {}
    public record ErrorHabitacionDTO(String mensaje) {}

    @GetMapping
    public ResponseEntity<List<HabitacionVista>> listar() {
        List<HabitacionVista> habitaciones = habitacionRepository.findAllConTipo().stream()
            .sorted(Comparator.comparing(Habitacion::getNumHabitacion))
            .map(h -> new HabitacionVista(h.getId(), h.getNumHabitacion(), h.getPiso(),
                h.getTipoHabitacion() == null ? "Sin tipo" : h.getTipoHabitacion().getNombreTematico(),
                h.getEstadoRack(), h.getMotivoBloqueo(), h.getBloqueoTemporalHasta()))
            .toList();
        return ResponseEntity.ok(habitaciones);
    }

    @PostMapping("/{habitacionId}/bloqueo")
    public ResponseEntity<?> bloquear(@PathVariable Integer habitacionId,
                                      @Valid @RequestBody BloqueoHabitacionRequestDTO request) {
        try {
            Habitacion habitacion = reservaService.bloquearHabitacion(
                    habitacionId, request.getMotivo());
            return ResponseEntity.ok(vista(habitacion));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorHabitacionDTO(exception.getMessage()));
        }
    }

    @DeleteMapping("/{habitacionId}/bloqueo")
    public ResponseEntity<?> liberar(@PathVariable Integer habitacionId) {
        try {
            return ResponseEntity.ok(vista(reservaService.liberarHabitacion(habitacionId)));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorHabitacionDTO(exception.getMessage()));
        }
    }

    private HabitacionVista vista(Habitacion habitacion) {
        return new HabitacionVista(habitacion.getId(), habitacion.getNumHabitacion(),
                habitacion.getPiso(), habitacion.getTipoHabitacion().getNombreTematico(),
                habitacion.getEstadoRack(), habitacion.getMotivoBloqueo(),
                habitacion.getBloqueoTemporalHasta());
    }
}
