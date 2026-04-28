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
import tn.esprit.entities.User;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

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

    public void genererTicketParticipation(
            Evenement evenement,
            ParticipationDemande demande,
            User user,
            String cheminFichier
    ) throws Exception {
        if (evenement == null || demande == null || cheminFichier == null || cheminFichier.isBlank()) {
            throw new IllegalArgumentException("Donnees ticket invalides.");
        }
        if (!ParticipationDemande.STATUS_ACCEPTED.equals(demande.getStatus())) {
            throw new IllegalArgumentException("Le ticket PDF est disponible uniquement pour une participation acceptee.");
        }

        Document document = new Document(PageSize.A4, 36, 36, 34, 34);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        Color primary = new Color(0, 151, 178);
        Color secondary = new Color(15, 163, 191);
        Color dark = new Color(21, 57, 70);
        Color soft = new Color(237, 249, 253);
        Color line = new Color(190, 229, 240);

        Font brandFont = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, dark);
        Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, primary);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, dark);
        Font ticketFont = new Font(Font.HELVETICA, 15, Font.BOLD, primary);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.4f, 2.6f});
        header.setSpacingAfter(18f);

        PdfPCell brand = new PdfPCell(new Phrase("MEDFLOW", brandFont));
        brand.setBackgroundColor(primary);
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setPadding(12f);
        brand.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.addCell(brand);

        PdfPCell access = new PdfPCell(new Phrase("Ticket d'acces evenement - Participation acceptee", brandFont));
        access.setBackgroundColor(secondary);
        access.setBorder(Rectangle.NO_BORDER);
        access.setPadding(12f);
        access.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.addCell(access);
        document.add(header);

        Paragraph title = new Paragraph(safe(evenement.getTitre_event()), titleFont);
        title.setSpacingAfter(6f);
        document.add(title);

        Paragraph subtitle = new Paragraph("Presentez ce PDF a l'entree. Le QR code contient les informations de verification.", subtitleFont);
        subtitle.setSpacingAfter(16f);
        document.add(subtitle);

        PdfPTable main = new PdfPTable(2);
        main.setWidthPercentage(100);
        main.setWidths(new float[]{1.7f, 1.0f});
        main.setSpacingAfter(16f);

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[]{1.0f, 1.7f});
        addPlainCell(info, "Participant", labelFont, soft, line);
        addPlainCell(info, participantName(demande, user), valueFont, soft, line);
        addPlainCell(info, "Email", labelFont, soft, line);
        addPlainCell(info, valueOrFallback(demande.getEmail(), user == null ? "" : user.getEmailUser()), valueFont, soft, line);
        addPlainCell(info, "Telephone", labelFont, soft, line);
        addPlainCell(info, valueOrFallback(demande.getTelephone(), user == null ? "" : user.getTelephoneUser()), valueFont, soft, line);
        addPlainCell(info, "Evenement", labelFont, soft, line);
        addPlainCell(info, safe(evenement.getTitre_event()), valueFont, soft, line);
        addPlainCell(info, "Lieu", labelFont, soft, line);
        addPlainCell(info, safe(evenement.getNom_lieu_event()) + " - " + safe(evenement.getVille_event()), valueFont, soft, line);
        addPlainCell(info, "Dates", labelFont, soft, line);
        addPlainCell(info, dateText(evenement.getDate_debut_event()) + " -> " + dateText(evenement.getDate_fin_event()), valueFont, soft, line);
        addPlainCell(info, "Code ticket", labelFont, soft, line);
        addPlainCell(info, safe(demande.getTicketCode()), ticketFont, soft, line);

        PdfPCell infoCell = new PdfPCell(info);
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPadding(0);
        main.addCell(infoCell);

        Image qr = Image.getInstance(createQrPng(buildTicketQrText(evenement, demande, user)));
        qr.scaleToFit(190, 190);
        qr.setAlignment(Element.ALIGN_CENTER);
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBackgroundColor(Color.WHITE);
        qrCell.setBorder(Rectangle.BOX);
        qrCell.setBorderColor(line);
        qrCell.setPadding(18f);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(qr);
        Paragraph qrLabel = new Paragraph("Scan verification", labelFont);
        qrLabel.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(qrLabel);
        main.addCell(qrCell);

        document.add(main);

        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell footerCell = new PdfPCell(new Phrase(
                "Acces autorise uniquement pour ce participant. Genere le "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                subtitleFont
        ));
        footerCell.setBackgroundColor(soft);
        footerCell.setBorder(Rectangle.BOX);
        footerCell.setBorderColor(line);
        footerCell.setPadding(12f);
        footer.addCell(footerCell);
        document.add(footer);

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

    private void addPlainCell(PdfPTable table, String value, Font font, Color bg, Color borderColor) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(value), font));
        cell.setPadding(10f);
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(borderColor);
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

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? safe(fallback) : value;
    }

    private String participantName(ParticipationDemande demande, User user) {
        String name = demande.getDisplayName();
        if (!"Participant".equals(name)) {
            return name;
        }
        if (user == null) {
            return name;
        }
        String fullName = (nullToEmpty(user.getPrenom()) + " " + nullToEmpty(user.getNom())).trim();
        return fullName.isBlank() ? name : fullName;
    }

    private String buildTicketQrText(Evenement evenement, ParticipationDemande demande, User user) {
        return "MEDFLOW ACCESS\n"
                + "Status: ACCEPTED\n"
                + "Ticket: " + safe(demande.getTicketCode()) + "\n"
                + "Event ID: " + evenement.getId() + "\n"
                + "Event: " + safe(evenement.getTitre_event()) + "\n"
                + "Participant ID: " + (user == null ? demande.getUserId() : user.getId()) + "\n"
                + "Participant: " + participantName(demande, user) + "\n"
                + "Demande ID: " + safe(demande.getId()) + "\n"
                + "Access: entree autorisee";
    }

    private byte[] createQrPng(String text) throws Exception {
        com.itextpdf.text.pdf.BarcodeQRCode qr =
                new com.itextpdf.text.pdf.BarcodeQRCode(text, 260, 260, null);
        java.awt.Image awtImage = qr.createAwtImage(java.awt.Color.BLACK, java.awt.Color.WHITE);
        BufferedImage image = new BufferedImage(260, 260, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(awtImage, 0, 0, 260, 260, null);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
