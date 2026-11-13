package pe.com.eclipsesuites.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.eclipsesuites.dto.request.ExtensionReservaRequestDTO;
import pe.com.eclipsesuites.dto.request.PagoReservaRequestDTO;
import pe.com.eclipsesuites.dto.request.ReservaRequestDTO;
import pe.com.eclipsesuites.entity.EstadoExtensionReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IReservaService;
import pe.com.eclipsesuites.service.QrCodeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/cliente/reservas")
@RequiredArgsConstructor
public class ClienteReservaController {

    private static final List<MetodoPagoReserva> METODOS_PAGO =
            List.of(MetodoPagoReserva.TARJETA, MetodoPagoReserva.YAPE,
                    MetodoPagoReserva.PLIN, MetodoPagoReserva.EFECTIVO);

    private final IReservaService reservaService;
    private final QrCodeService qrCodeService;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) Integer tipoId, Model model) {
        if (tipoId == null) {
            return "redirect:/inicio";
        }
        ReservaRequestDTO request = model.containsAttribute("reservaRequest")
                ? (ReservaRequestDTO) model.getAttribute("reservaRequest")
                : nuevaSolicitud(tipoId);
        cargarOpcionesFormulario(model, request);
        return "cliente/reserva-paso1";
    }

    @GetMapping("/disponibilidad")
    @ResponseBody
    public Set<Integer> disponibilidad(
            @RequestParam Integer tipoHabitacionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaReserva,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam Integer modalidadId) {
        ModalidadTurno modalidad = reservaService.listarModalidades().stream()
                .filter(item -> item.getId().equals(modalidadId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        int minutos = modalidad.getDuracionHoras().multiply(java.math.BigDecimal.valueOf(60))
                .intValueExact();
        LocalDateTime inicio = fechaReserva.atTime(horaInicio);
        try {
            return reservaService.listarHabitacionesDisponibles(
                    tipoHabitacionId, inicio, inicio.plusMinutes(minutos));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("reservaRequest") ReservaRequestDTO request,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model) {
        if (bindingResult.hasErrors()) {
            cargarOpcionesFormulario(model, request);
            return "cliente/reserva-paso1";
        }

        try {
            ReservaEstadia reserva = reservaService.crearReserva(
                    authentication.getName(), request);
            return "redirect:/cliente/reservas/" + reserva.getId() + "/pago";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("errorReserva", exception.getMessage());
            cargarOpcionesFormulario(model, request);
            return "cliente/reserva-paso1";
        }
    }

    @GetMapping("/{id}/pago")
    public String pago(@PathVariable Long id, Authentication authentication, Model model) {
        ReservaEstadia reserva = reservaService.buscarReservaCliente(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reserva.getEstadoReserva()
                != pe.com.eclipsesuites.entity.EstadoReserva.PENDIENTE_PAGO) {
            return "redirect:/cliente/reservas/" + id;
        }
        model.addAttribute("reserva", reserva);
        model.addAttribute("metodosPago", METODOS_PAGO);
        model.addAttribute("pago", new PagoReservaRequestDTO());
        model.addAttribute("demoMode", demoMode);
        return "cliente/reserva-pago";
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable Long id,
                        @Valid @ModelAttribute("pago") PagoReservaRequestDTO pago,
                        BindingResult bindingResult,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorReserva",
                    "Selecciona un método de pago.");
            return "redirect:/cliente/reservas/" + id + "/pago";
        }
        try {
            if (demoMode) {
                reservaService.simularPagoReserva(
                        authentication.getName(), id, pago.getMetodoPago());
            } else {
                reservaService.registrarMetodoPagoReserva(
                        authentication.getName(), id, pago.getMetodoPago());
            }
            return "redirect:/cliente/reservas/" + id;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
            return "redirect:/cliente/reservas/" + id + "/pago";
        }
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable Long id, Authentication authentication) {
        ReservaEstadia reserva = reservaService.buscarReservaCliente(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reserva.getTokenQr() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(qrCodeService.generarPng(reserva.getTokenQr()));
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Authentication authentication, Model model) {
        ReservaEstadia reserva = reservaService.buscarReservaCliente(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("reserva", reserva);
        model.addAttribute("demoMode", demoMode);
        return "cliente/confirmacion-reserva";
    }

    @GetMapping("/{id}/extensiones/nueva")
    public String nuevaExtension(@PathVariable Long id, Authentication authentication, Model model) {
        if (!demoMode) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Las ampliaciones están habilitadas únicamente en modo demo.");
        }
        ReservaEstadia reserva = reservaService.buscarReservaCliente(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("reserva", reserva);
        model.addAttribute("modalidades", reservaService.listarModalidades());
        model.addAttribute("extensionRequest", new ExtensionReservaRequestDTO());
        model.addAttribute("demoMode", demoMode);
        return "cliente/extension-reserva";
    }

    @PostMapping("/{id}/extensiones")
    public String crearExtension(@PathVariable Long id,
                                 @Valid @ModelAttribute("extensionRequest")
                                 ExtensionReservaRequestDTO request,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorReserva",
                    "Selecciona la duración adicional.");
            return "redirect:/cliente/reservas/" + id + "/extensiones/nueva";
        }
        try {
            ExtensionEstadia extension = reservaService.crearExtension(
                    authentication.getName(), id, request.getModalidadId());
            return "redirect:/cliente/reservas/extensiones/" + extension.getId() + "/pago";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
            return "redirect:/cliente/reservas/" + id + "/extensiones/nueva";
        }
    }

    @GetMapping("/extensiones/{extensionId}/pago")
    public String pagoExtension(@PathVariable Long extensionId,
                                Authentication authentication,
                                Model model) {
        if (!demoMode) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Las ampliaciones están habilitadas únicamente en modo demo.");
        }
        ExtensionEstadia extension = reservaService
                .buscarExtensionCliente(extensionId, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("extension", extension);
        model.addAttribute("metodosPago", METODOS_PAGO);
        model.addAttribute("pago", new PagoReservaRequestDTO());
        model.addAttribute("demoMode", demoMode);
        model.addAttribute("extensionExpirada",
                extension.getEstado() == EstadoExtensionReserva.EXPIRADA);
        return "cliente/extension-pago";
    }

    @PostMapping("/extensiones/{extensionId}/pagar")
    public String pagarExtension(@PathVariable Long extensionId,
                                 @Valid @ModelAttribute("pago") PagoReservaRequestDTO pago,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorReserva",
                    "Selecciona un método de pago.");
            return "redirect:/cliente/reservas/extensiones/" + extensionId + "/pago";
        }
        try {
            ExtensionEstadia extension = reservaService.simularPagoExtension(
                    authentication.getName(), extensionId, pago.getMetodoPago());
            return "redirect:/cliente/reservas/" + extension.getReserva().getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
            return "redirect:/cliente/reservas/extensiones/" + extensionId + "/pago";
        }
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        model.addAttribute("reservas",
                reservaService.listarReservasCliente(authentication.getName()));
        model.addAttribute("demoMode", demoMode);
        return "cliente/mis-reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelarReservaCliente(id, authentication.getName());
            redirectAttributes.addFlashAttribute("mensaje",
                    "La reserva pendiente fue cancelada.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorReserva", exception.getMessage());
        }
        return "redirect:/cliente/reservas";
    }

    private ReservaRequestDTO nuevaSolicitud(Integer tipoId) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setTipoHabitacionId(tipoId);
        request.setFechaReserva(LocalDate.now().plusDays(1));
        request.setHoraInicio(LocalTime.of(18, 0));
        request.setCantidadPersonas(1);
        reservaService.listarModalidades().stream().findFirst()
                .ifPresent(modalidad -> request.setModalidadId(modalidad.getId()));
        return request;
    }

    private void cargarOpcionesFormulario(Model model, ReservaRequestDTO request) {
        TipoHabitacion tipo = reservaService.listarTiposHabitacion().stream()
                .filter(item -> item.getId().equals(request.getTipoHabitacionId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<ModalidadTurno> modalidades = reservaService.listarModalidades();
        List<pe.com.eclipsesuites.entity.Habitacion> habitaciones =
                reservaService.listarHabitacionesPorTipo(tipo.getId());

        if (request.getFechaReserva() == null) {
            request.setFechaReserva(LocalDate.now().plusDays(1));
        }
        if (request.getHoraInicio() == null) {
            request.setHoraInicio(LocalTime.of(18, 0));
        }
        if (request.getModalidadId() == null && !modalidades.isEmpty()) {
            request.setModalidadId(modalidades.get(0).getId());
        }
        Set<Integer> disponibles = Set.of();
        ModalidadTurno modalidad = modalidades.stream()
                .filter(item -> item.getId().equals(request.getModalidadId()))
                .findFirst().orElse(null);
        if (modalidad != null) {
            LocalDateTime inicio = request.getFechaReserva().atTime(request.getHoraInicio());
            int minutos = modalidad.getDuracionHoras()
                    .multiply(java.math.BigDecimal.valueOf(60)).intValueExact();
            disponibles = reservaService.listarHabitacionesDisponibles(
                    tipo.getId(), inicio, inicio.plusMinutes(minutos));
        }

        model.addAttribute("reservaRequest", request);
        model.addAttribute("tipoHabitacion", tipo);
        model.addAttribute("habitaciones", habitaciones);
        model.addAttribute("habitacionesDisponibles", disponibles);
        model.addAttribute("modalidades", modalidades);
        model.addAttribute("tarifas", reservaService.listarTarifas());
        model.addAttribute("fechaMinima", LocalDate.now());
        model.addAttribute("demoMode", demoMode);
    }

}
