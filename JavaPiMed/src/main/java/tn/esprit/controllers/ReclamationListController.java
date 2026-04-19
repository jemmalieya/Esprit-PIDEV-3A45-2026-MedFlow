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
                    reclamationService.supprimer(r);
                    loadTable();
                });

                // 🔥 MODIFIER
                editBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    openEditPage(r);
                });

                viewBtn.getStyleClass().add("btn-view");

// 🔥 VOIR RÉPONSE
                viewBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    showReclamationDetails(r);
                });viewBtn.getStyleClass().add("btn-view");

            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, editBtn, deleteBtn, viewBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        loadTable();
    }
    private void showReclamationDetails(Reclamation r) {
        try {
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

                    Label msg = new Label("📩 " + rep.getMessage());
                    Label type = new Label("📌 Type : " + rep.getType_reponse());
                    Label date = new Label("📅 " + rep.getDate_creation_rep());
                    Label lu = new Label(rep.isIs_read() ? "✔ Lu" : "❌ Non lu");

                    msg.setStyle("-fx-font-weight: bold;");
                    lu.setStyle(rep.isIs_read()
                            ? "-fx-text-fill: green;"
                            : "-fx-text-fill: red;");

                    card.getChildren().addAll(msg, type, date, lu);
                    container.getChildren().add(card);
                }
            }

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(300);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Réponses");
            alert.setHeaderText("Détails des réponses");
            alert.getDialogPane().setContent(scroll);
            alert.getDialogPane().setPrefWidth(500);
            alert.getDialogPane().setPrefHeight(350);
            alert.getDialogPane().setStyle("-fx-background-color: #f8fafc;");

            alert.showAndWait(); // UNE SEULE FOIS

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
                List<Reclamation> list = reclamationService.getAll();

                Platform.runLater(() ->
                        tableReclamations.setItems(FXCollections.observableArrayList(list))
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}