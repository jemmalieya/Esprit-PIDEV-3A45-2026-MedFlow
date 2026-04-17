package tn.esprit.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import tn.esprit.entities.Evenement;

import java.awt.Color;
import java.io.FileOutputStream;
import java.util.List;

public class EvenementPDFService {

    public void genererRapportEvenements(List<Evenement> evenements, String cheminFichier) throws Exception {
        if (evenements == null || cheminFichier == null || cheminFichier.isBlank()) {
            throw new IllegalArgumentException("Liste événements ou chemin invalide.");
        }

        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        Color primary = new Color(19, 74, 122);
        Color lightGray = new Color(245, 247, 250);
        Color lineGray = new Color(220, 226, 233);

        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, primary);
        Font subFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

        Paragraph title = new Paragraph("RAPPORT DES ÉVÉNEMENTS", titleFont);
        title.setSpacingAfter(8f);
        document.add(title);

        Paragraph subtitle = new Paragraph("Liste complète des événements", subFont);
        subtitle.setSpacingAfter(16f);
        document.add(subtitle);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.2f, 1.8f, 1.8f, 1.8f, 2.2f, 3.8f});

        addHeaderCell(table, "TITRE", headerFont, primary);
        addHeaderCell(table, "TYPE", headerFont, primary);
        addHeaderCell(table, "VILLE", headerFont, primary);
        addHeaderCell(table, "STATUT", headerFont, primary);
        addHeaderCell(table, "ORGANISATEUR", headerFont, primary);
        addHeaderCell(table, "DESCRIPTION", headerFont, primary);

        for (Evenement e : evenements) {
            addBodyCell(table, safe(e.getTitre_event()), cellFont, lineGray);
            addBodyCell(table, safe(e.getType_event()), cellFont, lineGray);
            addBodyCell(table, safe(e.getVille_event()), cellFont, lineGray);
            addBodyCell(table, safe(e.getStatut_event()), cellFont, lineGray);
            addBodyCell(table, safe(e.getNom_organisateur_event()), cellFont, lineGray);
            addBodyCell(table, safe(e.getDescription_event()), cellFont, lineGray);
        }

        document.add(table);

        Paragraph footer = new Paragraph("\nTotal événements : " + evenements.size(), subFont);
        footer.setSpacingBefore(16f);
        document.add(footer);

        document.close();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, Color borderColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(borderColor);
        table.addCell(cell);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}