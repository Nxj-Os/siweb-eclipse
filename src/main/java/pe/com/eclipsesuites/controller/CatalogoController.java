package pe.com.eclipsesuites.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import pe.com.eclipsesuites.dto.response.DisponibilidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteDetalleDTO;
import pe.com.eclipsesuites.service.ICatalogoService;
import pe.com.eclipsesuites.service.impl.DisponibilidadRateLimiter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Catálogo público de suites: vistas Thymeleaf y API JSON reutilizable.
 */
@Controller
@RequiredArgsConstructor
public class CatalogoController {

    private static final String ORDEN_RECOMENDADO = "recomendado";

    private final ICatalogoService catalogoService;
    private final DisponibilidadRateLimiter disponibilidadRateLimiter;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @GetMapping("/catalogo")
    public String catalogo(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean soloDisponibles,
            @RequestParam(required = false, defaultValue = ORDEN_RECOMENDADO) String orden,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer modalidadId,
            Model model) {
        List<SuiteCatalogoDTO> suites = catalogoService.listarSuites(fecha, hora, modalidadId);
        boolean soloDisponiblesActivo = Boolean.TRUE.equals(soloDisponibles);

        model.addAttribute("suites",
                filtrar(suites, q, soloDisponiblesActivo, orden));
        model.addAttribute("totalSuites", suites.size());
        model.addAttribute("modalidades", catalogoService.listarModalidades());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("soloDisponibles", soloDisponiblesActivo);
        model.addAttribute("orden", orden);
        model.addAttribute("fecha", fecha);
        model.addAttribute("hora", hora);
        model.addAttribute("modalidadId", modalidadId);
        model.addAttribute("filtrandoHorario", fecha != null && hora != null);
        model.addAttribute("demoMode", demoMode);
        return "catalogo/index";
    }

    @GetMapping("/catalogo/{codTipo}")
    public String detalle(
            @PathVariable String codTipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer modalidadId,
            Model model) {
        SuiteDetalleDTO detalle = catalogoService.obtenerDetalle(codTipo, fecha, hora, modalidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La suite solicitada no existe en el catálogo."));

        model.addAttribute("suite", detalle);
        model.addAttribute("modalidades", catalogoService.listarModalidades());
        model.addAttribute("fecha", fecha);
        model.addAttribute("hora", hora);
        model.addAttribute("modalidadId", modalidadId);
        model.addAttribute("filtrandoHorario", fecha != null && hora != null);
        model.addAttribute("demoMode", demoMode);
        return "catalogo/detalle";
    }

    @GetMapping("/api/catalogo")
    @ResponseBody
    public List<SuiteCatalogoDTO> apiCatalogo(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean soloDisponibles,
            @RequestParam(required = false, defaultValue = ORDEN_RECOMENDADO) String orden,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer modalidadId) {
        return filtrar(catalogoService.listarSuites(fecha, hora, modalidadId), q,
                Boolean.TRUE.equals(soloDisponibles), orden);
    }

    @GetMapping("/api/catalogo/{codTipo}")
    @ResponseBody
    public SuiteDetalleDTO apiDetalle(
            @PathVariable String codTipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer modalidadId) {
        return catalogoService.obtenerDetalle(codTipo, fecha, hora, modalidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La suite solicitada no existe en el catálogo."));
    }

    @GetMapping("/api/catalogo/disponibilidad")
    @ResponseBody
    public DisponibilidadCatalogoDTO disponibilidad(
            @RequestParam Integer tipoHabitacionId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer modalidadId,
            HttpServletRequest request) {
        if (!disponibilidadRateLimiter.permitir(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas consultas de disponibilidad. Intenta nuevamente en un minuto.");
        }
        try {
            return catalogoService.consultarDisponibilidad(tipoHabitacionId, fecha, hora,
                    modalidadId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private List<SuiteCatalogoDTO> filtrar(List<SuiteCatalogoDTO> suites, String q,
                                           boolean soloDisponibles, String orden) {
        String termino = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Stream<SuiteCatalogoDTO> flujo = suites.stream();

        if (!termino.isEmpty()) {
            flujo = flujo.filter(suite ->
                    contiene(suite.getNombre(), termino)
                            || contiene(suite.getCodTipo(), termino)
                            || contiene(suite.getDescripcion(), termino)
                            || suite.getAmenidades().stream()
                            .anyMatch(amenidad -> contiene(amenidad, termino)));
        }
        if (soloDisponibles) {
            flujo = flujo.filter(SuiteCatalogoDTO::isDisponible);
        }

        return flujo.sorted(comparador(orden)).toList();
    }

    private boolean contiene(String valor, String termino) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termino);
    }

    private Comparator<SuiteCatalogoDTO> comparador(String orden) {
        String criterio = orden == null ? ORDEN_RECOMENDADO : orden;
        return switch (criterio) {
            case "precio-asc" -> Comparator.comparing(SuiteCatalogoDTO::getPrecioDesde,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "precio-desc" -> Comparator.comparing(SuiteCatalogoDTO::getPrecioDesde,
                    Comparator.nullsLast((BigDecimal a, BigDecimal b) -> b.compareTo(a)));
            case "nombre" -> Comparator.comparing(SuiteCatalogoDTO::getNombre,
                    String.CASE_INSENSITIVE_ORDER);
            case "disponibilidad" -> Comparator
                    .comparingInt(SuiteCatalogoDTO::getHabitacionesDisponibles).reversed();
            default -> Comparator
                    .comparingInt(SuiteCatalogoDTO::getHabitacionesDisponibles).reversed()
                    .thenComparing(SuiteCatalogoDTO::getNombre, String.CASE_INSENSITIVE_ORDER);
        };
    }
}
