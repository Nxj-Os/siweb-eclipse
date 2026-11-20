package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;import pe.com.eclipsesuites.dao.CategoriaProductoRepository;
import pe.com.eclipsesuites.dao.DetallePedidoRSRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.PedidoRoomServiceRepository;
import pe.com.eclipsesuites.dao.ProductoRepository;
import pe.com.eclipsesuites.dto.request.CrearPedidoRequestDTO;
import pe.com.eclipsesuites.dto.request.PedidoItemDTO;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.entity.CategoriaProducto;
import pe.com.eclipsesuites.entity.DetallePedidoRS;
import pe.com.eclipsesuites.entity.EstadoPedido;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.PedidoRoomService;
import pe.com.eclipsesuites.entity.Producto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceServiceImplTest {

    private static final String TOKEN = "demo-suite-101";

    @Mock
    private HabitacionRepository habitacionRepository;
    @Mock
    private CategoriaProductoRepository categoriaProductoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PedidoRoomServiceRepository pedidoRoomServiceRepository;
    @Mock
    private DetallePedidoRSRepository detallePedidoRSRepository;

    @org.mockito.Captor
    private ArgumentCaptor<List<DetallePedidoRS>> detallesCaptor;

    private RoomServiceServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new RoomServiceServiceImpl(habitacionRepository, categoriaProductoRepository,
                productoRepository, pedidoRoomServiceRepository, detallePedidoRSRepository);
        ReflectionTestUtils.setField(servicio, "minutosEnPreparacion", 3L);
        ReflectionTestUtils.setField(servicio, "minutosControlado", 7L);
        ReflectionTestUtils.setField(servicio, "minutosEntregado", 12L);
    }

    private Habitacion habitacionVigente() {
        return Habitacion.builder()
                .id(1)
                .numHabitacion("101")
                .tokenQrDinamico(TOKEN)
                .fechaExpiracionToken(OffsetDateTime.now().plusDays(1))
                .estadoRack("OCUPADA")
                .build();
    }

    private CategoriaProducto categoria(Integer id, String nombre) {
        return CategoriaProducto.builder().id(id).nombreCategoria(nombre).build();
    }

    private Producto producto(Integer id, CategoriaProducto cat, String nombre,
                             String precio, int stock) {
        return Producto.builder()
                .id(id)
                .categoria(cat)
                .nombre(nombre)
                .precioUnitario(new BigDecimal(precio))
                .stockDisponible(stock)
                .build();
    }

    // ---------------------------------------------------------- carta

    @Test
    void obtenerCarta_tokenInexistenteBloqueaElAcceso() {
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.empty());

        assertThrows(AccesoRoomServiceNoPermitidoException.class,
                () -> servicio.obtenerCarta(TOKEN, null, null));
    }

    @Test
    void obtenerCarta_tokenExpiradoBloqueaElAcceso() {
        Habitacion habitacion = habitacionVigente();
        habitacion.setFechaExpiracionToken(OffsetDateTime.now().minusMinutes(1));
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacion));

        assertThrows(AccesoRoomServiceNoPermitidoException.class,
                () -> servicio.obtenerCarta("  " + TOKEN + " ", null, null));
    }

    @Test
    void obtenerCarta_sinTokenBloqueaElAccesoSinConsultarRepositorios() {
        assertThrows(AccesoRoomServiceNoPermitidoException.class,
                () -> servicio.obtenerCarta(null, null, null));
        assertThrows(AccesoRoomServiceNoPermitidoException.class,
                () -> servicio.obtenerCarta("   ", null, null));
    }

    @Test
    void obtenerCarta_armaCategoriasYProductos() {
        CategoriaProducto platos = categoria(1, "Platos fuertes");
        CategoriaProducto bebidas = categoria(2, "Bebidas");
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(categoriaProductoRepository.findAllByOrderByNombreCategoria())
                .thenReturn(List.of(bebidas, platos));
        when(productoRepository.findAllConCategoria())
                .thenReturn(List.of(
                        producto(10, platos, "Hamburguesa clásica", "15.00", 25),
                        producto(11, bebidas, "Café americano", "9.00", 40)));

        CartaResponseDTO carta = servicio.obtenerCarta(TOKEN, null, null);

        assertEquals("101", carta.getNumHabitacion());
        assertEquals(2, carta.getCategorias().size());
        assertEquals(2, carta.getProductos().size());
        assertEquals("Bebidas", carta.getCategorias().get(0).getNombre());
        assertEquals(new BigDecimal("15.00"), carta.getProductos().get(0).getPrecio());
    }

    @Test
    void obtenerCarta_filtraPorBusquedaYCategoria() {
        CategoriaProducto platos = categoria(1, "Platos fuertes");
        CategoriaProducto bebidas = categoria(2, "Bebidas");
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(categoriaProductoRepository.findAllByOrderByNombreCategoria())
                .thenReturn(List.of(bebidas, platos));
        when(productoRepository.findAllConCategoria())
                .thenReturn(List.of(
                        producto(10, platos, "Hamburguesa clásica", "15.00", 25),
                        producto(11, bebidas, "Café americano", "9.00", 40)));

        CartaResponseDTO porBusqueda = servicio.obtenerCarta(TOKEN, "hamburg", null);
        assertEquals(1, porBusqueda.getProductos().size());
        assertEquals("Hamburguesa clásica", porBusqueda.getProductos().get(0).getNombre());

        CartaResponseDTO porCategoria = servicio.obtenerCarta(TOKEN, null, 2);
        assertEquals(1, porCategoria.getProductos().size());
        assertEquals("Café americano", porCategoria.getProductos().get(0).getNombre());
    }

    // ---------------------------------------------------------- pedido

    @Test
    void crearPedido_congelaPrecioDescuentaStockYGeneraCodigo() {
        CategoriaProducto platos = categoria(1, "Platos fuertes");
        Producto hamburguesa = producto(10, platos, "Hamburguesa clásica", "15.00", 25);
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(pedidoRoomServiceRepository.existsByCodigoPedido(any())).thenReturn(false);
        when(pedidoRoomServiceRepository.save(any(PedidoRoomService.class)))
                .thenAnswer(invocation -> {
                    PedidoRoomService guardado = invocation.getArgument(0);
                    guardado.setId(55);
                    return guardado;
                });
        when(productoRepository.findById(10)).thenReturn(Optional.of(hamburguesa));
        when(detallePedidoRSRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepository.descontarStock(10, 2)).thenReturn(1);

        PedidoResponseDTO respuesta = servicio.crearPedido(CrearPedidoRequestDTO.builder()
                .token(TOKEN)
                .items(List.of(PedidoItemDTO.builder().idProducto(10).cantidad(2).build()))
                .build());

        assertNotNull(respuesta.getCodigo());
        assertTrue(respuesta.getCodigo().matches("[A-Z]\\d{4}"),
                "Código con formato letra + 4 dígitos, pero fue: " + respuesta.getCodigo());
        assertEquals(new BigDecimal("30.00"), respuesta.getMontoTotal());
        assertEquals("RECIBIDO", respuesta.getEstado());
        assertEquals("Recibido", respuesta.getEstadoEtiqueta());
        assertEquals(1, respuesta.getItems().size());
        assertEquals("Hamburguesa clásica", respuesta.getItems().get(0).getNombre());
        assertEquals(2, respuesta.getItems().get(0).getCantidad());
        assertEquals(new BigDecimal("30.00"), respuesta.getItems().get(0).getSubtotal());

        verify(detallePedidoRSRepository).saveAll(detallesCaptor.capture());
        DetallePedidoRS detalle = detallesCaptor.getValue().get(0);
        assertEquals(55, detalle.getIdPedido());
        assertEquals(10, detalle.getIdProducto());
        assertEquals(2, detalle.getCantidadSolicitada());
        assertEquals(new BigDecimal("15.00"), detalle.getPrecioUnitarioCongelado());
        assertEquals(new BigDecimal("30.00"), detalle.getSubtotalLinea());
        verify(productoRepository).descontarStock(10, 2);
    }

    @Test
    void crearPedido_stockInsuficienteNoEscribeNada() {
        CategoriaProducto platos = categoria(1, "Platos fuertes");
        Producto almendras = producto(12, platos, "Almendras tostadas", "8.00", 1);
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(productoRepository.findById(12)).thenReturn(Optional.of(almendras));

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> servicio.crearPedido(CrearPedidoRequestDTO.builder()
                        .token(TOKEN)
                        .items(List.of(PedidoItemDTO.builder().idProducto(12).cantidad(5).build()))
                        .build()));

        assertEquals("Almendras tostadas", error.getNombreProducto());
        assertEquals(1, error.getStockDisponible());
        verify(pedidoRoomServiceRepository, never()).save(any());
        verify(productoRepository, never()).descontarStock(anyInt(), anyInt());
    }

    @Test
    void crearPedido_itemsVaciosRechazaLaSolicitud() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearPedido(CrearPedidoRequestDTO.builder()
                        .token(TOKEN)
                        .items(List.of())
                        .build()));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearPedido(null));
    }

    @Test
    void crearPedido_productoInexistenteRechazaLaSolicitud() {
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearPedido(CrearPedidoRequestDTO.builder()
                        .token(TOKEN)
                        .items(List.of(PedidoItemDTO.builder().idProducto(999).cantidad(1).build()))
                        .build()));
        verify(pedidoRoomServiceRepository, never()).save(any());
    }

    // ------------------------------------------------------ seguimiento

    @Test
    void obtenerPedido_codigoDesconocidoRetorna404() {
        when(habitacionRepository.findByTokenQrDinamico(TOKEN))
                .thenReturn(Optional.of(habitacionVigente()));
        when(pedidoRoomServiceRepository.findByCodigoPedido("A0000"))
                .thenReturn(Optional.empty());

        assertThrows(PedidoNoEncontradoException.class,
                () -> servicio.obtenerPedido(TOKEN, "A0000"));
    }

    @Test
    void obtenerPedido_deOtraSuiteNoRevelaElPedido() {
        Habitacion propia = habitacionVigente();
        Habitacion ajena = Habitacion.builder().id(2).numHabitacion("202").build();
        PedidoRoomService pedido = PedidoRoomService.builder()
                .id(7)
                .habitacion(ajena)
                .codigoPedido("A1488")
                .estadoPedido(EstadoPedido.RECIBIDO)
                .montoTotal(new BigDecimal("15.00"))
                .fHPedido(OffsetDateTime.now())
                .fHUltimaTransicion(OffsetDateTime.now())
                .build();
        when(habitacionRepository.findByTokenQrDinamico(TOKEN)).thenReturn(Optional.of(propia));
        when(pedidoRoomServiceRepository.findByCodigoPedido("A1488"))
                .thenReturn(Optional.of(pedido));

        assertThrows(PedidoNoEncontradoException.class,
                () -> servicio.obtenerPedido(TOKEN, "A1488"));
    }

    @Test
    void obtenerPedido_devuelveEstadoEfectivoYContenido() {
        Habitacion habitacion = habitacionVigente();
        PedidoRoomService pedido = PedidoRoomService.builder()
                .id(7)
                .habitacion(habitacion)
                .codigoPedido("A1488")
                .estadoPedido(EstadoPedido.RECIBIDO)
                .montoTotal(new BigDecimal("15.00"))
                .fHPedido(OffsetDateTime.now().minusMinutes(4))
                .fHUltimaTransicion(OffsetDateTime.now().minusMinutes(4))
                .build();
        Producto hamburguesa = producto(10, categoria(1, "Platos fuertes"),
                "Hamburguesa clásica", "15.00", 25);
        when(habitacionRepository.findByTokenQrDinamico(TOKEN)).thenReturn(Optional.of(habitacion));
        when(pedidoRoomServiceRepository.findByCodigoPedido("A1488"))
                .thenReturn(Optional.of(pedido));
        when(detallePedidoRSRepository.findByIdPedido(7)).thenReturn(List.of(
                DetallePedidoRS.builder()
                        .idPedido(7)
                        .idProducto(10)
                        .producto(hamburguesa)
                        .cantidadSolicitada(1)
                        .precioUnitarioCongelado(new BigDecimal("15.00"))
                        .subtotalLinea(new BigDecimal("15.00"))
                        .build()));

        PedidoResponseDTO respuesta = servicio.obtenerPedido(TOKEN, "A1488");

        assertEquals("A1488", respuesta.getCodigo());
        // 4 minutos desde la creación ya supera el umbral de preparación.
        assertEquals("EN_PREPARACION", respuesta.getEstado());
        assertEquals("En preparación", respuesta.getEstadoEtiqueta());
        assertEquals("101", respuesta.getNumHabitacion());
        assertEquals(1, respuesta.getItems().size());
        assertEquals("Hamburguesa clásica", respuesta.getItems().get(0).getNombre());
    }

    // ------------------------------------------ estados automáticos

    @Test
    void estadoEfectivo_avanzaSoloConElTiempoTranscurrido() {
        OffsetDateTime creacion = OffsetDateTime.now().minusMinutes(13);
        PedidoRoomService pedido = PedidoRoomService.builder()
                .estadoPedido(EstadoPedido.RECIBIDO)
                .fHPedido(creacion)
                .fHUltimaTransicion(creacion)
                .build();

        assertEquals(EstadoPedido.RECIBIDO,
                servicio.estadoEfectivo(pedido, creacion.plusMinutes(1)));
        assertEquals(EstadoPedido.EN_PREPARACION,
                servicio.estadoEfectivo(pedido, creacion.plusMinutes(4)));
        assertEquals(EstadoPedido.CONTROLADO,
                servicio.estadoEfectivo(pedido, creacion.plusMinutes(8)));
        assertEquals(EstadoPedido.ENTREGADO,
                servicio.estadoEfectivo(pedido, creacion.plusMinutes(13)));
    }

    @Test
    void estadoEfectivo_nuncaRetrocedeBajoElEstadoPersistido() {
        PedidoRoomService pedido = PedidoRoomService.builder()
                .estadoPedido(EstadoPedido.ENTREGADO)
                .fHPedido(OffsetDateTime.now())
                .fHUltimaTransicion(OffsetDateTime.now())
                .build();

        assertEquals(EstadoPedido.ENTREGADO,
                servicio.estadoEfectivo(pedido, OffsetDateTime.now().plusMinutes(1)));
    }

    @Test
    void actualizarEstado_persisteElAvanceYReiniciaElCronometro() {
        PedidoRoomService pedido = PedidoRoomService.builder()
                .id(7)
                .habitacion(habitacionVigente())
                .codigoPedido("A1488")
                .estadoPedido(EstadoPedido.RECIBIDO)
                .montoTotal(new BigDecimal("15.00"))
                .fHPedido(OffsetDateTime.now().minusMinutes(8))
                .fHUltimaTransicion(OffsetDateTime.now().minusMinutes(8))
                .build();
        when(pedidoRoomServiceRepository.findByCodigoPedido("A1488"))
                .thenReturn(Optional.of(pedido));
        when(pedidoRoomServiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detallePedidoRSRepository.findByIdPedido(7)).thenReturn(List.of());

        PedidoResponseDTO respuesta = servicio.actualizarEstadoPedido("A1488", "controlado");

        assertEquals("CONTROLADO", respuesta.getEstado());
        assertEquals(EstadoPedido.CONTROLADO, pedido.getEstadoPedido());
        // El nuevo estado queda como referencia del avance posterior.
        assertTrue(java.time.Duration.between(pedido.getFHUltimaTransicion(),
                OffsetDateTime.now()).toMinutes() < 1);
        verify(pedidoRoomServiceRepository).save(pedido);
    }

    @Test
    void actualizarEstado_rechazaRetrocesoRespectoAlEstadoPersistido() {
        PedidoRoomService pedido = PedidoRoomService.builder()
                .id(7)
                .habitacion(habitacionVigente())
                .codigoPedido("A1488")
                .estadoPedido(EstadoPedido.ENTREGADO)
                .montoTotal(new BigDecimal("15.00"))
                .fHPedido(OffsetDateTime.now())
                .fHUltimaTransicion(OffsetDateTime.now())
                .build();
        when(pedidoRoomServiceRepository.findByCodigoPedido("A1488"))
                .thenReturn(Optional.of(pedido));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoPedido("A1488", "EN_PREPARACION"));
        verify(pedidoRoomServiceRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_rechazaRetrocesoRespectoAlAvancePorTiempo() {
        PedidoRoomService pedido = PedidoRoomService.builder()
                .id(7)
                .habitacion(habitacionVigente())
                .codigoPedido("A1488")
                .estadoPedido(EstadoPedido.RECIBIDO)
                .montoTotal(new BigDecimal("15.00"))
                .fHPedido(OffsetDateTime.now().minusMinutes(30))
                .fHUltimaTransicion(OffsetDateTime.now().minusMinutes(30))
                .build();
        when(pedidoRoomServiceRepository.findByCodigoPedido("A1488"))
                .thenReturn(Optional.of(pedido));

        // Con 30 minutos el estado efectivo ya es ENTREGADO.
        assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoPedido("A1488", "CONTROLADO"));
        verify(pedidoRoomServiceRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_nombreInvalidoRechazaLaSolicitud() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoPedido("A1488", "VOLANDO"));
        verify(pedidoRoomServiceRepository, never()).findByCodigoPedido(eq("A1488"));
    }
}
