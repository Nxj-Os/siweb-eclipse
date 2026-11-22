package pe.com.eclipsesuites.dto.response;

public record HabitacionCardDTO(
        String numHabitacion,
        Short piso,
        String codTipo,
        String nombreTematico,
        String descripcionTematica,
        Short capacidadMaxima,
        String galeriaFotos,
        String estado,
        boolean disponible,
        String badge) {
}
