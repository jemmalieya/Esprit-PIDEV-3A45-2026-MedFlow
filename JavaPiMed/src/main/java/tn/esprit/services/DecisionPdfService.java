package tn.esprit.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import tn.esprit.entities.User;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DecisionPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─────────────────────────── Public API ───────────────────────────────────

    public static byte[] buildStaffApprovalPdf(User user, String adminName, String reason) {
        return buildDecisionPdf(
                "Demande d'acces staff approuvee",
                "APPROUVEE",
                "staff",
                user, reason, adminName,
                new Color(16, 185, 129),   // vert
                new Color(209, 250, 229)   // fond vert clair
        );
    }

    public static byte[] buildStaffRejectionPdf(User user, String adminName, String reason) {
        return buildDecisionPdf(
                "Demande d'acces staff refusee",
                "REFUSEE",
                "staff",
                user, reason, adminName,
                new Color(220, 38, 38),    // rouge
                new Color(254, 226, 226)   // fond rouge clair
        );
    }

    public static byte[] buildAccountBanPdf(User user, String reason, String adminName) {
        return buildDecisionPdf(
                "Suspension de compte",
                "COMPTE SUSPENDU",
                "compte",
                user, reason, adminName,
                new Color(220, 38, 38),    // rouge
                new Color(254, 226, 226)   // fond rouge clair
        );
    }

    // ─────────────────────────── Core builder ─────────────────────────────────

    private static byte[] buildDecisionPdf(
            String decision,
            String statusBadge,
            String contextType,
            User user,
            String reason,
            String adminName,
            Color accentColor,
            Color badgeBg
    ) {
        if (user == null) return new byte[0];

        String fullName  = (safe(user.getNom()) + " " + safe(user.getPrenom())).trim();
        if (fullName.isBlank()) fullName = "Utilisateur #" + user.getId();
        String email     = safe(user.getEmailUser());
        String cin       = safe(user.getCin());
        String telephone = safe(user.getTelephoneUser());
        String typeStaff = safe(user.getTypeStaff());
        String motive    = safe(reason).isBlank() ? "Non specifie" : safe(reason);
        String signer    = safe(adminName).isBlank() ? "Admin MedFlow" : safe(adminName);
        String now       = LocalDateTime.now().format(DATE_FMT);

        Color primaryBlue  = new Color(15, 45, 107);
        Color darkGray     = new Color(55, 65, 81);
        Color lightGray    = new Color(245, 247, 250);
        Color lineGray     = new Color(209, 213, 219);
        Color white        = Color.WHITE;

        Font fontTitle  = new Font(Font.HELVETICA, 13, Font.BOLD,   primaryBlue);
        Font fontLabel  = new Font(Font.HELVETICA, 9,  Font.BOLD,   primaryBlue);
        Font fontValue  = new Font(Font.HELVETICA, 9,  Font.NORMAL, darkGray);
        Font fontBadge  = new Font(Font.HELVETICA, 10, Font.BOLD,   white);
        Font fontSign   = new Font(Font.HELVETICA, 9,  Font.ITALIC, darkGray);
        Font fontFooter = new Font(Font.HELVETICA, 7,  Font.NORMAL, Color.GRAY);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── HEADER: bandeau bleu + logo ───────────────────────────────────
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 3.5f});
            headerTable.setSpacingAfter(0f);

            // Logo cell
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setBackgroundColor(primaryBlue);
            logoCell.setPadding(10f);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            try (InputStream logoStream = DecisionPdfService.class.getResourceAsStream("/images/logo.png")) {
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(64, 40);
                    logoCell.addElement(logo);
                } else {
                    // Fallback texte si logo absent
                    logoCell.addElement(new Paragraph("✦", new Font(Font.HELVETICA, 22, Font.BOLD, white)));
                }
            } catch (Exception ignored) {
                logoCell.addElement(new Paragraph("✦", new Font(Font.HELVETICA, 22, Font.BOLD, white)));
            }

            // Title cell
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setBackgroundColor(primaryBlue);
            titleCell.setPadding(12f);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph appName = new Paragraph("MEDFLOW", new Font(Font.HELVETICA, 18, Font.BOLD, white));
            appName.setSpacingAfter(2f);
            Paragraph subtitle = new Paragraph("Plateforme de Sante Numerique", new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(191, 219, 254)));
            titleCell.addElement(appName);
            titleCell.addElement(subtitle);

            headerTable.addCell(logoCell);
            headerTable.addCell(titleCell);
            doc.add(headerTable);

            // ── TITRE DU DOCUMENT ─────────────────────────────────────────────
            Paragraph docTitle = new Paragraph("FICHE DE DECISION", fontTitle);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            docTitle.setSpacingBefore(18f);
            docTitle.setSpacingAfter(4f);
            doc.add(docTitle);

            Paragraph docSubtitle = new Paragraph(decision, new Font(Font.HELVETICA, 10, Font.NORMAL, darkGray));
            docSubtitle.setAlignment(Element.ALIGN_CENTER);
            docSubtitle.setSpacingAfter(12f);
            doc.add(docSubtitle);

            // ── BADGE statut ──────────────────────────────────────────────────
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeTable.setWidthPercentage(40);
            badgeTable.setSpacingAfter(16f);
            PdfPCell badgeCell = new PdfPCell(new Phrase(statusBadge, fontBadge));
            badgeCell.setBackgroundColor(accentColor);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setPaddingTop(7f);
            badgeCell.setPaddingBottom(7f);
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeTable.addCell(badgeCell);
            doc.add(badgeTable);

            // ── BLOC INFOS UTILISATEUR ────────────────────────────────────────
            doc.add(sectionTitle("Informations utilisateur", fontLabel, primaryBlue));

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(14f);
            addInfoRow(infoTable, "Nom complet",    fullName,  fontLabel, fontValue, lightGray);
            addInfoRow(infoTable, "Email",          email.isBlank() ? "-" : email, fontLabel, fontValue, white);
            addInfoRow(infoTable, "CIN",            cin.isBlank() ? "-" : cin, fontLabel, fontValue, lightGray);
            if (!telephone.isBlank()) {
                addInfoRow(infoTable, "Telephone",  telephone, fontLabel, fontValue, white);
            }
            if ("staff".equals(contextType) && !typeStaff.isBlank()) {
                addInfoRow(infoTable, "Type de poste", typeStaff, fontLabel, fontValue, lightGray);
            }
            doc.add(infoTable);

            // ── BLOC DECISION ─────────────────────────────────────────────────
            doc.add(sectionTitle("Details de la decision", fontLabel, primaryBlue));

            PdfPTable decisionTable = new PdfPTable(2);
            decisionTable.setWidthPercentage(100);
            decisionTable.setSpacingAfter(14f);
            addInfoRow(decisionTable, "Decision",   decision,  fontLabel, fontValue, lightGray);
            addInfoRow(decisionTable, "Date",       now,       fontLabel, fontValue, white);

            // Badge motif (fond coloré)
            PdfPCell motivLabel = new PdfPCell(new Phrase("Motif / Raison", fontLabel));
            motivLabel.setBackgroundColor(lightGray);
            motivLabel.setBorder(Rectangle.BOX);
            motivLabel.setBorderColor(lineGray);
            motivLabel.setPadding(8f);

            PdfPCell motivValue = new PdfPCell(new Phrase(motive, fontValue));
            motivValue.setBackgroundColor(badgeBg);
            motivValue.setBorder(Rectangle.BOX);
            motivValue.setBorderColor(lineGray);
            motivValue.setPadding(8f);

            decisionTable.addCell(motivLabel);
            decisionTable.addCell(motivValue);
            doc.add(decisionTable);

            // ── SIGNATURE ─────────────────────────────────────────────────────
            Paragraph signatureLine = new Paragraph("_______________________________________", fontSign);
            signatureLine.setAlignment(Element.ALIGN_RIGHT);
            signatureLine.setSpacingBefore(30f);
            signatureLine.setSpacingAfter(2f);
            doc.add(signatureLine);

            Paragraph signedBy = new Paragraph("Signe par  " + signer, new Font(Font.HELVETICA, 9, Font.BOLD, primaryBlue));
            signedBy.setAlignment(Element.ALIGN_RIGHT);
            signedBy.setSpacingAfter(2f);
            doc.add(signedBy);

            Paragraph signedDate = new Paragraph("Le " + now, fontSign);
            signedDate.setAlignment(Element.ALIGN_RIGHT);
            signedDate.setSpacingAfter(20f);
            doc.add(signedDate);

            // ── FOOTER ────────────────────────────────────────────────────────
            addHorizontalRule(doc, lineGray);

            Paragraph footer = new Paragraph(
                    "Ce document est genere automatiquement par MedFlow. "
                    + "Il constitue une notification officielle. "
                    + "Toute contestation doit etre adressée a l'administrateur MedFlow.",
                    fontFooter
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(6f);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            System.err.println("[DecisionPdfService] Erreur generation PDF: " + e.getMessage());
            return new byte[0];
        }
    }

    // ─────────────────────────── Helpers ──────────────────────────────────────

    private static Paragraph sectionTitle(String text, Font font, Color lineColor) {
        Paragraph p = new Paragraph(text.toUpperCase(), font);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(4f);
        return p;
    }

    private static void addInfoRow(PdfPTable table,
                                   String label, String value,
                                   Font fontLabel, Font fontValue,
                                   Color bgColor) {
        Color lineGray = new Color(209, 213, 219);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontLabel));
        labelCell.setBackgroundColor(bgColor);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(lineGray);
        labelCell.setPadding(8f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "-" : value, fontValue));
        valueCell.setBackgroundColor(bgColor);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(lineGray);
        valueCell.setPadding(8f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static void addHorizontalRule(Document doc, Color color) throws DocumentException {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell(new Phrase(""));
        ruleCell.setBorder(Rectangle.BOTTOM);
        ruleCell.setBorderColor(color);
        ruleCell.setBorderWidth(1f);
        ruleCell.setFixedHeight(4f);
        ruleCell.setPadding(0);
        ruleCell.setBackgroundColor(Color.WHITE);
        rule.addCell(ruleCell);
        rule.setSpacingBefore(10f);
        doc.add(rule);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

