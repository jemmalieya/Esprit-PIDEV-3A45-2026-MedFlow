package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;

import java.util.List;
import tn.esprit.tools.SessionManager;

import tn.esprit.tools.SystemNotification;
import tn.esprit.entities.User;

public class ReclamationListController {

    @FXML private TableView<Reclamation> tableReclamations;

    @FXML private TableColumn<Reclamation, String> colContenu;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, String> colPriorite;
    @FXML private TableColumn<Reclamation, Void> colActions;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ReponseService reponseService = new ReponseService();

    @FXML
    public void initialize() {

        colContenu.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("contenu"));
        colDescription.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut_reclamation"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colActions.setMinWidth(220);
        tableReclamations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 🔥 BADGE STATUT
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setText(null);
                    setGraphic(null); // IMPORTANT
                } else {
                    Label label = new Label(statut);
                    label.getStyleClass().add("badge");

                    if (statut.toLowerCase().contains("attente")) {
                        label.getStyleClass().add("badge-orange");
                    } else {
                        label.getStyleClass().add("badge-green");
                    }

                    setText(null);
                    setGraphic(label);
                }
            }
        });

        // 🔥 BADGE PRIORITE
        colPriorite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String prio, boolean empty) {
                super.updateItem(prio, empty);

                if (empty || prio == null) {
                    setText(null);
                    setGraphic(null); // IMPORTANT
                } else {
                    Label label = new Label(prio);
                    label.getStyleClass().add("badge");

                    if (prio.equalsIgnoreCase("Élevée")) {
                        label.getStyleClass().add("badge-red");
                    } else if (prio.equalsIgnoreCase("Moyenne")) {
                        label.getStyleClass().add("badge-orange");
                    } else {
                        label.getStyleClass().add("badge-green");
                    }

                    setText(null);
                    setGraphic(label);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button("🗑 Supprimer");
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button viewBtn = new Button("👁 voir reponses");

            {
                deleteBtn.getStyleClass().add("btn-delete");
                editBtn.getStyleClass().add("btn-edit");

// 🔥 SUPPRIMER
                deleteBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());

                    if (reponseService.hasAdminResponseReadByPatient(r.getId_reclamation())) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Action refusée");
                        alert.setHeaderText(null);
                        alert.setContentText("Cette réclamation est verrouillée car une réponse a déjà été lue.");
                        alert.showAndWait();
                        return;
                    }

                    reclamationService.supprimer(r);
                    loadTable();
                });

// 🔥 MODIFIER
                editBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());

                    if (reponseService.hasAdminResponseReadByPatient(r.getId_reclamation())) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Action refusée");
                        alert.setHeaderText(null);
                        alert.setContentText("Cette réclamation est verrouillée car une réponse a déjà été lue.");
                        alert.showAndWait();
                        return;
                    }

                    openEditPage(r);
                });

                viewBtn.getStyleClass().add("btn-view");

