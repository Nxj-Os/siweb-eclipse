package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.CategoriaProductoRepository;
import pe.com.eclipsesuites.dao.DetallePedidoRSRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.PedidoRoomServiceRepository;
import pe.com.eclipsesuites.dao.ProductoRepository;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dto.request.CrearPedidoRequestDTO;
import pe.com.eclipsesuites.dto.request.PedidoItemDTO;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.CategoriaCartaDTO;
import pe.com.eclipsesuites.dto.response.PedidoItemResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.dto.response.ProductoCartaDTO;
import pe.com.eclipsesuites.entity.CategoriaProducto;
import pe.com.eclipsesuites.entity.DetallePedidoRS;
import pe.com.eclipsesuites.entity.EstadoPedido;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.PedidoRoomService;
import pe.com.eclipsesuites.entity.Producto;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.service.IRoomServiceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementación de la carta de room service (flujo S2 del informe):
 * la carta y la comanda no exigen check-in (el QR se integrará luego);
 * el huésped elige la suite de entrega desde el selector. Comanda con
 * precio congelado y descuento atómico de stock, y estado que avanza
 * automáticamente con el tiempo (sin depender del rack).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceServiceImpl implements IRoomServiceService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    /** Sin I/O para que el código sea fácil de leer en pantalla. */
    private static final String LETRAS_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int MAX_INTENTOS_CODIGO = 10;

    private final HabitacionRepository habitacionRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRoomServiceRepository pedidoRoomServiceRepository;
    private final DetallePedidoRSRepository detallePedidoRSRepository;
    private final ReservaEstadiaRepository reservaEstadiaRepository;

    /** Minutos acumulados desde la última transición para cada avance. */
    @Value("${app.roomservice.estados.minutos-en-preparacion:3}")
    private long minutosEnPreparacion;

    @Value("${app.roomservice.estados.minutos-controlado:7}")
    private long minutosControlado;

    @Value("${app.roomservice.estados.minutos-entregado:12}")
    private long minutosEntregado;

    @Override
    @Transactional(readOnly = true)
    public CartaResponseDTO obtenerCarta(String token, String busqueda, Integer idCategoria) {
        return obtenerCarta(token, busqueda, idCategoria, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CartaResponseDTO obtenerCarta(String token, String busqueda, Integer idCategoria,
                                         String correoCliente) {
        Habitacion habitacion = resolverHabitacionPorToken(token);
        if (habitacion == null) {
            habitacion = suiteCheckeadaDelCliente(correoCliente);
        }

        List<CategoriaCartaDTO> categorias = categoriaProductoRepository
                .findAllByOrderByNombreCategoria()
                .stream()
                .map(categoria -> CategoriaCartaDTO.builder()
                        .idCategoria(categoria.getId())
                        .nombre(categoria.getNombreCategoria())
                        .build())
                .toList();

        String consulta = StringUtils.trimToEmpty(busqueda).toLowerCase(Locale.ROOT);
        List<ProductoCartaDTO> productos = productoRepository.findAllConCategoria().stream()
                .filter(producto -> idCategoria == null
                        || producto.getCategoria().getId().equals(idCategoria))
                .filter(producto -> consulta.isEmpty()
                        || StringUtils.containsIgnoreCase(producto.getNombre(), consulta)
                        || StringUtils.containsIgnoreCase(
                                StringUtils.defaultString(producto.getDescripcion()), consulta))
                .map(this::aProductoCarta)
                .toList();

        return CartaResponseDTO.builder()
                .numHabitacion(habitacion == null ? null : habitacion.getNumHabitacion())
                .suitesDisponibles(suitesOcupadas())
                .categorias(categorias)
                .productos(productos)
                .build();
    }

    @Override
    @Transactional
    public PedidoResponseDTO crearPedido(CrearPedidoRequestDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto");
        }
        Habitacion habitacion = resolverHabitacionParaPedido(request);
        if (habitacion == null) {
            throw new IllegalArgumentException(
                    "Elige la suite a la que quieres que entreguemos el pedido");
        }

        // Consolidar cantidades por producto (el cliente puede repetir ítems).
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (PedidoItemDTO item : request.getItems()) {
            if (item.getIdProducto() == null || item.getCantidad() == null || item.getCantidad() < 1) {
                throw new IllegalArgumentException("Cada producto debe indicar una cantidad válida");
            }
            cantidades.merge(item.getIdProducto(), item.getCantidad(), Integer::sum);
        }

        // Validar precios y stock antes de escribir nada.
        record LineaValidada(Producto producto, int cantidad, BigDecimal precio, BigDecimal subtotal) {
        }
        List<LineaValidada> lineas = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El producto " + entrada.getKey() + " ya no está disponible"));
            int cantidad = entrada.getValue();
            if (producto.getStockDisponible() < cantidad) {
                throw new StockInsuficienteException(producto.getNombre(),
                        producto.getStockDisponible());
            }
            BigDecimal precio = producto.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            lineas.add(new LineaValidada(producto, cantidad, precio, subtotal));
            montoTotal = montoTotal.add(subtotal);
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        PedidoRoomService pedido = pedidoRoomServiceRepository.save(PedidoRoomService.builder()
                .habitacion(habitacion)
                .codigoPedido(generarCodigoPedido())
                .estadoPedido(EstadoPedido.RECIBIDO)
                .montoTotal(montoTotal)
                .fHPedido(ahora)
                .fHUltimaTransicion(ahora)
                .build());

        List<DetallePedidoRS> detalles = new ArrayList<>();
        for (LineaValidada linea : lineas) {
            detalles.add(DetallePedidoRS.builder()
                    .idPedido(pedido.getId())
                    .idProducto(linea.producto().getId())
                    .pedido(pedido)
                    .producto(linea.producto())
                    .cantidadSolicitada(linea.cantidad())
                    .precioUnitarioCongelado(linea.precio())
                    .subtotalLinea(linea.subtotal())
                    .build());
        }
        detallePedidoRSRepository.saveAll(detalles);

        // Descuento atómico: si otra comanda se adelantó, revierte todo.
        for (LineaValidada linea : lineas) {
            int filas = productoRepository.descontarStock(
                    linea.producto().getId(), linea.cantidad());
            if (filas == 0) {
                throw new StockInsuficienteException(linea.producto().getNombre(),
                        linea.producto().getStockDisponible());
            }
        }

        log.info("Pedido {} creado para la suite {} con monto {}",
                pedido.getCodigoPedido(), habitacion.getNumHabitacion(), montoTotal);
        return construirRespuesta(pedido, lineas.stream()
                .map(linea -> PedidoItemResponseDTO.builder()
                        .nombre(linea.producto().getNombre())
                        .cantidad(linea.cantidad())
                        .subtotal(linea.subtotal())
                        .build())
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPedido(String token, String codigoPedido) {
        PedidoRoomService pedido = buscarPedido(codigoPedido);
        Habitacion habitacion = resolverHabitacionPorToken(token);
        // Si el huésped llega con token de check-in, solo puede ver
        // los pedidos de su propia suite; sin token se consulta por código.
        if (habitacion != null
                && !pedido.getHabitacion().getId().equals(habitacion.getId())) {
            throw new PedidoNoEncontradoException(codigoPedido);
        }
        return construirRespuesta(pedido, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidos() {
        return pedidoRoomServiceRepository.findAllConDetalles().stream()
                .map(pedido -> {
                    List<PedidoItemResponseDTO> items = pedido.getDetalles().stream()
                            .map(detalle -> PedidoItemResponseDTO.builder()
                                    .nombre(detalle.getProducto() == null ? null
                                            : detalle.getProducto().getNombre())
                                    .cantidad(detalle.getCantidadSolicitada())
                                    .subtotal(detalle.getSubtotalLinea())
                                    .build())
                            .toList();
                    return construirRespuesta(pedido, items);
                })
                .toList();
    }

    @Override
    @Transactional
    public PedidoResponseDTO actualizarEstadoPedido(String codigoPedido, String estado) {
        EstadoPedido objetivo = parsearEstado(estado);
        PedidoRoomService pedido = buscarPedido(codigoPedido);

        EstadoPedido actual = estadoEfectivo(pedido, OffsetDateTime.now());
        if (objetivo.esAnteriorA(actual)) {
            throw new IllegalArgumentException("No se puede retroceder el estado del pedido de "
                    + etiqueta(actual) + " a " + etiqueta(objetivo));
        }
        if (objetivo != pedido.getEstadoPedido()) {
            pedido.setEstadoPedido(objetivo);
            pedido.setFHUltimaTransicion(OffsetDateTime.now());
            pedidoRoomServiceRepository.save(pedido);
            log.info("Pedido {} avanzó a {}", pedido.getCodigoPedido(), objetivo);
        }
        return construirRespuesta(pedido, null);
    }

    /**
     * Resuelve la suite del pedido: primero por token de check-in
     * (cuando se integre el QR) y, sin check-in, por el número de
     * suite elegido en el selector de la carta.
     */
    private Habitacion resolverHabitacionParaPedido(CrearPedidoRequestDTO request) {
        Habitacion porToken = resolverHabitacionPorToken(request.getToken());
        if (porToken != null) {
            return porToken;
        }
        String numHabitacion = StringUtils.trimToEmpty(request.getNumHabitacion());
        if (!numHabitacion.isEmpty()) {
            return habitacionRepository.findByNumHabitacion(numHabitacion)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La suite " + numHabitacion + " no está disponible"));
        }
        List<String> ocupadas = suitesOcupadas();
        return ocupadas.isEmpty()
                ? null
                : habitacionRepository.findByNumHabitacion(ocupadas.get(0))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay suites disponibles para la entrega"));
    }

    /**
     * Suite por token de check-in: {@code null} cuando no hay token
     * (el QR se integrará después) o el token no corresponde a ninguna
     * suite vigente.
     */
    private Habitacion resolverHabitacionPorToken(String token) {
        String tokenLimpio = StringUtils.trimToEmpty(token);
        if (tokenLimpio.isEmpty()) {
            return null;
        }
        return habitacionRepository.findByTokenQrDinamico(tokenLimpio)
                .filter(habitacion -> habitacion.getFechaExpiracionToken() == null
                        || habitacion.getFechaExpiracionToken().isAfter(OffsetDateTime.now()))
                .orElse(null);
    }

    /**
     * Suite del huésped autenticado: la reserva que ya hizo check-in.
     * Es la que se precarga en el selector de entrega de la carta.
     */
    private Habitacion suiteCheckeadaDelCliente(String correoCliente) {
        if (StringUtils.isBlank(correoCliente)) {
            return null;
        }
        return reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(correoCliente)
                .stream()
                .filter(reserva -> reserva.getEstadoReserva() == EstadoReserva.CHECKED_IN)
                .map(ReservaEstadia::getHabitacion)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** Suites ocupadas para el selector de entrega de la carta. */
    private List<String> suitesOcupadas() {
        return habitacionRepository.findByEstadoRack("OCUPADA").stream()
                .map(Habitacion::getNumHabitacion)
                .toList();
    }

    private ProductoCartaDTO aProductoCarta(Producto producto) {
        return ProductoCartaDTO.builder()
                .idProducto(producto.getId())
                .idCategoria(producto.getCategoria().getId())
                .categoria(producto.getCategoria().getNombreCategoria())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecioUnitario())
                .stockDisponible(producto.getStockDisponible())
                .imagen(producto.getImagen())
                .build();
    }

    private PedidoRoomService buscarPedido(String codigoPedido) {
        String codigo = StringUtils.trimToEmpty(codigoPedido);
        if (codigo.isEmpty()) {
            throw new PedidoNoEncontradoException(codigoPedido);
        }
        return pedidoRoomServiceRepository.findByCodigoPedido(codigo)
                .orElseThrow(() -> new PedidoNoEncontradoException(codigo));
    }

    private EstadoPedido parsearEstado(String estado) {
        try {
            return EstadoPedido.valueOf(StringUtils.trimToEmpty(estado)
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de pedido no válido: " + estado);
        }
    }

    /**
     * Estado efectivo: el máximo entre el estado persistido y el que
     * corresponde por tiempo transcurrido desde la última transición.
     * Así el pedido avanza solo (RECIBIDO → EN_PREPARACION →
     * CONTROLADO → ENTREGADO) sin que el rack cambie estados.
     */
    EstadoPedido estadoEfectivo(PedidoRoomService pedido, OffsetDateTime ahora) {
        EstadoPedido persistido = pedido.getEstadoPedido();
        long minutos = Duration.between(pedido.getFHUltimaTransicion(), ahora).toMinutes();
        EstadoPedido porTiempo = EstadoPedido.RECIBIDO;
        if (minutos >= minutosEnPreparacion) {
            porTiempo = EstadoPedido.EN_PREPARACION;
        }
        if (minutos >= minutosControlado) {
            porTiempo = EstadoPedido.CONTROLADO;
        }
        if (minutos >= minutosEntregado) {
            porTiempo = EstadoPedido.ENTREGADO;
        }
        return porTiempo.esAnteriorA(persistido) ? persistido : porTiempo;
    }

    private PedidoResponseDTO construirRespuesta(PedidoRoomService pedido,
                                                 List<PedidoItemResponseDTO> itemsConocidos) {
        List<PedidoItemResponseDTO> items = itemsConocidos;
        if (items == null) {
            items = detallePedidoRSRepository.findByIdPedido(pedido.getId()).stream()
                    .map(detalle -> PedidoItemResponseDTO.builder()
                            .nombre(detalle.getProducto() == null ? null
                                    : detalle.getProducto().getNombre())
                            .cantidad(detalle.getCantidadSolicitada())
                            .subtotal(detalle.getSubtotalLinea())
                            .build())
                    .toList();
        }
        EstadoPedido efectivo = estadoEfectivo(pedido, OffsetDateTime.now());
        return PedidoResponseDTO.builder()
                .codigo(pedido.getCodigoPedido())
                .estado(efectivo.name())
                .estadoEtiqueta(etiqueta(efectivo))
                .montoTotal(pedido.getMontoTotal())
                .fHPedido(pedido.getFHPedido())
                .numHabitacion(pedido.getHabitacion() == null ? null
                        : pedido.getHabitacion().getNumHabitacion())
                .items(items)
                .build();
    }

    static String etiqueta(EstadoPedido estado) {
        return switch (estado) {
            case RECIBIDO -> "Recibido";
            case EN_PREPARACION -> "En preparación";
            case CONTROLADO -> "Controlado";
            case ENTREGADO -> "Entregado";
        };
    }

    private String generarCodigoPedido() {
        for (int intento = 0; intento < MAX_INTENTOS_CODIGO; intento++) {
            char letra = LETRAS_CODIGO.charAt(ALEATORIO.nextInt(LETRAS_CODIGO.length()));
            String codigo = letra + String.valueOf(1000 + ALEATORIO.nextInt(9000));
            if (!pedidoRoomServiceRepository.existsByCodigoPedido(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de pedido único");
    }
}
