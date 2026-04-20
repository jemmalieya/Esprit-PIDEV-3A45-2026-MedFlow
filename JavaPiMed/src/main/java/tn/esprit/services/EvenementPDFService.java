package tn.esprit.services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import tn.esprit.entities.Evenement;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;

public class EvenementPDFService {

    public void genererFicheEvenement(Evenement evenement, String cheminFichier) throws Exception {
        if (evenement == null || cheminFichier == null || cheminFichier.isBlank()) {
            throw new IllegalArgumentException("Evenement ou chemin invalide.");
        }

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        Color primary = new Color(14, 104, 135);
        Color secondary = new Color(31, 155, 190);
        Color lineGray = new Color(220, 226, 233);
        Color panelBg = new Color(244, 250, 252);

        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, primary);
        Font subFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, primary);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);

        Paragraph title = new Paragraph(safe(evenement.getTitre_event()), titleFont);
        title.setSpacingAfter(8f);
        document.add(title);

        Paragraph subtitle = new Paragraph("Fiche detaillee de l'evenement selectionne", subFont);
        subtitle.setSpacingAfter(16f);
        document.add(subtitle);

        Image illustration = loadImage(evenement.getImage_couverture_event());
        if (illustration != null) {
            illustration.scaleToFit(520, 240);
            illustration.setAlignment(Element.ALIGN_CENTER);
            illustration.setSpacingAfter(18f);
            document.add(illustration);
        }

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{1.3f, 1.7f});
        metaTable.setSpacingAfter(18f);

        addSectionCell(metaTable, "Informations generales", sectionFont, primary);
        addSectionCell(metaTable, "Publication et contact", sectionFont, secondary);

        addKeyValueCell(metaTable, "Type", safe(evenement.getType_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Statut", safe(evenement.getStatut_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Date debut", dateText(evenement.getDate_debut_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Date fin", dateText(evenement.getDate_fin_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Lieu", safe(evenement.getNom_lieu_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Adresse", safe(evenement.getAdresse_event()) + " - " + safe(evenement.getVille_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Organisateur", safe(evenement.getNom_organisateur_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Email", safe(evenement.getEmail_contact_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Telephone", safe(evenement.getTel_contact_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Visibilite", safe(evenement.getVisibilite_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Participants max", String.valueOf(evenement.getNb_participants_max_event()), labelFont, valueFont, panelBg, lineGray);
        addKeyValueCell(metaTable, "Date limite", dateText(evenement.getDate_limite_inscription_event()), labelFont, valueFont, panelBg, lineGray);

        document.add(metaTable);

        addTextSection(document, "Description", safe(evenement.getDescription_event()), sectionFont, bodyFont, primary, panelBg, lineGray);
        addTextSection(document, "Objectif", safe(evenement.getObjectif_event()), sectionFont, bodyFont, secondary, panelBg, lineGray);

        document.close();
    }

    private void addSectionCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addKeyValueCell(PdfPTable table, String key, String value, Font keyFont, Font valueFont, Color bg, Color borderColor) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(key + "\n", keyFont));
        paragraph.add(new Phrase(safe(value), valueFont));

        PdfPCell cell = new PdfPCell(paragraph);
        cell.setPadding(12f);
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(borderColor);
        cell.setMinimumHeight(56f);
        table.addCell(cell);
    }

    private void addTextSection(Document document, String title, String value, Font headerFont, Font bodyFont, Color headerColor, Color bg, Color borderColor) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(14f);

        PdfPCell header = new PdfPCell(new Phrase(title, headerFont));
        header.setBackgroundColor(headerColor);
        header.setPadding(10f);
        header.setBorder(Rectangle.NO_BORDER);
        table.addCell(header);

        PdfPCell body = new PdfPCell(new Phrase(safe(value), bodyFont));
        body.setPadding(14f);
        body.setBackgroundColor(bg);
        body.setBorder(Rectangle.BOX);
        body.setBorderColor(borderColor);
        body.setMinimumHeight(90f);
        table.addCell(body);

        document.add(table);
    }

    private Image loadImage(String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                return null;
            }

            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return Image.getInstance(imagePath);
            }

            File file = new File(imagePath);
            if (file.exists()) {
                return Image.getInstance(file.getAbsolutePath());
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String dateText(java.util.Date date) {
        return date == null ? "-" : date.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
