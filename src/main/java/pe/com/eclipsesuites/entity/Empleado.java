package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "empleado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    @ToString.Exclude
    private RolEmpleado rol;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;

    @Column(name = "edad")
    private Integer edad;

    @Column(name = "username_login", nullable = false, unique = true, length = 50)
    private String usernameLogin;

    @Column(name = "password_hash_bcrypt", nullable = false, length = 100)
    private String passwordHashBcrypt;

    @Column(name = "turno_laboral", length = 10)
    private String turnoLaboral;

    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;
}
