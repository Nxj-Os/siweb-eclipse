package pe.com.eclipsesuites.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmpleadoFrontendController {
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
    public String roomservice() { return "roomservice/menu-suite"; }

    @GetMapping("/empleado/extension")
    public String extension() { return "roomservice/extension-tiempo"; }

}
