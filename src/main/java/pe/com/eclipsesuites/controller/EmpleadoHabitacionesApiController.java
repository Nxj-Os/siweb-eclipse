package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.entity.Habitacion;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/empleado/api/habitaciones")
@RequiredArgsConstructor
public class EmpleadoHabitacionesApiController {
    private final HabitacionRepository habitacionRepository;

    public record HabitacionVista(Integer id, String numero, Short piso,
                                 String tipo, String estado) {}

    @GetMapping
    public ResponseEntity<List<HabitacionVista>> listar() {
        List<HabitacionVista> habitaciones = habitacionRepository.findAll().stream()
            .sorted(Comparator.comparing(Habitacion::getNumHabitacion))
            .map(h -> new HabitacionVista(h.getId(), h.getNumHabitacion(), h.getPiso(),
                h.getTipoHabitacion() == null ? "Sin tipo" : h.getTipoHabitacion().getNombreTematico(),
                h.getEstadoRack()))
            .toList();
        return ResponseEntity.ok(habitaciones);
    }
}
