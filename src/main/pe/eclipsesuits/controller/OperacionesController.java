package pe.com.eclipsesuites.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.AveriaRequestDTO;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.OrdenLimpieza;
import pe.com.eclipsesuites.entity.ReporteAveria;
import pe.com.eclipsesuites.service.ILimpiezaService;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * API del módulo de Operaciones: turnaround de limpieza (RF-13 a RF-15)
 * y averías técnicas (RF-16). Pensada para el flujo móvil de 2 toques (RNF-06):
 * POST /iniciar (toque 1) y POST /finalizar (toque 2).
 */
@RestController
@RequestMapping("/api/operaciones")
public class OperacionesController {

    private final ILimpiezaService limpiezaService;

    public OperacionesController(ILimpiezaService limpiezaService) {
        this.limpiezaService = limpiezaService;
    }

    @GetMapping("/limpieza/pendientes")
    public ResponseEntity<List<Habitacion>> listarPendientes() {
        return ResponseEntity.ok(limpiezaService.listarHabitacionesPendientes());
    }

    @PostMapping("/limpieza/{idHabitacion}/iniciar")
    public ResponseEntity<OrdenLimpieza> iniciarLimpieza(@PathVariable Integer idHabitacion,
                                                           @RequestParam Integer idEmpleado) {
        return ResponseEntity.ok(limpiezaService.iniciarLimpieza(idHabitacion, idEmpleado));
    }

    @PostMapping("/limpieza/ordenes/{idOrden}/finalizar")
    public ResponseEntity<OrdenLimpieza> finalizarLimpieza(@PathVariable Integer idOrden,
                                                             @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(limpiezaService.finalizarLimpieza(idOrden, observaciones));
    }

    @PostMapping("/averias")
    public ResponseEntity<ReporteAveria> registrarAveria(@Valid @RequestBody AveriaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(limpiezaService.registrarAveria(request));
    }

    @GetMapping("/averias/habitacion/{idHabitacion}")
    public ResponseEntity<List<ReporteAveria>> listarAveriasPorHabitacion(@PathVariable Integer idHabitacion) {
        return ResponseEntity.ok(limpiezaService.listarAveriasPorHabitacion(idHabitacion));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
