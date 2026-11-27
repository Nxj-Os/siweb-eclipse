package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.CategoriaCartaDTO;
import pe.com.eclipsesuites.dto.response.PedidoItemResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.dto.response.ProductoCartaDTO;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.IRoomServiceService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomServiceController.class)
@Import(SecurityConfig.class)
class RoomServiceControllerTest {

    private static final String TOKEN = "demo-suite-101";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRoomServiceService roomServiceService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    private CartaResponseDTO carta() {
        return CartaResponseDTO.builder()
                .numHabitacion("101")
                .categorias(List.of(CategoriaCartaDTO.builder()
                        .idCategoria(1).nombre("Platos fuertes").build()))
                .productos(List.of(ProductoCartaDTO.builder()
                        .idProducto(10)
                        .idCategoria(1)
                        .categoria("Platos fuertes")
                        .nombre("Hamburguesa clásica")
                        .precio(new BigDecimal("15.00"))
                        .stockDisponible(25)
                        .build()))
                .build();
    }

    private PedidoResponseDTO pedido() {
        return PedidoResponseDTO.builder()
                .codigo("A1488")
                .estado("RECIBIDO")
                .estadoEtiqueta("Recibido")
                .montoTotal(new BigDecimal("15.00"))
                .numHabitacion("101")
                .items(List.of(PedidoItemResponseDTO.builder()
                        .nombre("Hamburguesa clásica")
                        .cantidad(1)
                        .subtotal(new BigDecimal("15.00"))
                        .build()))
                .build();
    }

    @Test
    void menu_sinTokenRetornaLaCarta() throws Exception {
        when(roomServiceService.obtenerCarta(null, null, null, null)).thenReturn(carta());

        mockMvc.perform(get("/api/roomservice/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].nombre").value("Platos fuertes"));
    }

    @Test
    void menu_conTokenRetornaLaCarta() throws Exception {
        when(roomServiceService.obtenerCarta(TOKEN, null, null, null)).thenReturn(carta());

        mockMvc.perform(get("/api/roomservice/menu").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numHabitacion").value("101"))
                .andExpect(jsonPath("$.categorias[0].nombre").value("Platos fuertes"))
                .andExpect(jsonPath("$.productos[0].nombre").value("Hamburguesa clásica"))
                .andExpect(jsonPath("$.productos[0].precio").value(15.00))
                .andExpect(jsonPath("$.productos[0].stockDisponible").value(25));
    }

    @Test
    void crearPedido_conCsrfRetorna201() throws Exception {
        when(roomServiceService.crearPedido(any())).thenReturn(pedido());

        mockMvc.perform(post("/api/roomservice/pedidos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"demo-suite-101",
                                 "items":[{"idProducto":10,"cantidad":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("A1488"))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.montoTotal").value(15.00));
    }

    @Test
    void crearPedido_sinCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/api/roomservice/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"demo-suite-101",
                                 "items":[{"idProducto":10,"cantidad":1}]}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roomServiceService);
    }

    @Test
    void crearPedido_itemsVaciosRetorna400() throws Exception {
        mockMvc.perform(post("/api/roomservice/pedidos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"demo-suite-101\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.items").exists());

        verifyNoInteractions(roomServiceService);
    }

    @Test
    void consultarPedido_conTokenRetornaElEstado() throws Exception {
        when(roomServiceService.obtenerPedido(TOKEN, "A1488")).thenReturn(pedido());

        mockMvc.perform(get("/api/roomservice/pedidos/A1488").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A1488"))
                .andExpect(jsonPath("$.estadoEtiqueta").value("Recibido"))
                .andExpect(jsonPath("$.items[0].nombre").value("Hamburguesa clásica"));
    }

    @Test
    void crearPedido_sinTokenYConSuiteDeEntregaRetorna201() throws Exception {
        when(roomServiceService.crearPedido(any())).thenReturn(pedido());

        mockMvc.perform(post("/api/roomservice/pedidos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numHabitacion":"101",
                                 "items":[{"idProducto":10,"cantidad":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("A1488"));
    }

    @Test
    void consultarPedido_sinTokenRetornaElEstado() throws Exception {
        when(roomServiceService.obtenerPedido(null, "A1488")).thenReturn(pedido());

        mockMvc.perform(get("/api/roomservice/pedidos/A1488"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A1488"));
    }

    @Test
    void listarPedidos_sinSesionRetorna401() throws Exception {
        mockMvc.perform(get("/api/roomservice/pedidos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomServiceService);
    }

    @Test
    void listarPedidos_empleadoRoomserviceRetorna200() throws Exception {
        when(roomServiceService.listarPedidos()).thenReturn(List.of(pedido()));

        mockMvc.perform(get("/api/roomservice/pedidos")
                        .with(user("luis").roles("ROOMSERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("A1488"))
                .andExpect(jsonPath("$[0].numHabitacion").value("101"));

        verify(roomServiceService).listarPedidos();
    }

    @Test
    void actualizarEstado_sinSesionRetorna401() throws Exception {
        mockMvc.perform(patch("/api/roomservice/pedidos/A1488/estado").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomServiceService);
    }

    @Test
    void actualizarEstado_empleadoRoomservicePermitido() throws Exception {
        when(roomServiceService.actualizarEstadoPedido("A1488", "ENTREGADO"))
                .thenReturn(pedido());

        mockMvc.perform(patch("/api/roomservice/pedidos/A1488/estado")
                        .with(user("luis").roles("ROOMSERVICE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A1488"));

        verify(roomServiceService).actualizarEstadoPedido("A1488", "ENTREGADO");
    }

    @Test
    void actualizarEstado_clienteNoTienePermiso() throws Exception {
        mockMvc.perform(patch("/api/roomservice/pedidos/A1488/estado")
                        .with(user("ana@example.com").roles("CLIENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roomServiceService);
    }
}
