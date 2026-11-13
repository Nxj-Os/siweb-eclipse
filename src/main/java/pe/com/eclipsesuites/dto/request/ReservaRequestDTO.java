package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaRequestDTO {

    @NotNull(message = "Selecciona una suite.")
    private Integer tipoHabitacionId;

    @NotNull(message = "Selecciona una habitación.")
    private Integer habitacionId;

    @NotNull(message = "Selecciona un turno.")
    private Integer modalidadId;

    @NotNull(message = "Selecciona una fecha.")
    @FutureOrPresent(message = "La fecha de reserva no puede estar en el pasado.")
    private LocalDate fechaReserva;

    @NotNull(message = "Selecciona la hora de ingreso.")
    private LocalTime horaInicio;

    @NotNull(message = "Indica cuántas personas asistirán.")
    @Min(value = 1, message = "La reserva debe incluir al menos una persona.")
    private Integer cantidadPersonas;
}