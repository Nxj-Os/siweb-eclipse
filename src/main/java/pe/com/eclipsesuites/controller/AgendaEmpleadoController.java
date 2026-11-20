package pe.com.eclipsesuites.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.com.eclipsesuites.dao.*;
import pe.com.eclipsesuites.entity.*;
import java.time.*;
import java.util.*;

@Controller
public class AgendaEmpleadoController {
 private final AgendaEventoRepository eventos;
 private final AgendaNotificacionRepository avisos;
 private final EmpleadoRepository empleados;
 public AgendaEmpleadoController(AgendaEventoRepository eventos,AgendaNotificacionRepository avisos,EmpleadoRepository empleados){this.eventos=eventos;this.avisos=avisos;this.empleados=empleados;}
 @GetMapping("/empleado/agenda") public String agenda(){return "empleado/agenda";}
 @GetMapping("/empleado/perfil") public String perfil(Authentication auth,Model model){
   Empleado e=empleados.findByUsernameLogin(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
   model.addAttribute("empleado",e);return "empleado/perfil";
 }
 @GetMapping("/api/empleado/agenda") @ResponseBody public List<AgendaEvento> listar(){return eventos.findAllByOrderByInicioAsc();}
 @GetMapping("/api/empleado/agenda/{id}") @ResponseBody public AgendaEvento detalle(@PathVariable Long id){return buscar(id);}
 public record Solicitud(@NotBlank String tipo,@NotBlank String titulo,@NotBlank String suite,@NotNull LocalDateTime inicio,@NotNull LocalDateTime fin,String descripcion,String responsable){}
 @PostMapping("/api/empleado/agenda") @ResponseBody public AgendaEvento crear(@Valid @RequestBody Solicitud s,Authentication auth){
   if(!s.tipo().equals("RESERVA")&&!s.tipo().equals("ACTIVIDAD"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo inválido");
   if(!s.fin().isAfter(s.inicio()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fecha final debe ser posterior al inicio");
   if(s.titulo().length()>120||s.suite().length()>30||(s.descripcion()!=null&&s.descripcion().length()>1500)||(s.responsable()!=null&&s.responsable().length()>80))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Texto demasiado largo");
   if(s.tipo().equals("RESERVA")&&eventos.findAll().stream().anyMatch(e->e.tipo.equals("RESERVA")&&!List.of("CANCELADA","FINALIZADA").contains(e.estado)&&e.suite.equalsIgnoreCase(s.suite().trim())&&s.inicio().isBefore(e.fin)&&s.fin().isAfter(e.inicio)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Reserva superpuesta para esta suite");
   AgendaEvento e=new AgendaEvento();e.tipo=s.tipo();e.titulo=s.titulo().trim();e.suite=s.suite().trim();e.inicio=s.inicio();e.fin=s.fin();e.descripcion=s.descripcion();e.responsable=s.responsable();e.creadoPor=auth.getName();e=eventos.save(e);
   notificar("Nueva "+(e.tipo.equals("RESERVA")?"reserva":"actividad"),e.titulo+" · Suite "+e.suite,e.id,e.responsable);
   return e;
 }
 public record EstadoRequest(@NotBlank String estado){}
 @PatchMapping("/api/empleado/agenda/{id}/estado") @ResponseBody public AgendaEvento estado(@PathVariable Long id,@RequestBody EstadoRequest req){
   AgendaEvento e=buscar(id);String next=req.estado().toUpperCase(Locale.ROOT);
   Map<String,List<String>> allowed=Map.of("PENDIENTE",List.of("EN_CURSO","CANCELADA"),"EN_CURSO",List.of("FINALIZADA","CANCELADA"));
   if(!allowed.getOrDefault(e.estado,List.of()).contains(next))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Transición no permitida");
   e.estado=next;if(next.equals("EN_CURSO"))e.inicioReal=LocalDateTime.now();if(next.equals("FINALIZADA"))e.finReal=LocalDateTime.now();e=eventos.save(e);
   notificar("Estado actualizado",e.titulo+" · "+next,e.id,e.responsable);return e;
 }
 @GetMapping("/api/empleado/notificaciones") @ResponseBody public List<AgendaNotificacion> notificaciones(Authentication a){return avisos.findTop100ByDestinatarioInOrderByFechaDesc(List.of("TODOS",a.getName()));}
 @PostMapping("/api/empleado/notificaciones/{id}/leer") @ResponseBody public AgendaNotificacion leer(@PathVariable Long id,Authentication a){
   AgendaNotificacion n=avisos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
   if(!n.destinatario.equals("TODOS")&&!n.destinatario.equals(a.getName()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   // Para avisos globales, crear un acuse individual sin ocultarlo a otros usuarios.
   if(n.destinatario.equals("TODOS")){AgendaNotificacion ack=new AgendaNotificacion();ack.titulo="Leído: "+n.titulo;ack.detalle=n.detalle;ack.eventoId=n.eventoId;ack.destinatario=a.getName();ack.leida=true;return avisos.save(ack);}
   n.leida=true;return avisos.save(n);
 }
 private AgendaEvento buscar(Long id){return eventos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro inexistente"));}
 private void notificar(String titulo,String detalle,Long evento,String responsable){
   AgendaNotificacion n=new AgendaNotificacion();n.titulo=titulo;n.detalle=detalle;n.eventoId=evento;n.destinatario="TODOS";avisos.save(n);
   if(responsable!=null&&!responsable.isBlank()&&!responsable.equals("TODOS")){AgendaNotificacion personal=new AgendaNotificacion();personal.titulo=titulo;personal.detalle=detalle;personal.eventoId=evento;personal.destinatario=responsable;avisos.save(personal);}
 }
}
