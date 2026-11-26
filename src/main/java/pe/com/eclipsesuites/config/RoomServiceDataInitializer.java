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
 * categorías, productos con precio/stock (fotos de stock en Pexels,
 * guardadas como URL en la columna imagen) y una suite ocupada con
 * token de check-in de muestra para recorrer el flujo completo del
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
        CategoriaProducto consumibles = crearCategoria("Consumibles",
                "Bebidas de la habitación y hielo para tu barra");
        CategoriaProducto amenities = crearCategoria("Amenities",
                "Artículos de confort y limpieza para tu suite");

        List<Producto> productos = List.of(
                producto(entradas, "Papa a la huancaína", "Papa amarilla, ají amarillo y queso",
                        "16.00", 20, "COCINA", foto(28448378)),
                producto(entradas, "Causa limeña", "Causa fria con pollo y palta",
                        "18.00", 15, "COCINA", foto(28448382)),
                producto(entradas, "Ceviche clásico", "Corvina, limón, cebolla y camote",
                        "28.00", 10, "COCINA", foto(26586571)),
                producto(platos, "Hamburguesa clásica", "Carne, queso, lechuga y papas",
                        "15.00", 25, "COCINA", foto(5179783)),
                producto(platos, "Lomo saltado", "Lomo, cebolla, tomate y papas fritas",
                        "32.00", 12, "COCINA", foto(28503593)),
                producto(platos, "Arroz chaufa", "Arroz frito con pollo y huevo",
                        "26.00", 14, "COCINA", foto(40262815)),
                producto(platos, "Pollo a la brasa", "Quarter de pollo con papas",
                        "28.00", 8, "COCINA", foto(28635476)),
                producto(bebidas, "Chicha morada", "Maíz morado, limón y canela (750 ml)",
                        "8.00", 30, "BAR", foto(12987518)),
                producto(bebidas, "Limonada de hierbabuena", "Limonada fresca (500 ml)",
                        "7.00", 30, "BAR", foto(39536485)),
                producto(bebidas, "Café americano", "Café de origen, taza 240 ml",
                        "9.00", 40, "BAR", foto(3936163)),
                producto(bebidas, "Té de canela", "Infusión caliente, taza 240 ml",
                        "6.00", 40, "BAR", foto(36861188)),
                producto(postres, "Tres leches", "Bizcocho bañado con crema",
                        "12.00", 10, "COCINA", foto(37228442)),
                producto(postres, "Flan napolitano", "Flan con caramelo",
                        "10.00", 12, "COCINA", foto(35020131)),
                producto(postres, "Helado artesanal", "Dos bochas a elección",
                        "11.00", 16, "COCINA", foto(26068500)),
                producto(snacks, "Papas fritas", "Porción grande con sal y limón",
                        "10.00", 25, "BAR", foto(5779487)),
                producto(snacks, "Palitos de ajo", "Con salsa de la casa",
                        "9.00", 18, "BAR", foto(13062441)),
                producto(snacks, "Almendras tostadas", "Porción 120 g",
                        "8.00", 2, "BAR", foto(34533459)),
                // Consumibles de habitación
                producto(consumibles, "Agua embotellada", "Botella 625 ml (pack de 1)",
                        "5.00", 48, "ALMACEN", foto(31699476)),
                producto(consumibles, "Gaseosa lata", "Lata 350 ml a elección",
                        "6.00", 36, "ALMACEN", foto(5860659)),
                producto(consumibles, "Bolsa de hielo", "Hielo de 5 kg para tu barra",
                        "8.00", 20, "ALMACEN", foto(7254237)),
                // Amenities de housekeeping
                producto(amenities, "Toalla extra", "Toalla de baño adicional",
                        "10.00", 30, "HOUSEKEEPING", foto(45980)),
                producto(amenities, "Jabón de manos", "Barra de jabón neutro 100 g",
                        "4.00", 40, "HOUSEKEEPING", foto(6690839)),
                producto(amenities, "Shampoo", "Shampoo 40 ml para ducha",
                        "6.00", 40, "HOUSEKEEPING", foto(13516802)),
                producto(amenities, "Papel higiénico", "Rollo extra de papel higiénico",
                        "5.00", 35, "HOUSEKEEPING", foto(3958205)),
                producto(amenities, "Kit dental", "Cepillo y pasta de dientes",
                        "8.00", 25, "HOUSEKEEPING", foto(7622555)));
        productoRepository.saveAll(productos);
        log.info("Carta de room service sembrada: {} categorías y {} productos",
                7, productos.size());
    }

    private CategoriaProducto crearCategoria(String nombre, String descripcion) {
        return categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombreCategoria(nombre)
                .descripcion(descripcion)
                .build());
    }

    private Producto producto(CategoriaProducto categoria, String nombre, String descripcion,
                              String precio, int stock, String area, String imagen) {
        return Producto.builder()
                .categoria(categoria)
                .nombre(nombre)
                .descripcion(descripcion)
                .precioUnitario(new BigDecimal(precio))
                .stockDisponible(stock)
                .areaDespacho(area)
                // URL directa de stock (Pexels): la carta la renderiza como <img>.
                .imagen(imagen)
                .build();
    }

    /** URL directa de la foto de stock en Pexels (no se descargan archivos). */
    private static String foto(int idFoto) {
        return "https://images.pexels.com/photos/" + idFoto
                + "/pexels-photo-" + idFoto + ".jpeg?auto=compress&cs=tinysrgb&w=800";
    }
}
