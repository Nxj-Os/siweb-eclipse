package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Clasificador por palabras clave. Funciona sin Gemini para que la intención
 * quede registrada también en el modo público.
 */
@Component
public class IntencionChatbotDetector {

    private static final List<String> DERIVAR = List.of(
            "hablar con recepcion", "hablar con recepción", "con recepcion", "con recepción",
            "quiero hablar con", "atenderme", "un humano", "persona real", "agente humano",
            "ayuda de alguien", "colaborador", "atencion al cliente", "contactar con recepcion",
            "llamar a recepcion", "soporte humano");

    private static final List<String> RESERVA = List.of(
            "reservar", "reserva mi", "quiero reservar", "hacer una reserva", "apartar",
            "booking", "reservar suite", "reservar habitacion", "reservar habitación");

    private static final List<String> TARIFA = List.of(
            "tarifa", "precio", "precios", "cuanto cuesta", "cuánto cuesta", "costo", "costos",
            "cuanto vale", "cuánto vale", "cuesta", "cotizacion", "cotización", "cuanto cobran",
            "costo por noche", "desde cuanto", "desde cuánto");

    private static final List<String> DISPONIBILIDAD = List.of(
            "disponible", "disponibilidad", "hay plazas", "sin plazas", "libre", "libres",
            "ocupada", "ocupadas", "libres para", "tienen libre", "esta libre", "está libre",
            "fechas disponibles", "cuando hay", "cuándo hay");

    private static final List<String> SERVICIOS = List.of(
            "servicio", "servicios", "amenities", "amennities", "comodidades", "incluye",
            "desayuno", "wifi", "wi-fi", "parking", "estacionamiento", "piscina", "gym",
            "room service", "roomservice", "jacuzzi", "spa", "que ofrecen", "qué ofrecen");

    private static final List<String> UBICACION = List.of(
            "donde estan", "dónde están", "ubicacion", "ubicación", "direccion", "dirección",
            "como llegar", "cómo llegar", "mapa", "telefono", "teléfono", "contacto",
            "horario", "horarios", "check-in", "checkin", "entrada y salida", "hora de salida");

    private static final List<String> POLITICAS = List.of(
            "politica", "política", "politicas", "políticas", "reglas", "cancelacion",
            "cancelación", "reembolso", "menores", "mascotas", "fumar", "acumulad",
            "requisitos", "identificacion", "identificación");

    private static final List<String> SALUDO = List.of(
            "hola", "buenas", "buen dia", "buen día", "buenas tardes", "buenas noches",
            "que tal", "qué tal", "hey", "saludos", "iniciar");

    private static final List<String> DESPEDIDA = List.of(
            "adios", "adiós", "chau", "hasta luego", "gracias", "nos vemos", "bye",
            "eso es todo", "nada mas", "nada más");

    public Intencion detectar(String mensaje) {
        String texto = normalizar(mensaje);
        if (texto.isBlank()) {
            return Intencion.OTRO;
        }
        if (contiene(texto, DERIVAR)) {
            return Intencion.DERIVAR_RECEPCION;
        }
        if (contiene(texto, RESERVA)) {
            return Intencion.RESERVA;
        }
        if (contiene(texto, TARIFA)) {
            return Intencion.TARIFA;
        }
        if (contiene(texto, DISPONIBILIDAD)) {
            return Intencion.DISPONIBILIDAD;
        }
        if (contiene(texto, SERVICIOS)) {
            return Intencion.SERVICIOS;
        }
        if (contiene(texto, UBICACION)) {
            return Intencion.UBICACION;
        }
        if (contiene(texto, POLITICAS)) {
            return Intencion.POLITICAS;
        }
        if (contiene(texto, DESPEDIDA)) {
            return Intencion.DESPEDIDA;
        }
        if (contiene(texto, SALUDO)) {
            return Intencion.SALUDO;
        }
        return Intencion.OTRO;
    }

    private boolean contiene(String texto, List<String> claves) {
        return claves.stream().anyMatch(texto::contains);
    }

    static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String minuscules = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return minuscules.replaceAll("\\s+", " ");
    }
}
