package tn.esprit.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.User;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommandePDFService {

    public void genererFactureCommande(Commande commande, String cheminFichier) throws Exception {
        if (commande == null || cheminFichier == null || cheminFichier.isBlank()) {
            throw new IllegalArgumentException("Commande ou chemin fichier invalide.");
        }

        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        // ===== Couleurs =====
        Color primary = new Color(19, 74, 122);
        Color softBlue = new Color(237, 245, 255);
        Color lightGray = new Color(245, 247, 250);
        Color lineGray = new Color(220, 226, 233);
        Color green = new Color(22, 163, 74);
        Color orange = new Color(245, 158, 11);
        Color red = new Color(220, 38, 38);
        Color blue = new Color(37, 99, 235);

        // ===== Fonts =====
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, primary);
        Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.GRAY);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, primary);
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD, primary);
        Font totalFont = new Font(Font.HELVETICA, 16, Font.BOLD, green);
        Font whiteBold = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

        // ===== HEADER =====
        Paragraph title = new Paragraph("FACTURE", titleFont);
        title.setSpacingAfter(6f);
        document.add(title);

        Paragraph cmdRef = new Paragraph("Commande #" + commande.getId_commande(), subtitleFont);
        cmdRef.setSpacingAfter(12f);
        document.add(cmdRef);

        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell(new Phrase(""));
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderColor(primary);
        lineCell.setBorderWidth(2f);
        lineCell.setFixedHeight(8f);
        lineCell.setPadding(0);
        lineCell.setBackgroundColor(Color.WHITE);
        lineTable.addCell(lineCell);
        lineTable.setSpacingAfter(18f);
        document.add(lineTable);

        // ===== BLOC INFOS =====
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.2f, 1f});
        infoTable.setSpacingAfter(16f);

        PdfPCell leftInfo = new PdfPCell();
        leftInfo.setBorder(Rectangle.NO_BORDER);
        leftInfo.setBackgroundColor(lightGray);
        leftInfo.setPadding(16f);

        leftInfo.addElement(new Paragraph(
                "Date : " + (
                        commande.getDate_creation_commande() != null
                                ? commande.getDate_creation_commande().format(formatter)
                                : "-"
                ),
                boldFont
        ));

        Paragraph statutP = new Paragraph(
                "Statut : " + normalizeStatut(commande.getStatut_commande()),
                boldFont
        );
        statutP.setSpacingBefore(8f);
        leftInfo.addElement(statutP);

        User user = commande.getUser();
        String clientNom = "Client";
        String clientEmail = "-";

        if (user != null) {
            String prenom = user.getPrenom() != null ? user.getPrenom() : "";
            String nom = user.getNom() != null ? user.getNom() : "";
            String full = (prenom + " " + nom).trim();

            if (!full.isEmpty()) {
                clientNom = full;
            }

            if (user.getEmailUser() != null && !user.getEmailUser().isBlank()) {
                clientEmail = user.getEmailUser();
            }
        }

        PdfPCell rightInfo = new PdfPCell();
        rightInfo.setBorder(Rectangle.NO_BORDER);
        rightInfo.setBackgroundColor(softBlue);
        rightInfo.setPadding(16f);

        rightInfo.addElement(new Paragraph("Client", sectionFont));
        rightInfo.addElement(new Paragraph(clientNom, boldFont));
        rightInfo.addElement(new Paragraph(clientEmail, normalFont));

        infoTable.addCell(leftInfo);
        infoTable.addCell(rightInfo);
        document.add(infoTable);

        // ===== TABLE PRODUITS =====
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.5f, 1.6f, 1.2f, 1.8f});
        table.setSpacingBefore(6f);
        table.setSpacingAfter(14f);

        addHeaderCell(table, "PRODUIT", primary);
        addHeaderCell(table, "PRIX U.", primary);
        addHeaderCell(table, "QTÉ", primary);
        addHeaderCell(table, "SOUS-TOTAL", primary);

        double total = 0.0;

        List<CommandeProduit> lignes = commande.getCommande_produits();
        if (lignes != null) {
            for (CommandeProduit cp : lignes) {
                String nomProduit = cp.getProduit() != null
                        ? safe(cp.getProduit().getNom_produit())
                        : "Produit";

                double prix = cp.getProduit() != null
                        ? cp.getProduit().getPrix_produit()
                        : 0.0;

                int qte = cp.getQuantite_commandee();
                double sousTotal = prix * qte;
                total += sousTotal;

                addBodyCell(table, nomProduit, normalFont, Element.ALIGN_LEFT);
                addBodyCell(table, String.format("%.2f Dt", prix), normalFont, Element.ALIGN_RIGHT);
                addBodyCell(table, String.valueOf(qte), normalFont, Element.ALIGN_CENTER);
                addBodyCell(table, String.format("%.2f Dt", sousTotal), boldFont, Element.ALIGN_RIGHT);
            }
        }

        document.add(table);

        // ===== TOTAL =====
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setWidths(new float[]{4f, 1.5f});

        PdfPCell totalLabelCell = new PdfPCell(
                new Phrase("TOTAL", new Font(Font.HELVETICA, 14, Font.BOLD, primary))
        );
        totalLabelCell.setBorder(Rectangle.TOP);
        totalLabelCell.setBorderColor(primary);
        totalLabelCell.setBorderWidth(2f);
        totalLabelCell.setPaddingTop(12f);
        totalLabelCell.setPaddingBottom(10f);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setBackgroundColor(Color.WHITE);

        PdfPCell totalValueCell = new PdfPCell(
                new Phrase(String.format("%.2f Dt", total), totalFont)
        );
        totalValueCell.setBorder(Rectangle.TOP);
        totalValueCell.setBorderColor(primary);
        totalValueCell.setBorderWidth(2f);
        totalValueCell.setPaddingTop(12f);
        totalValueCell.setPaddingBottom(10f);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(Color.WHITE);

        totalTable.addCell(totalLabelCell);
        totalTable.addCell(totalValueCell);
        totalTable.setSpacingAfter(28f);
        document.add(totalTable);

        // ===== BADGE STATUT =====
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        badgeTable.setWidthPercentage(25);

        Color badgeColor = blue;
        String s = commande.getStatut_commande() == null ? "" : commande.getStatut_commande().toLowerCase();

        if (s.contains("final")) {
            badgeColor = green;
        } else if (s.contains("attente")) {
            badgeColor = orange;
        } else if (s.contains("annul")) {
            badgeColor = red;
        } else if (s.contains("confirm")) {
            badgeColor = blue;
        } else if (s.contains("cours")) {
            badgeColor = blue;
        } else if (s.contains("livraison")) {
            badgeColor = blue;
        }

        PdfPCell badgeCell = new PdfPCell(
                new Phrase(normalizeStatut(commande.getStatut_commande()), whiteBold)
        );
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        badgeCell.setBackgroundColor(badgeColor);
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setPadding(8f);
        badgeTable.addCell(badgeCell);

        document.add(badgeTable);

        // ===== FOOTER =====
        Paragraph footerSpace = new Paragraph(" ");
        footerSpace.setSpacingBefore(22f);
        document.add(footerSpace);

        PdfPTable footerLine = new PdfPTable(1);
        footerLine.setWidthPercentage(100);
        PdfPCell footerLineCell = new PdfPCell(new Phrase(""));
        footerLineCell.setBorder(Rectangle.BOTTOM);
        footerLineCell.setBorderColor(lineGray);
        footerLineCell.setBorderWidth(1f);
        footerLineCell.setFixedHeight(8f);
        footerLineCell.setPadding(0);
        footerLineCell.setBackgroundColor(Color.WHITE);
        footerLine.addCell(footerLineCell);
        footerLine.setSpacingAfter(18f);
        document.add(footerLine);

        Paragraph thanks = new Paragraph("Merci pour votre confiance", subtitleFont);
        thanks.setAlignment(Element.ALIGN_CENTER);
        document.add(thanks);

        document.close();
    }

    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank()) return "En attente";

        String s = statut.toLowerCase();

        if (s.contains("confirm")) return "Confirmée";
        if (s.contains("cours")) return "En cours";
        if (s.contains("attente")) return "En attente";
        if (s.contains("livraison")) return "Livraison";
        if (s.contains("final")) return "Finalisée";
        if (s.contains("annul")) return "Annulée";

        return statut;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void addHeaderCell(PdfPTable table, String text, Color bg) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(230, 235, 240));
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }
}