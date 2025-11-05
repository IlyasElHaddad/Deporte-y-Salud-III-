import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.*;
import java.util.List;

public class InformePDF {

    private static final String SALIDA = "InformeEntrenamientos.pdf";
    private static final String RUTA_TTF = "src/main/resources/fonts/DejaVuSans.ttf";
    private static final String RUTA_LOGO = "src/main/resources/img/Deporte.png";

    public static void crearInforme(List<Entrenamiento> entrenos) {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDPageContentStream contenido = new PDPageContentStream(doc, pagina);

            PDFont font;
            File ftt = new File(RUTA_TTF);
            if (ftt.exists()) {
                font = PDType0Font.load(doc, new FileInputStream(ftt), true);
            } else {
                font = PDType1Font.HELVETICA;
            }

            contenido.beginText();
            contenido.setFont(font, 18);
            contenido.newLineAtOffset(50, pagina.getMediaBox().getHeight() - 80);
            contenido.showText("Informe de Entrenamientos");
            contenido.newLineAtOffset(0, -25);
            contenido.setFont(font, 12);
            contenido.showText("Total entrenamientos guardados: " + entrenos.size());
            contenido.endText();

            float startY = pagina.getMediaBox().getHeight() - 120;
            float marginLeft = 50;
            float rowHeight = 18;
            float tableWidth = pagina.getMediaBox().getWidth() - 2 * marginLeft;
            int cols = 4;
            float colWidth = tableWidth / cols;

            contenido.setFont(font, 11);
            float textY = startY;
            String[] headers = {"Tipo", "Intensidad", "Duración", "Calorías"};

            for (int i = 0; i < headers.length; i++) {
                float tx = marginLeft + i * colWidth + 2;
                contenido.beginText();
                contenido.newLineAtOffset(tx, textY);
                contenido.showText(headers[i]);
                contenido.endText();
            }

            textY -= rowHeight;
            for (Entrenamiento e : entrenos) {
                String[] valores = {
                    e.getTipo(),
                    e.getIntensidad(),
                    String.valueOf(e.getDuracion()),
                    String.valueOf(e.getCalorias())
                };
                for (int i = 0; i < valores.length; i++) {
                    float tx = marginLeft + i * colWidth + 2;
                    contenido.beginText();
                    contenido.newLineAtOffset(tx, textY);
                    contenido.showText(valores[i]);
                    contenido.endText();
                }
                textY -= rowHeight;
            }

            String grafico = "grafico_barras.jpg";
            File gfile = new File(grafico);
            if (gfile.exists()) {
                PDImageXObject img = PDImageXObject.createFromFile(grafico, doc);
                float imgWidth = img.getWidth();
                float imgHeight = img.getHeight();
                float scale = 0.5f;
                contenido.drawImage(img, marginLeft, textY - 320, imgWidth * scale, imgHeight * scale);
            }

            File logo = new File(RUTA_LOGO);
            if (logo.exists()) {
                PDImageXObject logoImg = PDImageXObject.createFromFile(RUTA_LOGO, doc);
                contenido.drawImage(logoImg, pagina.getMediaBox().getWidth() - 180, pagina.getMediaBox().getHeight() - 120, 120, 80);
            }

            contenido.close();
            doc.save(SALIDA);
            System.out.println("PDF creado: " + SALIDA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