// 🔥 VOIR RÉPONSE
                viewBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    showReclamationDetails(r);
                });

            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    boolean isLocked = reponseService.hasAdminResponseReadByPatient(r.getId_reclamation());

                    editBtn.setDisable(isLocked);
                    deleteBtn.setDisable(isLocked);

                    HBox box = new HBox(10, editBtn, deleteBtn, viewBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        loadTable();
        Platform.runLater(this::checkAndShowSystemNotifications);
    }
    private void showReclamationDetails(Reclamation r) {
        try {
            reponseService.markAdminMessagesAsReadByPatient(r.getId_reclamation());

            List<ReponseReclamation> reponses =
                    reponseService.getByReclamationId(r.getId_reclamation());

            VBox container = new VBox(10);
            container.setStyle("-fx-padding: 15;");

            if (reponses.isEmpty()) {
                Label empty = new Label("Aucune réponse trouvée.");
                empty.setStyle("-fx-text-fill: gray; -fx-font-size: 14;");
                container.getChildren().add(empty);
            } else {
                for (ReponseReclamation rep : reponses) {
                    VBox card = new VBox(5);
                    card.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-padding: 10;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-border-color: #e2e8f0;"
                    );

                    Label auteur = new Label("👤 " + (rep.getAuteur() != null ? rep.getAuteur() : "Inconnu"));
                    Label msg = new Label("📩 " + rep.getMessage());
                    Label type = new Label("📌 Type : " + rep.getType_reponse());
                    Label date = new Label("📅 " + rep.getDate_creation_rep());
                    Label lu = new Label(rep.isLu_par_patient() ? "✔ Lu" : "❌ Non lu");

                    msg.setStyle("-fx-font-weight: bold;");
                    lu.setStyle(rep.isLu_par_patient()
                            ? "-fx-text-fill: green;"
                            : "-fx-text-fill: red;");

                    card.getChildren().addAll(auteur, msg, type, date, lu);
                    container.getChildren().add(card);
                }
            }

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(300);

            TextArea replyArea = new TextArea();
            replyArea.setPromptText("Écrire votre réponse...");
            replyArea.setPrefRowCount(3);

            Button sendBtn = new Button("Envoyer");
            sendBtn.getStyleClass().add("btn-view");

            VBox content = new VBox(10, scroll, replyArea, sendBtn);
            content.setStyle("-fx-padding: 10;");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Réponses");
            alert.setHeaderText("Détails des réponses");
            alert.getDialogPane().setContent(content);
            alert.getDialogPane().setPrefWidth(550);
            alert.getDialogPane().setPrefHeight(450);
            alert.getDialogPane().setStyle("-fx-background-color: #f8fafc;");

            sendBtn.setOnAction(ev -> {
                String msg = replyArea.getText() == null ? "" : replyArea.getText().trim();

                if (msg.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    a.setTitle("Champ vide");
                    a.setHeaderText(null);
                    a.setContentText("Veuillez écrire une réponse.");
                    a.showAndWait();
                    return;
                }

                try {
                    User currentUser = SessionManager.getCurrentUser();
                    if (currentUser == null) {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Erreur");
                        a.setHeaderText(null);
                        a.setContentText("Aucun utilisateur connecté.");
                        a.showAndWait();
                        return;
                    }

                    String nomAuteur = currentUser.getNom(); // <-- remplace par ton vrai getter

                    ReponseReclamation rep = new ReponseReclamation();
                    rep.setReclamation(r);
                    rep.setMessage(msg);
                    rep.setType_reponse("Réponse patient");
                    rep.setDate_creation_rep(java.time.LocalDateTime.now());
                    rep.setDate_modification_rep(java.time.LocalDateTime.now());
                    rep.setAuteur(nomAuteur);
                    rep.setRole_emetteur("PATIENT");
                    rep.setLu_par_admin(false);
                    rep.setLu_par_patient(true);

                    reponseService.ajouter(rep);

                    reclamationService.updateNotificationEnvoyee(r.getId_reclamation(), false);

                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Succès");
                    ok.setHeaderText(null);
                    ok.setContentText("Votre réponse a été envoyée.");
                    ok.showAndWait();

                    alert.close();
                    showReclamationDetails(r);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            alert.showAndWait();
            loadTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openEditPage(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FrontFXML/Réclamation.fxml")
            );

            Parent root = loader.load();

            // 🔥 récupérer controller
            ReclamationController controller = loader.getController();

            // 🔥 envoyer donnée
            controller.setReclamation(r);

            Stage stage = (Stage) tableReclamations.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadTable() {
        new Thread(() -> {
            try {

                // 🔥 récupérer user connecté
                int currentUserId = SessionManager.getCurrentUser().getId();

                // 🔥 charger seulement ses réclamations
                List<Reclamation> list = reclamationService.getByUserId(currentUserId);

                Platform.runLater(() ->
                        tableReclamations.setItems(FXCollections.observableArrayList(list))
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkAndShowSystemNotifications() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) return;

            List<Reclamation> mesReclamations = reclamationService.getByUserId(currentUser.getId());

            for (Reclamation r : mesReclamations) {
                boolean hasUnread = reponseService.hasUnreadAdminResponseForPatient(r.getId_reclamation());

                if (hasUnread && !r.isNotification_envoyee()) {
                    SystemNotification.showNotification(
                            "Nouvelle réponse admin",
                            "Votre réclamation " + r.getReference_reclamation() + " a reçu une nouvelle réponse."
                    );

                    reclamationService.updateNotificationEnvoyee(r.getId_reclamation(), true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}