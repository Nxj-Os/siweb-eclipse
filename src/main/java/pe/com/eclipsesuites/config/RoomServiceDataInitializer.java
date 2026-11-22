package pe.com.eclipsesuites.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pe.com.eclipsesuites.dao.CategoriaProductoRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.ProductoRepository;
import pe.com.eclipsesuites.dao.TipoHabitacionRepository;
import pe.com.eclipsesuites.entity.CategoriaProducto;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.Producto;
import pe.com.eclipsesuites.entity.TipoHabitacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Semilla de la carta de room service para perfiles demo/dev:
 * categorías, productos con precio/stock y una suite con check-in
 * activo (token vigente) para recorrer el flujo completo del
 * huésped sin PostgreSQL ni reservas reales.
 */
@Slf4j
@Configuration
@Profile({"demo", "dev"})
@RequiredArgsConstructor
public class RoomServiceDataInitializer {

    /** Token fijo de la suite demo: /menu-suite?token=demo-suite-101 */
    public static final String TOKEN_DEMO = "demo-suite-101";

    private static final String NUM_HABITACION_DEMO = "101";

    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final HabitacionRepository habitacionRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;

    @Bean
    public CommandLineRunner inicializarCartaRoomService() {
        return args -> {
            asegurarSuiteDemo();
            if (categoriaProductoRepository.count() > 0) {
                return;
            }
            sembrarCarta();
        };
    }

    private void asegurarSuiteDemo() {
        if (habitacionRepository.findByNumHabitacion(NUM_HABITACION_DEMO).isPresent()) {
            return;
        }
        TipoHabitacion tipo = tipoHabitacionRepository
                .findByCodTipo("SUITE-DEMO")
                .orElseGet(() -> tipoHabitacionRepository.save(TipoHabitacion.builder()
                        .codTipo("SUITE-DEMO")
                        .nombreTematico("Suite Eclipse Demo")
                        .descripcionTematica("Suite de demostración para el módulo de room service")
                        .galeriaFotos("[]")
                        .capacidadMaxima((short) 2)
                        .build()));
        OffsetDateTime ahora = OffsetDateTime.now();
        habitacionRepository.save(Habitacion.builder()
                .tipoHabitacion(tipo)
                .numHabitacion(NUM_HABITACION_DEMO)
                .piso((short) 1)
                .estadoRack("OCUPADA")
                .tokenQrDinamico(TOKEN_DEMO)
                .fechaExpiracionToken(ahora.plusDays(30))
                .bloqueoTemporalHasta(ahora.minusDays(1))
                .build());
        log.info("Suite {} sembrada con token de room service {}", NUM_HABITACION_DEMO, TOKEN_DEMO);
    }

    private void sembrarCarta() {
        CategoriaProducto entradas = crearCategoria("Entradas",
                "Para compartir antes del plato principal");
        CategoriaProducto platos = crearCategoria("Platos fuertes",
                "Fuertes de la casa servidos en suite");
        CategoriaProducto bebidas = crearCategoria("Bebidas",
                "Bebidas frías y calientes");
        CategoriaProducto postres = crearCategoria("Postres",
                "Dulces para cerrar la comida");
        CategoriaProducto snacks = crearCategoria("Snacks",
                "Botanas rápidas a cualquier hora");

        List<Producto> productos = List.of(
                producto(entradas, "Papa a la huancaína", "Papa amarilla, ají amarillo y queso",
                        "16.00", 20, "COCINA", "🥗"),
                producto(entradas, "Causa limeña", "Causa fria con pollo y palta",
                        "18.00", 15, "COCINA", "🥗"),
                producto(entradas, "Ceviche clásico", "Corvina, limón, cebolla y camote",
                        "28.00", 10, "COCINA", "🐟"),
                producto(platos, "Hamburguesa clásica", "Carne, queso, lechuga y papas",
                        "15.00", 25, "COCINA", "🍔"),
                producto(platos, "Lomo saltado", "Lomo, cebolla, tomate y papas fritas",
                        "32.00", 12, "COCINA", "🍛"),
                producto(platos, "Arroz chaufa", "Arroz frito con pollo y huevo",
                        "26.00", 14, "COCINA", "🍛"),
                producto(platos, "Pollo a la brasa", "Quarter de pollo con papas",
                        "28.00", 8, "COCINA", "🍗"),
                producto(bebidas, "Chicha morada", "Maíz morado, limón y canela (750 ml)",
                        "8.00", 30, "BAR", "🥤"),
                producto(bebidas, "Limonada de hierbabuena", "Limonada fresca (500 ml)",
                        "7.00", 30, "BAR", "🥤"),
                producto(bebidas, "Café americano", "Café de origen, taza 240 ml",
                        "9.00", 40, "BAR", "☕"),
                producto(bebidas, "Té de canela", "Infusión caliente, taza 240 ml",
                        "6.00", 40, "BAR", "☕"),
                producto(postres, "Tres leches", "Bizcocho bañado con crema",
                        "12.00", 10, "COCINA", "🍰"),
                producto(postres, "Flan napolitano", "Flan con caramelo",
                        "10.00", 12, "COCINA", "🍮"),
                producto(postres, "Helado artesanal", "Dos bochas a elección",
                        "11.00", 16, "COCINA", "🍨"),
                producto(snacks, "Papas fritas", "Porción grande con sal y limón",
                        "10.00", 25, "BAR", "🍟"),
                producto(snacks, "Palitos de ajo", "Con salsa de la casa",
                        "9.00", 18, "BAR", "🥖"),
                producto(snacks, "Almendras tostadas", "Porción 120 g",
                        "8.00", 2, "BAR", "🥜"));
        productoRepository.saveAll(productos);
        log.info("Carta de room service sembrada: {} categorías y {} productos",
                5, productos.size());
    }

    private CategoriaProducto crearCategoria(String nombre, String descripcion) {
        return categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombreCategoria(nombre)
                .descripcion(descripcion)
                .build());
    }

    private Producto producto(CategoriaProducto categoria, String nombre, String descripcion,
                              String precio, int stock, String area, String emoji) {
        return Producto.builder()
                .categoria(categoria)
                .nombre(nombre)
                .descripcion(descripcion)
                .precioUnitario(new BigDecimal(precio))
                .stockDisponible(stock)
                .areaDespacho(area)
                // El emoji de la imagen es un respaldo visual en la carta.
                .imagen(emoji)
                .build();
    }
}
