package pe.com.eclipsesuites.service.impl;

/**
 * El token de habitación no existe o expiró: el huésped aún no tiene
 * check-in validado y el servicio queda bloqueado (Figma C-09).
 */
public class AccesoRoomServiceNoPermitidoException extends RuntimeException {

    public AccesoRoomServiceNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
