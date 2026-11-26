package pe.com.eclipsesuites.dto.response;

public record PerfilClienteResponseDTO(
        Integer id,
        String nombre,
        String apellido,
        String nombreCompleto,
        String correoElectronico,
        String tipoDocumento,
        String numDocumento,
        String paisEmisionDoc,
        String telefono,
        Integer edad,
        String rol,
        String fechaRegistro,
        boolean activo,
        ReservaResumenDTO reservaActiva,
        int totalReservas) {
}
