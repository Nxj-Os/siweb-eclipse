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
}
