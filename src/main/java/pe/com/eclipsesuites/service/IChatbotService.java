package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatEstadoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatHistoricoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatMensajeResponseDTO;

public interface IChatbotService {

    CatalogoChatbotResponseDTO catalogo();

    ChatMensajeResponseDTO enviar(String idSesion, Integer idCliente, String direccionIp,
                                  String canal, String mensaje);

    ChatHistoricoResponseDTO historico(String idSesion, Integer idCliente);

    ChatEstadoResponseDTO estado(String idSesion, Integer idCliente);

    void reiniciarCola(String idSesion);
}
