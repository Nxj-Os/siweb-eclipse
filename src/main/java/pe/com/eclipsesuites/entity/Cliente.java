package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cliente", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cliente_correo_electronico", columnNames = "correo_electronico"),
    @UniqueConstraint(name = "uk_cliente_num_documento", columnNames = "num_documento"),
    @UniqueConstraint(name = "uk_cliente_telefono", columnNames = "telefono")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "num_documento", nullable = false, length = 20)
    private String numDocumento;

    @Column(name = "pais_emision_doc", nullable = false, length = 3)
    private String paisEmisionDoc;

    @Column(name = "correo_electronico", nullable = false, length = 100)
    private String correoElectronico;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "edad", nullable = false)
    private Integer edad;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Builder.Default
    @Column(name = "acepto_politica_privacidad", nullable = false, columnDefinition = "boolean default false")
    private Boolean aceptoPoliticaPrivacidad = false;

    @Column(name = "fecha_aceptacion_privacidad")
    private Instant fechaAceptacionPrivacidad;

}