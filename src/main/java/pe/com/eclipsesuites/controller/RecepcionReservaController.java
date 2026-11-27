package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.service.IReservaService;

@Controller
@RequestMapping("/recepcion/reservas")
@RequiredArgsConstructor
public class RecepcionReservaController {

    private final IReservaService reservaService;

    @GetMapping
    public String pendientes(Model model) {
        model.addAttribute("reservas", reservaService.listarReservasPendientes());
        model.addAttribute("reservasCheckIn", reservaService.listarReservasPendientesCheckIn());
        model.addAttribute("extensionesPendientes",
                reservaService.listarExtensionesPendientesValidacion());
        return "recepcion/reservas-pendientes";
    }

    @PostMapping("/check-in")
    public String validarCheckIn(@RequestParam String tokenQr,
                                 RedirectAttributes redirectAttributes) {
        try {
            var reserva = reservaService.registrarCheckIn(tokenQr);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Check-in validado para la reserva #" + reserva.getId() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
        }
        return "redirect:/recepcion/reservas";
    }

    @PostMapping("/{id}/confirmar")
    public String confirmarPago(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservaService.actualizarEstadoPendiente(id, EstadoReserva.CONFIRMADA);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Pago validado y reserva confirmada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorReserva", ex.getMessage());
        }
        return "redirect:/recepcion/reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservaService.actualizarEstadoPendiente(id, EstadoReserva.CANCELADA);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud de reserva cancelada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorReserva", ex.getMessage());
        }
        return "redirect:/recepcion/reservas";
    }

    @PostMapping("/{id}/extensiones")
    public String extenderEstadia(@PathVariable Long id,
                                  @RequestParam Integer horas,
                                  RedirectAttributes redirectAttributes) {
        try {
            var extension = reservaService.extenderReservaRecepcion(id, horas);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Extensión registrada: nueva salida "
                            + extension.getFechaFin().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            + " · S/ " + extension.getMontoTotal() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
        }
        return "redirect:/empleado/extension";
    }

    @PostMapping("/extensiones/{extensionId}/validar")
    public String validarExtension(@PathVariable Long extensionId,
                                   @RequestParam boolean aprobar,
                                   RedirectAttributes redirectAttributes) {
        try {
            var extension = reservaService.validarExtensionRecepcion(extensionId, aprobar);
            redirectAttributes.addFlashAttribute("mensaje", aprobar
                    ? "Extensión aprobada; nueva salida " + extension.getFechaFin() + "."
                    : "Extensión rechazada y retención liberada.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
        }
        return "redirect:/recepcion/reservas";
    }
}
