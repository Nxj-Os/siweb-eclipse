package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_chatbot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @Column(name = "id_conversacion", nullable = false)
    private Long idConversacion;

    @Column(name = "rol_emisor", nullable = false, length = 20)
    private String rolEmisor;

    @Column(name = "contenido_mensaje", nullable = false, columnDefinition = "TEXT")
    private String contenidoMensaje;

    @Column(name = "intencion_detectada", nullable = true, length = 100)
    private String intencionDetectada;

    @Column(name = "f_h_mensaje", nullable = false)
    private LocalDateTime fhMensaje;
}