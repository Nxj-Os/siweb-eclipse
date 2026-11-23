package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.com.eclipsesuites.service.IReporteDashboardService;
import pe.com.eclipsesuites.service.IReporteExcelService;

import java.time.LocalDate;

/**
 * Dashboard gerencial de reportes. La ruta {@code /reportes} exige rol ADMIN
 * (regla definida en {@code SecurityConfig}).
 */
@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReportePanelController {

    static final int DIAS_POR_DEFECTO = 7;
    static final int DIAS_MAXIMOS = 366;

    private final IReporteDashboardService dashboardService;

    @GetMapping
    public String panel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate hoy = LocalDate.now(IReporteExcelService.ZONA_HOTEL);

        // Rango por defecto: últimos 7 días. Se corrigen rangos invertidos, futuros o demasiado largos.
        LocalDate a = desde != null ? desde
                : (hasta != null ? hasta.minusDays(DIAS_POR_DEFECTO - 1L) : hoy.minusDays(DIAS_POR_DEFECTO - 1L));
        LocalDate b = hasta != null ? hasta : hoy;
        LocalDate inicio = a.isAfter(b) ? b : a;
        LocalDate fin = a.isAfter(b) ? a : b;
        if (fin.isAfter(hoy)) {
            fin = hoy;
        }
        if (inicio.isAfter(fin)) {
            inicio = fin;
        }
        LocalDate limite = fin.minusDays(DIAS_MAXIMOS - 1L);
        if (inicio.isBefore(limite)) {
            inicio = limite;
        }

        model.addAttribute("hoy", hoy);
        model.addAttribute("dash", dashboardService.obtenerDashboard(inicio, fin));
        return "reportes/panel-reportes";
    }
}
