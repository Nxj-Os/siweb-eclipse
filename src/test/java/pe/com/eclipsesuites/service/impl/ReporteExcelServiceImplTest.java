package pe.com.eclipsesuites.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.TransaccionPagoRepository;
import pe.com.eclipsesuites.entity.ComprobantePago;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.entity.TransaccionPago;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteExcelServiceImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 16);

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private HabitacionRepository habitacionRepository;

    @InjectMocks
    private ReporteExcelServiceImpl servicio;

    // ---------------------------------------------------------------- cierre de caja

    @Test
    void cierreDeCaja_sumaSoloTransaccionesCobradasYAgrupaPorMetodo() throws IOException {
        when(transaccionPagoRepository.findByFechaTransaccionEntre(any(), any())).thenReturn(List.of(
                transaccion("B001-1", "Efectivo", "APROBADO", "100.00", 9),
                transaccion("B001-2", "Yape", "APROBADO", "50.50", 10),
                transaccion("B001-3", "Yape", "ANULADO", "30.00", 11)));

        try (XSSFWorkbook libro = abrir(servicio.generarCierreCajaDiario(DIA))) {
            Sheet hoja = libro.getSheet("Cierre de caja");
            assertNotNull(hoja);

            Row yape = filaConEtiqueta(hoja, "YAPE");
            assertEquals(1, yape.getCell(1).getNumericCellValue(), 0.001);
            assertEquals(50.50, yape.getCell(2).getNumericCellValue(), 0.001);

            Row efectivo = filaConEtiqueta(hoja, "EFECTIVO");
            assertEquals(100.00, efectivo.getCell(2).getNumericCellValue(), 0.001);

            Row total = filaConEtiqueta(hoja, "TOTAL COBRADO");
            assertEquals(2, total.getCell(1).getNumericCellValue(), 0.001);
            assertEquals(150.50, total.getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void cierreDeCaja_consultaElDiaCompleto() {
        when(transaccionPagoRepository.findByFechaTransaccionEntre(
                DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay())).thenReturn(List.of());

        byte[] contenido = servicio.generarCierreCajaDiario(DIA);

        assertNotNull(contenido);
    }

    @Test
    void cierreDeCaja_sinTransaccionesGeneraLibroConTotalCero() throws IOException {
        when(transaccionPagoRepository.findByFechaTransaccionEntre(any(), any())).thenReturn(List.of());

        try (XSSFWorkbook libro = abrir(servicio.generarCierreCajaDiario(DIA))) {
            Row total = filaConEtiqueta(libro.getSheet("Cierre de caja"), "TOTAL COBRADO");
            assertEquals(0, total.getCell(1).getNumericCellValue(), 0.001);
            assertEquals(0, total.getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void cierreDeCaja_rechazaFechaNula() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarCierreCajaDiario(null));
        verifyNoInteractions(transaccionPagoRepository);
    }

    // ---------------------------------------------------------------- ocupación

    @Test
    void ocupacion_calculaTasaYDetallePorHabitacion() throws IOException {
        TipoHabitacion panoramica = TipoHabitacion.builder().nombreTematico("Suite Panorámica").build();
        TipoHabitacion presidencial = TipoHabitacion.builder().nombreTematico("Suite Presidencial").build();
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of(
                habitacion("101", 1, panoramica, "OCUPADA"),
                habitacion("102", 1, panoramica, "DISPONIBLE"),
                habitacion("201", 2, presidencial, "ocupada")));

        try (XSSFWorkbook libro = abrir(servicio.generarOcupacionSuites())) {
            Sheet resumen = libro.getSheet("Resumen");
            Row total = filaConEtiqueta(resumen, "TOTAL");
            assertEquals(3, total.getCell(1).getNumericCellValue(), 0.001);

            Row tasa = filaConEtiqueta(resumen, "Tasa de ocupación");
            assertEquals(2.0 / 3.0, tasa.getCell(1).getNumericCellValue(), 0.001);

            Row ocupadas = filaConEtiqueta(resumen, "OCUPADA");
            assertEquals(2, ocupadas.getCell(1).getNumericCellValue(), 0.001);

            Row tipo = filaConEtiqueta(resumen, "Suite Panorámica");
            assertEquals(2, tipo.getCell(tipo.getLastCellNum() - 1).getNumericCellValue(), 0.001);

            Sheet detalle = libro.getSheet("Detalle");
            assertEquals(3, detalle.getLastRowNum());
            assertEquals("101", detalle.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void ocupacion_sinHabitacionesNoDivideEntreCero() throws IOException {
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of());

        try (XSSFWorkbook libro = abrir(servicio.generarOcupacionSuites())) {
            Row tasa = filaConEtiqueta(libro.getSheet("Resumen"), "Tasa de ocupación");
            assertEquals(0, tasa.getCell(1).getNumericCellValue(), 0.001);
        }
    }

    // ---------------------------------------------------------------- utilidades

    private static TransaccionPago transaccion(String numero, String metodo, String estado,
                                               String monto, int hora) {
        ComprobantePago comprobante = ComprobantePago.builder()
                .numeroComprobante(numero)
                .metodoPago(metodo)
                .build();
        return TransaccionPago.builder()
                .comprobantePago(comprobante)
                .tipoTransaccion("PAGO")
                .monto(new BigDecimal(monto))
                .estado(estado)
                .fechaTransaccion(LocalDateTime.of(DIA, java.time.LocalTime.of(hora, 0)))
                .build();
    }

    private static Habitacion habitacion(String numero, int piso, TipoHabitacion tipo, String estado) {
        return Habitacion.builder()
                .numHabitacion(numero)
                .piso((short) piso)
                .tipoHabitacion(tipo)
                .estadoRack(estado)
                .build();
    }

    private static XSSFWorkbook abrir(byte[] contenido) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(contenido));
    }

    /** Busca la primera fila cuya primera celda de texto coincide con la etiqueta. */
    private static Row filaConEtiqueta(Sheet hoja, String etiqueta) {
        for (Row fila : hoja) {
            if (fila.getCell(0) != null
                    && fila.getCell(0).getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                    && etiqueta.equals(fila.getCell(0).getStringCellValue())) {
                return fila;
            }
        }
        throw new AssertionError("No se encontró la fila: " + etiqueta);
    }
}
