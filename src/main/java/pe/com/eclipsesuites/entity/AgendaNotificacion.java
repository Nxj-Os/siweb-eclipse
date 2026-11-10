package pe.com.eclipsesuites.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="agenda_notificacion")
public class AgendaNotificacion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=160) public String titulo;
 @Column(length=500) public String detalle;
 public Long eventoId;
 public LocalDateTime fecha=LocalDateTime.now();
 @Column(nullable=false,length=80) public String destinatario;
 public boolean leida=false;
}
