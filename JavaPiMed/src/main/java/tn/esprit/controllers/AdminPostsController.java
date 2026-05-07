package tn.esprit.controllers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Post;
import tn.esprit.entities.User;
import tn.esprit.services.PostService;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AdminPostsController {

    @FXML private BorderPane rootPane;

    @FXML private Label totalPostsLabel;
    @FXML private Label publishedPostsLabel;
    @FXML private Label pendingPostsLabel;
    @FXML private Label rejectedPostsLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;

    @FXML private TableView<Post> postsTable;
    @FXML private TableColumn<Post, String> titleCol;
    @FXML private TableColumn<Post, String> categoryCol;
    @FXML private TableColumn<Post, String> statusCol;
    @FXML private TableColumn<Post, String> dateCol;
    @FXML private TableColumn<Post, Integer> commentsCol;
    @FXML private TableColumn<Post, Post> actionsCol;

    private final PostService postService = new PostService();
    private final ObservableList<Post> postsObservableList = FXCollections.observableArrayList();
    private FilteredList<Post> filteredPosts;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadPosts();
    }

    private void setupTable() {
        titleCol.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getTitre()))
        );

        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(getCategory(data.getValue()))
        );

        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(getStatusLabel(data.getValue()))
        );



        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue()))
        );

        commentsCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getNbr_commentaires()).asObject()
        );

        actionsCol.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue())
        );

        setupStatusColumnStyle();
        setupActionsColumn();

        postsTable.setPlaceholder(new Label("Aucun post trouvé."));
    }

    private void setupStatusColumnStyle() {
        statusCol.setCellFactory(column -> new TableCell<Post, String>() {
            private final Label badge = new Label();

            {
                badge.setMinWidth(92);
                badge.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                badge.setText(status);

                if (status.equalsIgnoreCase("Publié")) {
                    badge.setStyle(
                            "-fx-background-color: #dcfce7;" +
                                    "-fx-text-fill: #15803d;" +
                                    "-fx-padding: 6 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                } else if (status.equalsIgnoreCase("En attente")) {
                    badge.setStyle(
                            "-fx-background-color: #fef3c7;" +
                                    "-fx-text-fill: #d97706;" +
                                    "-fx-padding: 6 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                } else if (status.equalsIgnoreCase("Refusé")) {
                    badge.setStyle(
                            "-fx-background-color: #fee2e2;" +
                                    "-fx-text-fill: #dc2626;" +
                                    "-fx-padding: 6 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                } else {
                    badge.setStyle(
                            "-fx-background-color: #e0f2fe;" +
                                    "-fx-text-fill: #0369a1;" +
                                    "-fx-padding: 6 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );
                }

                setGraphic(badge);
            }
        });
    }

    private void setupActionsColumn() {
        actionsCol.setCellFactory(column -> new TableCell<Post, Post>() {

            private final Button viewBtn = new Button("Voir");
            private final HBox box = new HBox(8, viewBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                viewBtn.setStyle(
                        "-fx-background-color: #dbeafe;" +
                                "-fx-text-fill: #2563eb;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 10;" +
                                "-fx-padding: 6 18;" +
                                "-fx-cursor: hand;"
                );

                viewBtn.setOnAction(event -> {
                    Post post = getTableView().getItems().get(getIndex());
                    showPostDetails(post);
                });
            }

            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);

                if (empty || post == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void setupFilters() {
        statusFilter.getItems().setAll("Tous", "Publié", "En attente", "Refusé", "Autre");
        statusFilter.setValue("Tous");

        filteredPosts = new FilteredList<>(postsObservableList, post -> true);
        postsTable.setItems(filteredPosts);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadPosts() {
        try {
            List<Post> posts = postService.recuperer();

            postsObservableList.clear();
            postsObservableList.addAll(posts);

            loadCategories(posts);
            updateStats(posts);
            applyFilters();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les posts.");
        }
    }

    private void loadCategories(List<Post> posts) {
        String selectedCategory = categoryFilter.getValue();

        Set<String> categories = new TreeSet<>();
        categories.add("Toutes");

        for (Post post : posts) {
            String category = getCategory(post);
            if (!category.isBlank()) {
                categories.add(category);
            }
        }

        categoryFilter.getItems().setAll(categories);

        if (selectedCategory != null && categories.contains(selectedCategory)) {
            categoryFilter.setValue(selectedCategory);
        } else {
            categoryFilter.setValue("Toutes");
        }
    }

    private void updateStats(List<Post> posts) {
        int total = posts.size();
        int published = 0;
        int pending = 0;
        int rejected = 0;

        for (Post post : posts) {
            String status = getStatusLabel(post);

            if (status.equalsIgnoreCase("Publié")) {
                published++;
            } else if (status.equalsIgnoreCase("En attente")) {
                pending++;
            } else if (status.equalsIgnoreCase("Refusé")) {
                rejected++;
            }
        }

        totalPostsLabel.setText(String.valueOf(total));
        publishedPostsLabel.setText(String.valueOf(published));
        pendingPostsLabel.setText(String.valueOf(pending));
        rejectedPostsLabel.setText(String.valueOf(rejected));
    }

    private void applyFilters() {
        if (filteredPosts == null) {
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilter.getValue() == null ? "Tous" : statusFilter.getValue();
        String selectedCategory = categoryFilter.getValue() == null ? "Toutes" : categoryFilter.getValue();

        filteredPosts.setPredicate(post -> {
            boolean matchesSearch =
                    search.isEmpty()
                            || safe(post.getTitre()).toLowerCase().contains(search)
                            || safe(post.getContenu()).toLowerCase().contains(search)
                            || getCategory(post).toLowerCase().contains(search)
                            || getStatusLabel(post).toLowerCase().contains(search);

            boolean matchesStatus =
                    selectedStatus.equals("Tous")
                            || getStatusLabel(post).equalsIgnoreCase(selectedStatus)
                            || (selectedStatus.equals("Autre") && isOtherStatus(post));

            boolean matchesCategory =
                    selectedCategory.equals("Toutes")
                            || getCategory(post).equalsIgnoreCase(selectedCategory);

            return matchesSearch && matchesStatus && matchesCategory;
        });
    }

    @FXML
    private void refreshData() {
        loadPosts();
    }

    @FXML
    private void exportPdf() {
        try {
            if (filteredPosts == null || filteredPosts.isEmpty()) {
                showWarning("Export PDF", "Aucun post à exporter.");
                return;
            }

            Document document = new Document(PageSize.A4.rotate(), 36, 36, 40, 40);
            PdfWriter.getInstance(document, new FileOutputStream("posts_admin_medflow.pdf"));
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(8, 145, 178));
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.GRAY);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);

            Paragraph title = new Paragraph("Liste des posts - MedFlow Admin", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            document.add(title);

            Paragraph subtitle = new Paragraph("Export généré automatiquement depuis l'espace administrateur", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 2f, 1.5f, 3.5f});

            addHeaderCell(table, "Titre", headerFont);
            addHeaderCell(table, "Catégorie", headerFont);
            addHeaderCell(table, "Statut", headerFont);
            addHeaderCell(table, "Date", headerFont);
            addHeaderCell(table, "Com.", headerFont);
            addHeaderCell(table, "Contenu", headerFont);

            for (Post post : filteredPosts) {
                addBodyCell(table, safe(post.getTitre()), cellFont);
                addBodyCell(table, getCategory(post), cellFont);
                addBodyCell(table, getStatusLabel(post), cellFont);
                addBodyCell(table, formatDate(post), cellFont);
                addBodyCell(table, String.valueOf(post.getNbr_commentaires()), cellFont);
                addBodyCell(table, shorten(safe(post.getContenu()), 120), cellFont);
            }

            document.add(table);
            document.close();

            File file = new File("posts_admin_medflow.pdf");
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur PDF", "Impossible de générer le fichier PDF.");
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new BaseColor(8, 145, 178));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(7);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }

    private void showPostDetails(Post post) {
        if (post == null) {
            return;
        }

        String message =
                "Titre : " + safe(post.getTitre()) + "\n\n" +
                        "Catégorie : " + getCategory(post) + "\n" +
                        "Statut : " + getStatusLabel(post) + "\n" +
                        "Date : " + formatDate(post) + "\n" +
                        "Commentaires : " + post.getNbr_commentaires() + "\n\n" +
                        "Contenu :\n" + safe(post.getContenu());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du post");
        alert.setHeaderText(safe(post.getTitre()));
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getStatusLabel(Post post) {
        String status = "";

        try {
            status = safe(post.getModeration_status()).trim().toLowerCase();
        } catch (Exception ignored) {
        }

        if (status.isEmpty()) {
            return "Publié";
        }

        return switch (status) {
            case "approved", "approve", "publié", "publie", "published", "visible" -> "Publié";
            case "pending", "en attente", "attente" -> "En attente";
            case "rejected", "refused", "refusé", "refuse" -> "Refusé";
            default -> "Autre";
        };
    }

    private boolean isOtherStatus(Post post) {
        String status = getStatusLabel(post);
        return !status.equalsIgnoreCase("Publié")
                && !status.equalsIgnoreCase("En attente")
                && !status.equalsIgnoreCase("Refusé");
    }

    private String getCategory(Post post) {
        if (post == null || post.getCategorie() == null || post.getCategorie().isBlank()) {
            return "Sans catégorie";
        }

        return post.getCategorie().trim();
    }


   
    private String formatDate(Post post) {
        try {
            if (post != null && post.getDate_creation() != null) {
                return post.getDate_creation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        } catch (Exception ignored) {
        }

        return "-";
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String clean = text.replace("\n", " ").replace("\r", " ").trim();

        while (clean.contains("  ")) {
            clean = clean.replace("  ", " ");
        }

        if (clean.length() <= maxLength) {
            return clean;
        }

        return clean.substring(0, maxLength).trim() + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /* ================= NAVIGATION SIDEBAR ================= */

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openPatients() {
        goTo("/FXML/PatientsAdmin.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/FXML/UtilisateursAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/FXML/DemandesRole.fxml");
    }

    @FXML
    private void openUserRoles() {
        goTo("/FXML/UserRoles.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openDetection() {
        goTo("/DetectionEpidemie.fxml");
    }

    @FXML
    private void openStockRupture() {
        goTo("/FXML/StockRupture.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/FXML/EvenementsAdmin.fxml");
    }

    @FXML
    private void openEventParticipants() {
        goTo("/FXML/ParticipantsEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        goTo("/FXML/RessourcesAdmin.fxml");
    }

    @FXML
    private void openStatsEvents() {
        goTo("/FXML/StatsEvenements.fxml");
    }

    @FXML
    private void openStatsRessources() {
        goTo("/FXML/StatsRessources.fxml");
    }

    @FXML
    private void openConsultations() {
        goTo("/FXML/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openRendezVous() {
        goTo("/FXML/RendezVousAdmin.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/FXML/StatsConsultations.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/ReclamationsAdmin.fxml");
    }



    @FXML
    private void openStatsReclamations() {
        goTo("/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/PostsAdmin.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/BlogStatAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goTo("/FXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Page introuvable : " + fxmlPath + "\n\nVérifie le nom exact du fichier FXML.");
                return;
            }

            Parent page = FXMLLoader.load(url);
            rootPane.getScene().setRoot(page);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}