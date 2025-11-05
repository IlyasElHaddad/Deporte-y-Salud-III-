import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.util.IOUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.io.*;
import java.util.List;

public class ExcelEntrenamientos {

    private static final String NOMBRE_ARCHIVO = "Entrenamientos.xlsx";
    private static final String GRAFICO_BARRAS = "grafico_barras.jpg";
    private static final String GRAFICO_PIE = "grafico_pie.jpg";
    private static final String GRAFICO_COMBINADO = "grafico_combinado.jpg";
    private static final String RUTA_LOGO = "/img/Deporte.png";

    public static void escribirEntrenamientos(List<Entrenamiento> entrenos) {
        File archivo = new File(NOMBRE_ARCHIVO);
        Workbook libro = null;
        Sheet hoja = null;

        try {
            if (archivo.exists()) {
                try (FileInputStream fis = new FileInputStream(archivo)) {
                    libro = new XSSFWorkbook(fis);
                }
                hoja = libro.getSheet("Entrenamientos");
                if (hoja == null) hoja = libro.createSheet("Entrenamientos");
            } else {
                libro = new XSSFWorkbook();
                hoja = libro.createSheet("Entrenamientos");
                Row cab = hoja.createRow(0);
                String[] headers = {"Tipo", "Intensidad", "Duración (min)", "Calorías"};
                CellStyle style = libro.createCellStyle();
                Font f = libro.createFont();
                f.setBold(true);
                f.setFontHeightInPoints((short)12);
                style.setFont(f);
                style.setAlignment(HorizontalAlignment.CENTER);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = cab.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(style);
                }
            }

            for (Entrenamiento e : entrenos) {
                int filaExistente = buscarFilaPorTipo(hoja, e.getTipo());
                int fila;
                if (filaExistente == -1) {
                    fila = hoja.getLastRowNum() + 1;
                } else {
                    fila = filaExistente;
                }
                Row r = hoja.getRow(fila);
                if (r == null) r = hoja.createRow(fila);

                r.createCell(0).setCellValue(e.getTipo());
                r.createCell(1).setCellValue(e.getIntensidad());
                r.createCell(2).setCellValue(e.getDuracion());
                r.createCell(3).setCellValue(e.getCalorias());
            }

            for (int i = 0; i < 4; i++) hoja.autoSizeColumn(i);

            List<Entrenamiento> todos = leerEntrenamientosDesdeHoja(hoja);
            generarGraficoBarrasJPG(todos, GRAFICO_BARRAS);
            generarGraficoPieJPG(todos, GRAFICO_PIE);
            generarGraficoCombinadoJPG(todos, GRAFICO_COMBINADO);

            insertarImagenSiNoExiste(libro, hoja, GRAFICO_BARRAS, hoja.getLastRowNum() + 2, 0);
            insertarImagenSiNoExiste(libro, hoja, GRAFICO_PIE, hoja.getLastRowNum() + 20, 0);
            insertarImagenSiNoExiste(libro, hoja, GRAFICO_COMBINADO, hoja.getLastRowNum() + 40, 0);

            File logo = new File(RUTA_LOGO);
            if (logo.exists()) {
                insertarImagenSiNoExiste(libro, hoja, RUTA_LOGO, 0, 5);
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                libro.write(fos);
            }
            System.out.println("Excel guardado: " + archivo.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (libro != null) {
                try { libro.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static int buscarFilaPorTipo(Sheet hoja, String tipo) {
        int last = hoja.getLastRowNum();
        for (int i = 1; i <= last; i++) {
            Row r = hoja.getRow(i);
            if (r == null) continue;
            Cell c = r.getCell(0);
            if (c != null && c.getCellType() == CellType.STRING) {
                if (tipo.equalsIgnoreCase(c.getStringCellValue())) return i;
            }
        }
        return -1;
    }

    private static java.util.List<Entrenamiento> leerEntrenamientosDesdeHoja(Sheet hoja) {
        java.util.List<Entrenamiento> salida = new java.util.ArrayList<>();
        int last = hoja.getLastRowNum();
        for (int i = 1; i <= last; i++) {
            Row r = hoja.getRow(i);
            if (r == null) continue;
            String tipo = getStringCellSafe(r.getCell(0));
            String intensidad = getStringCellSafe(r.getCell(1));
            int dur = (int) getNumericCellSafe(r.getCell(2));
            int cal = (int) getNumericCellSafe(r.getCell(3));
            if (tipo != null && !tipo.isEmpty()) {
                salida.add(new Entrenamiento(tipo, intensidad, dur, cal));
            }
        }
        return salida;
    }

    private static String getStringCellSafe(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf(c.getNumericCellValue());
        return c.toString();
    }

    private static double getNumericCellSafe(Cell c) {
        if (c == null) return 0;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        try { return Double.parseDouble(c.toString()); } catch (Exception ex) { return 0; }
    }

    private static void generarGraficoBarrasJPG(List<Entrenamiento> datos, String salidaJpg) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Entrenamiento e : datos) {
            dataset.addValue(e.getCalorias(), e.getTipo(), "");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Calorías por Entrenamiento",
                "Entrenamiento",
                "Calorías",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        try {
            ChartUtils.saveChartAsJPEG(new File(salidaJpg), chart, 800, 600);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void generarGraficoPieJPG(List<Entrenamiento> datos, String salidaJpg) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Entrenamiento e : datos) {
            double val = Math.max(0.1, e.getCalorias());
            dataset.setValue(e.getTipo(), val);
        }
        JFreeChart chart = ChartFactory.createPieChart("Distribución de calorías", dataset, true, true, false);
        try {
            ChartUtils.saveChartAsJPEG(new File(salidaJpg), chart, 600, 600);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void generarGraficoCombinadoJPG(List<Entrenamiento> datos, String salidaJpg) {
        DefaultCategoryDataset barras = new DefaultCategoryDataset();
        DefaultCategoryDataset linea = new DefaultCategoryDataset();
        for (Entrenamiento e : datos) {
            barras.addValue(e.getCalorias(), "Calorías", e.getTipo());
            linea.addValue(e.getDuracion(), "Duración", e.getTipo());
        }
        CategoryPlot plot = new CategoryPlot();
        org.jfree.chart.renderer.category.BarRenderer barRenderer = new org.jfree.chart.renderer.category.BarRenderer();
        plot.setDataset(0, barras);
        plot.setRenderer(0, barRenderer);
        plot.setDomainAxis(new org.jfree.chart.axis.CategoryAxis("Entrenamiento"));
        plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis("Calorías"));

        plot.setDataset(1, linea);
        org.jfree.chart.renderer.category.LineAndShapeRenderer lineRenderer = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
        plot.setRenderer(1, lineRenderer);
        plot.mapDatasetToRangeAxis(1, 1);
        plot.setRangeAxis(1, new org.jfree.chart.axis.NumberAxis("Duración (min)"));

        JFreeChart chart = new JFreeChart("Calorías y Duración (combinado)", new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14), plot, true);
        try {
            ChartUtils.saveChartAsJPEG(new File(salidaJpg), chart, 1000, 600);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void insertarImagenSiNoExiste(Workbook libro, Sheet hoja, String rutaImagen, int fila, int col) {
        try {
            if (tieneImagenConRuta(hoja, rutaImagen)) {
                return;
            }
            byte[] bytes;
            if (rutaImagen.startsWith("src") || rutaImagen.startsWith("img") || rutaImagen.startsWith("/")) {
                try (InputStream is = new FileInputStream(rutaImagen)) {
                    bytes = IOUtils.toByteArray(is);
                }
            } else {
                try (InputStream is = new FileInputStream(rutaImagen)) {
                    bytes = IOUtils.toByteArray(is);
                }
            }

            int tipo = Workbook.PICTURE_TYPE_JPEG;
            if (rutaImagen.toLowerCase().endsWith(".png")) tipo = Workbook.PICTURE_TYPE_PNG;

            int idx = libro.addPicture(bytes, tipo);
            CreationHelper helper = libro.getCreationHelper();
            Drawing<?> drawing = hoja.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(col);
            anchor.setRow1(fila);
            Picture pict = drawing.createPicture(anchor, idx);
            pict.resize(1.0);
        } catch (IOException ex) {
            System.out.println("No se pudo insertar imagen: " + rutaImagen);
        }
    }

    private static boolean tieneImagenConRuta(Sheet hoja, String ruta) {
        if (hoja.getDrawingPatriarch() == null) return false;
        return hoja.getDrawingPatriarch().iterator().hasNext();
    }
}
