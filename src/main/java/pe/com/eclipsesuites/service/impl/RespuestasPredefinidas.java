package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO;
import pe.com.eclipsesuites.dto.response.HabitacionCardDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Respuestas escritas en el servidor con datos reales de la base. Es el motor
 * del modo público, donde no se invoca Gemini.
 */
@Service
public class RespuestasPredefinidas {

    static final String MENSAJE_SALUDO = """
            Hola, soy el asistente de Eclipse Suites.
            Puedo ayudarte con tarifas, disponibilidad, servicios y políticas de estadía.
            ¿Qué te gustaría saber?""";

    static final String CTA_REGISTRO = """
            Para consultas abiertas necesitas una cuenta de cliente.
            Crea tu cuenta en /registro y podrás hablar con el asistente sin límites.
            Mientras tanto, elige una de las opciones de abajo.""";

    static final String DERIVACION = """
            Te derivamos con recepción.
            Un colaborador te responderá en breve.""";

    static final String SIN_TARIFAS = "Aún no tenemos tarifas publicadas. Te derivamos con "
            + "recepción para que te cotice.";

    static final String SIN_DISPONIBILIDAD = "Hoy no tenemos suites libres. ¿Quieres que te "
            + "derivemos con recepción para buscar fechas alternativas?";

    static final String FALLBACK_CLIENTE = "No pude preparar una respuesta para esa consulta. "
            + "Puedes reformularla o pedir que te derive con recepción.";

    private static final List<String> SERVICIOS = List.of(
            "Recepción y seguridad 24 horas",
            "Desayuno buffet incluido en tarifas seleccionadas",
            "Wi-Fi sin costo en todas las suites",
            "Estacionamiento vigilado",
            "Room service durante el día",
            "Limpieza diaria de suite",
            "Jacuzzi en suites temáticas premium");

    private static final List<String> POLITICAS = List.of(
            "Check-in desde las 14:00 y check-out hasta las 12:00.",
            "Se solicita documento de identidad al llegar.",
            "Menores de edad deben estar acompañados por un adulto.",
            "No se permite fumar dentro de las suites.",
            "Mascotas consultadas con recepción antes de reservar.",
            "Cancelaciones: consulta las condiciones con recepción para tu fecha.");

    private static final List<String> UBICACION = List.of(
            "Recepción atiende las 24 horas.",
            "Check-in desde las 14:00 y check-out hasta las 12:00.",
            "Para direcciones, traslados o datos de contacto exactos, te derivamos con recepción.");

    private final ChatContextoBuilder contextoBuilder;

    public RespuestasPredefinidas(ChatContextoBuilder contextoBuilder) {
        this.contextoBuilder = contextoBuilder;
    }

    @Transactional(readOnly = true)
    public String responder(Intencion intencion) {
        return switch (intencion) {
            case SALUDO -> MENSAJE_SALUDO;
            case TARIFA -> textoTarifas();
            case DISPONIBILIDAD -> textoDisponibilidad();
            case SERVICIOS -> "Estos son nuestros servicios:\n" + enumerar(SERVICIOS);
            case UBICACION -> "Sobre tu estadía:\n" + enumerar(UBICACION);
            case POLITICAS -> "Políticas de estadía:\n" + enumerar(POLITICAS);
            case RESERVA -> "Puedo preparar tu solicitud, pero la reserva la confirma "
                    + "recepción. Te derivamos con recepción y un colaborador te contactará "
                    + "con las fechas y la suite que buscas.";
            case DERIVAR_RECEPCION -> DERIVACION;
            case DESPEDIDA -> "¡Que tengas una buena estancia! Cuando quieras sigo aquí "
                    + "para ayudarte con tarifas o disponibilidad.";
            case OTRO -> CTA_REGISTRO;
        };
    }

    @Transactional(readOnly = true)
    public String textoTarifas() {
        Map<String, BigDecimal> tarifas = contextoBuilder.tarifasPorTipo();
        if (tarifas.isEmpty()) {
            return SIN_TARIFAS;
        }
        String listado = tarifas.entrySet().stream()
                .map(entrada -> "- " + entrada.getKey() + ": desde S/. "
                        + entrada.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .collect(Collectors.joining("\n"));
        return "Tarifas de referencia por suite:\n" + listado
                + "\n\nEl precio final depende de la modalidad y las fechas. "
                + "¿Te derivo con recepción para confirmar?";
    }

    @Transactional(readOnly = true)
    public String textoDisponibilidad() {
        CatalogoChatbotResponseDTO catalogo = contextoBuilder.catalogo();
        List<HabitacionCardDTO> libres = catalogo.habitaciones().stream()
                .filter(HabitacionCardDTO::disponible)
                .toList();
        if (libres.isEmpty()) {
            return SIN_DISPONIBILIDAD;
        }
        String listado = libres.stream()
                .map(card -> "- " + card.numHabitacion() + " (" + card.nombreTematico()
                        + ", piso " + card.piso() + ")")
                .collect(Collectors.joining("\n"));
        return "Tenemos " + libres.size() + " de " + catalogo.total()
                + " suites disponibles ahora mismo:\n" + listado
                + "\n\n¿Te derivo con recepción para apartar alguna?";
    }

    /**
     * Respuesta de respaldo para un cliente cuando Gemini no está disponible.
     * Evita mostrarle el mensaje de registro, que solo aplica al modo público.
     */
    @Transactional(readOnly = true)
    public String responderParaCliente(Intencion intencion) {
        if (intencion == Intencion.OTRO) {
            return FALLBACK_CLIENTE;
        }
        return responder(intencion);
    }

    static List<String> sugerenciasDe(Intencion intencion) {
        return switch (intencion) {
            case SALUDO -> List.of("¿Cuánto cuesta una suite?",
                    "¿Qué suites están disponibles?", "Quiero hablar con recepción");
            case TARIFA, DISPONIBILIDAD -> List.of("¿Qué suites están disponibles?",
                    "¿Qué servicios incluyen?", "Quiero hablar con recepción");
            case SERVICIOS, UBICACION, POLITICAS -> List.of("¿Cuánto cuesta una suite?",
                    "¿Qué suites están disponibles?", "Quiero hablar con recepción");
            default -> ChatContextoBuilder.sugerenciasIniciales();
        };
    }

    private static String enumerar(List<String> elementos) {
        return elementos.stream()
                .map(elemento -> "- " + elemento)
                .collect(Collectors.joining("\n"));
    }
}
