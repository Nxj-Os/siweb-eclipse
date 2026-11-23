package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import pe.com.eclipsesuites.service.IReporteExcelService;

import java.time.LocalDate;

/**
 * Reportes de gestión. Todas las rutas {@code /reportes/**} exigen rol ADMIN
 * (regla definida en {@code SecurityConfig}).
 */
@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteExcelController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteExcelService reporteExcelService;

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("hoy", LocalDate.now(IReporteExcelService.ZONA_HOTEL));
        return "reportes/panel-reportes";
    }

    @GetMapping("/cierre-caja")
    public ResponseEntity<byte[]> cierreCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate hoy = LocalDate.now(IReporteExcelService.ZONA_HOTEL);
        LocalDate dia = fecha != null ? fecha : hoy;
        if (dia.isAfter(hoy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cerrar la caja de una fecha futura");
        }
        return descarga(reporteExcelService.generarCierreCajaDiario(dia), "cierre-caja-" + dia + ".xlsx");
    }

    @GetMapping("/ocupacion")
    public ResponseEntity<byte[]> ocupacion() {
        String nombre = "ocupacion-suites-" + LocalDate.now(IReporteExcelService.ZONA_HOTEL) + ".xlsx";
        return descarga(reporteExcelService.generarOcupacionSuites(), nombre);
    }

    private static ResponseEntity<byte[]> descarga(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }
}
