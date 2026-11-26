package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.response.BoletaReservaDTO;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.dto.response.ReservaResumenDTO;

import java.util.List;

public interface IClientePerfilService {

    PerfilClienteResponseDTO obtenerPerfil(String correoElectronico);

    List<ReservaResumenDTO> listarReservas(String correoElectronico);

    BoletaReservaDTO obtenerBoleta(String correoElectronico, Long idReserva);
}
