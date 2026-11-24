package pe.com.eclipsesuites.service.impl;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.TransaccionPagoRepository;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.TransaccionPago;
import pe.com.eclipsesuites.service.IReporteExcelService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteExcelServiceImpl implements IReporteExcelService {

    /**
     * Estados de transacción que NO suman al cierre de caja. Se muestran en el detalle,
     * marcados como "No", pero no entran en los totales. Ajustar si el equipo define
     * otros valores para el campo {@code transaccion_pago.estado}.
     */
    static final Set<String> ESTADOS_NO_COBRADOS = Set.of(
            "ANULADO", "ANULADA", "RECHAZADO", "RECHAZADA",
            "FALLIDO", "FALLIDA", "PENDIENTE", "EXTORNADO", "EXTORNADA");

    /** Estado del rack que cuenta como habitación ocupada (ver diagrama S4 del documento). */
    static final String ESTADO_OCUPADA = "OCUPADA";

    private static final String SIN_DATO = "SIN DATO";
    private static final String FORMATO_MONEDA = "\"S/\" #,##0.00";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransaccionPagoRepository transaccionPagoRepository;
    private final HabitacionRepository habitacionRepository;

    // ------------------------------------------------------------------ Cierre de caja

    @Override
    @Transactional(readOnly = true)
    public byte[] generarCierreCajaDiario(LocalDate fecha) {
        Preconditions.checkArgument(fecha != null, "La fecha del cierre es obligatoria");

        List<TransaccionPago> transacciones = transaccionPagoRepository.findByFechaTransaccionEntre(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(libro);
            Sheet hoja = libro.createSheet("Cierre de caja");

            int fila = 0;
            texto(hoja.createRow(fila++), 0, "Cierre de caja diario - Eclipse Suites Hotel", estilos.titulo);
            texto(hoja.createRow(fila), 0, "Fecha del cierre:", estilos.etiqueta);
            texto(hoja.getRow(fila++), 1, fecha.format(FORMATO_FECHA), estilos.texto);
            texto(hoja.createRow(fila), 0, "Generado:", estilos.etiqueta);
            texto(hoja.getRow(fila++), 1,
                    LocalDateTime.now(ZONA_HOTEL).format(FORMATO_FECHA_HORA), estilos.texto);
            fila++;

            // Resumen por método de pago (solo transacciones cobradas)
            texto(hoja.createRow(fila++), 0, "Resumen por método de pago", estilos.subtitulo);
            encabezado(hoja.createRow(fila++), estilos, "Método de pago", "Transacciones cobradas", "Monto cobrado");

            TreeMap<String, long[]> cantidadPorMetodo = new TreeMap<>();
            TreeMap<String, BigDecimal> montoPorMetodo = new TreeMap<>();
            BigDecimal totalCobrado = BigDecimal.ZERO;
            long totalCobradas = 0;
            for (TransaccionPago t : transacciones) {
                if (!esCobrada(t)) {
                    continue;
                }
                String metodo = normalizar(t.getComprobantePago().getMetodoPago());
                cantidadPorMetodo.computeIfAbsent(metodo, k -> new long[1])[0]++;
                montoPorMetodo.merge(metodo, montoDe(t), BigDecimal::add);
                totalCobrado = totalCobrado.add(montoDe(t));
                totalCobradas++;
            }
            for (var entrada : montoPorMetodo.entrySet()) {
                Row r = hoja.createRow(fila++);
                texto(r, 0, entrada.getKey(), estilos.texto);
                entero(r, 1, cantidadPorMetodo.get(entrada.getKey())[0], estilos.entero);
                monto(r, 2, entrada.getValue(), estilos.moneda);
            }
            Row total = hoja.createRow(fila++);
            texto(total, 0, "TOTAL COBRADO", estilos.etiquetaTotal);
            entero(total, 1, totalCobradas, estilos.enteroTotal);
            monto(total, 2, totalCobrado, estilos.monedaTotal);
            fila++;

            // Detalle de todas las transacciones del día
            texto(hoja.createRow(fila++), 0, "Detalle de transacciones", estilos.subtitulo);
            encabezado(hoja.createRow(fila++), estilos, "Hora", "N° comprobante", "Método de pago",
                    "Tipo de transacción", "Referencia", "Estado", "Monto", "¿Suma al cierre?");
            if (transacciones.isEmpty()) {
                texto(hoja.createRow(fila++), 0, "Sin transacciones en la fecha indicada", estilos.texto);
            }
            for (TransaccionPago t : transacciones) {
                Row r = hoja.createRow(fila++);
                texto(r, 0, t.getFechaTransaccion().format(FORMATO_HORA), estilos.texto);
                texto(r, 1, t.getComprobantePago().getNumeroComprobante(), estilos.texto);
                texto(r, 2, normalizar(t.getComprobantePago().getMetodoPago()), estilos.texto);
                texto(r, 3, StringUtils.defaultString(t.getTipoTransaccion()), estilos.texto);
                texto(r, 4, StringUtils.defaultString(t.getReferencia()), estilos.texto);
                texto(r, 5, normalizar(t.getEstado()), estilos.texto);
                monto(r, 6, montoDe(t), estilos.moneda);
                texto(r, 7, esCobrada(t) ? "Sí" : "No", estilos.centrado);
            }

            ajustarColumnas(hoja, 8);
            log.info("Cierre de caja generado para {}: {} transacciones", fecha, transacciones.size());
            return aBytes(libro);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el cierre de caja", e);
        }
    }

    // ------------------------------------------------------------------ Ocupación de suites

    @Override
    @Transactional(readOnly = true)
    public byte[] generarOcupacionSuites() {
        List<Habitacion> habitaciones = habitacionRepository.findAllConTipo();

        TreeMap<String, Long> porEstado = habitaciones.stream().collect(Collectors.groupingBy(
                h -> normalizar(h.getEstadoRack()), TreeMap::new, Collectors.counting()));
        var porTipo = habitaciones.stream().collect(Collectors.groupingBy(
                h -> StringUtils.defaultIfBlank(h.getTipoHabitacion().getNombreTematico(), SIN_DATO),
                TreeMap::new,
                Collectors.groupingBy(h -> normalizar(h.getEstadoRack()), TreeMap::new, Collectors.counting())));

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(libro);

            Sheet resumen = libro.createSheet("Resumen");
            int fila = 0;
            texto(resumen.createRow(fila++), 0, "Ocupación de suites - Eclipse Suites Hotel", estilos.titulo);
            texto(resumen.createRow(fila), 0, "Generado:", estilos.etiqueta);
            texto(resumen.getRow(fila++), 1,
                    LocalDateTime.now(ZONA_HOTEL).format(FORMATO_FECHA_HORA), estilos.texto);
            fila++;

            // Resumen por estado del rack
            long totalHabitaciones = habitaciones.size();
            texto(resumen.createRow(fila++), 0, "Habitaciones por estado del rack", estilos.subtitulo);
            encabezado(resumen.createRow(fila++), estilos, "Estado", "Habitaciones", "% del total");
            for (var entrada : porEstado.entrySet()) {
                Row r = resumen.createRow(fila++);
                texto(r, 0, entrada.getKey(), estilos.texto);
                entero(r, 1, entrada.getValue(), estilos.entero);
                porcentaje(r, 2, proporcion(entrada.getValue(), totalHabitaciones), estilos.porcentaje);
            }
            Row total = resumen.createRow(fila++);
            texto(total, 0, "TOTAL", estilos.etiquetaTotal);
            entero(total, 1, totalHabitaciones, estilos.enteroTotal);
            porcentaje(total, 2, totalHabitaciones == 0 ? 0 : 1, estilos.porcentajeTotal);

            long ocupadas = porEstado.getOrDefault(ESTADO_OCUPADA, 0L);
            Row tasa = resumen.createRow(fila++);
            texto(tasa, 0, "Tasa de ocupación", estilos.etiquetaTotal);
            porcentaje(tasa, 1, proporcion(ocupadas, totalHabitaciones), estilos.porcentajeTotal);
            fila++;

            // Matriz tipo de habitación x estado
            texto(resumen.createRow(fila++), 0, "Habitaciones por tipo y estado", estilos.subtitulo);
            TreeSet<String> estados = new TreeSet<>(porEstado.keySet());
            Row cabecera = resumen.createRow(fila++);
            int col = 0;
            texto(cabecera, col++, "Tipo de habitación", estilos.encabezado);
            for (String estado : estados) {
                texto(cabecera, col++, estado, estilos.encabezado);
            }
            texto(cabecera, col, "Total", estilos.encabezado);
            for (var entradaTipo : porTipo.entrySet()) {
                Row r = resumen.createRow(fila++);
                int c = 0;
                texto(r, c++, entradaTipo.getKey(), estilos.texto);
                long sumaTipo = 0;
                for (String estado : estados) {
                    long cantidad = entradaTipo.getValue().getOrDefault(estado, 0L);
                    entero(r, c++, cantidad, estilos.entero);
                    sumaTipo += cantidad;
                }
                entero(r, c, sumaTipo, estilos.enteroTotal);
            }
            ajustarColumnas(resumen, estados.size() + 2);

            // Detalle por habitación
            Sheet detalle = libro.createSheet("Detalle");
            encabezado(detalle.createRow(0), estilos, "Habitación", "Piso", "Tipo", "Estado del rack");
            int f = 1;
            for (Habitacion h : habitaciones) {
                Row r = detalle.createRow(f++);
                texto(r, 0, h.getNumHabitacion(), estilos.texto);
                if (h.getPiso() != null) {
                    entero(r, 1, h.getPiso(), estilos.entero);
                }
                texto(r, 2, StringUtils.defaultIfBlank(h.getTipoHabitacion().getNombreTematico(), SIN_DATO),
                        estilos.texto);
                texto(r, 3, normalizar(h.getEstadoRack()), estilos.texto);
            }
            ajustarColumnas(detalle, 4);

            log.info("Reporte de ocupación generado: {} habitaciones", totalHabitaciones);
            return aBytes(libro);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el reporte de ocupación", e);
        }
    }

    // ------------------------------------------------------------------ Utilidades

    private static boolean esCobrada(TransaccionPago t) {
        return !ESTADOS_NO_COBRADOS.contains(normalizar(t.getEstado()));
    }

    private static BigDecimal montoDe(TransaccionPago t) {
        return t.getMonto() == null ? BigDecimal.ZERO : t.getMonto();
    }

    private static String normalizar(String valor) {
        String limpio = StringUtils.trimToEmpty(valor);
        return limpio.isEmpty() ? SIN_DATO : StringUtils.upperCase(limpio, Locale.ROOT);
    }

    private static double proporcion(long parte, long total) {
        return total == 0 ? 0d : (double) parte / total;
    }

    private static byte[] aBytes(Workbook libro) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        libro.write(salida);
        return salida.toByteArray();
    }

    private static void ajustarColumnas(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    private static void encabezado(Row fila, Estilos estilos, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            texto(fila, i, titulos[i], estilos.encabezado);
        }
    }

    private static void texto(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static void entero(Row fila, int columna, long valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static void monto(Row fila, int columna, BigDecimal valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private static void porcentaje(Row fila, int columna, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    /** Estilos del libro, creados una sola vez por libro (Excel limita la cantidad de estilos). */
    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle subtitulo;
        final CellStyle etiqueta;
        final CellStyle encabezado;
        final CellStyle texto;
        final CellStyle centrado;
        final CellStyle entero;
        final CellStyle moneda;
        final CellStyle porcentaje;
        final CellStyle etiquetaTotal;
        final CellStyle enteroTotal;
        final CellStyle monedaTotal;
        final CellStyle porcentajeTotal;

        Estilos(Workbook libro) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            Font grande = libro.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 14);

            short formatoMoneda = libro.createDataFormat().getFormat(FORMATO_MONEDA);
            short formatoPorcentaje = libro.createDataFormat().getFormat("0.0%");
            short formatoEntero = libro.createDataFormat().getFormat("#,##0");

            titulo = estilo(libro, grande, false, (short) 0, HorizontalAlignment.LEFT);
            subtitulo = estilo(libro, negrita, false, (short) 0, HorizontalAlignment.LEFT);
            etiqueta = estilo(libro, negrita, false, (short) 0, HorizontalAlignment.LEFT);
            encabezado = estilo(libro, negrita, true, (short) 0, HorizontalAlignment.CENTER);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            texto = estilo(libro, null, true, (short) 0, HorizontalAlignment.LEFT);
            centrado = estilo(libro, null, true, (short) 0, HorizontalAlignment.CENTER);
            entero = estilo(libro, null, true, formatoEntero, HorizontalAlignment.RIGHT);
            moneda = estilo(libro, null, true, formatoMoneda, HorizontalAlignment.RIGHT);
            porcentaje = estilo(libro, null, true, formatoPorcentaje, HorizontalAlignment.RIGHT);
            etiquetaTotal = estilo(libro, negrita, true, (short) 0, HorizontalAlignment.LEFT);
            enteroTotal = estilo(libro, negrita, true, formatoEntero, HorizontalAlignment.RIGHT);
            monedaTotal = estilo(libro, negrita, true, formatoMoneda, HorizontalAlignment.RIGHT);
            porcentajeTotal = estilo(libro, negrita, true, formatoPorcentaje, HorizontalAlignment.RIGHT);
        }

        private static CellStyle estilo(Workbook libro, Font fuente, boolean borde,
                                        short formato, HorizontalAlignment alineacion) {
            CellStyle estilo = libro.createCellStyle();
            if (fuente != null) {
                estilo.setFont(fuente);
            }
            if (borde) {
                estilo.setBorderTop(BorderStyle.THIN);
                estilo.setBorderBottom(BorderStyle.THIN);
                estilo.setBorderLeft(BorderStyle.THIN);
                estilo.setBorderRight(BorderStyle.THIN);
            }
            if (formato != 0) {
                estilo.setDataFormat(formato);
            }
            estilo.setAlignment(alineacion);
            return estilo;
        }
    }
}
