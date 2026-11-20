package pe.com.eclipsesuites.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="agenda_evento", indexes=@Index(name="idx_agenda_inicio",columnList="inicio"))
public class AgendaEvento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=20) public String tipo;
 @Column(nullable=false,length=120) public String titulo;
 @Column(nullable=false,length=30) public String suite;
 @Column(nullable=false) public LocalDateTime inicio;
 @Column(nullable=false) public LocalDateTime fin;
 @Column(nullable=false,length=20) public String estado="PENDIENTE";
 @Column(length=1500) public String descripcion;
 @Column(length=80) public String responsable;
 @Column(length=80) public String creadoPor;
 public LocalDateTime creadoEn=LocalDateTime.now();
 public LocalDateTime inicioReal;
 public LocalDateTime finReal;
}
