package pe.com.eclipsesuites.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import pe.com.eclipsesuites.service.IReservaService;

@Controller
@RequiredArgsConstructor
public class EmpleadoFrontendController {
    private final IReservaService reservaService;

    @GetMapping("/empleado/recepcion")
    public String rack() { return "recepcion/rack"; }

    @GetMapping("/empleado/checkout")
    public String checkout() { return "recepcion/checkout"; }

    @GetMapping("/empleado/limpieza")
    public String limpieza() { return "operaciones/turnaround-limpieza"; }

    @GetMapping("/empleado/averias")
    public String averias() { return "operaciones/reporte-averias"; }

    @GetMapping("/empleado/reportes")
    public String reportes() { return "reportes/panel-reportes"; }
    @GetMapping("/empleado/roomservice")
    public String roomservice() {
        // Panel del empleado con las comandas reales; la carta del huésped
        // vive en /menu-suite y ya no comparte template con este dashboard.
        return "roomservice/comandas";
    }
    @GetMapping("/empleado/extension")
    public String extension(Model model) {
        model.addAttribute("reservasEnCurso", reservaService.listarReservasEnCurso());
        return "roomservice/extension-tiempo";
    }

}
