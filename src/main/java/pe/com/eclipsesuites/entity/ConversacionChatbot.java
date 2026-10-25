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

@Entity
@Table(name = "conversacion_chatbot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversacionChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Long id;

    @Column(name = "id_cliente", nullable = true)
    private Long idCliente;

    @Column(name = "id_sesion_web", nullable = false, unique = true, length = 100)
    private String idSesionWeb;

    @Column(name = "canal_dispositivo", nullable = false, length = 50)
    private String canalDispositivo;

    @Column(name = "derivo_a_reserva", nullable = false)
    private Boolean derivoAReserva;
}