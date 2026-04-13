package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Produit;
import tn.esprit.services.ProduitService;

import java.io.File;

public class ProduitController {

    @FXML
    private Button btnAjouterProduit;

    @FXML
    private Button btnResetProduit;

    @FXML
    private Button btnUploadProduit;

    @FXML
    private ComboBox<String> cbCategorieProduit;

    @FXML
    private Label lblImageProduit;

    @FXML
    private RadioButton rbDisponibleProduit;

    @FXML
    private RadioButton rbIndisponibleProduit;

    @FXML
    private RadioButton rbRuptureProduit;

    @FXML
    private TextArea taDescriptionProduit;

    @FXML
    private TextField tfNomProduit;

    @FXML
    private TextField tfPrixProduit;

    @FXML
    private TextField tfQuantiteProduit;

    private String imagePath = "";

    @FXML
    public void initialize() {
        cbCategorieProduit.getItems().addAll("Médicaments", "Parapharmacie", "Matériel");
        cbCategorieProduit.setValue("Médicaments");
        lblImageProduit.setText("Aucun fichier choisi");
        rbDisponibleProduit.setSelected(true);
    }

    @FXML
    void ajouterProduit(ActionEvent event) {
        try {
            String nom = tfNomProduit.getText().trim();
            String description = taDescriptionProduit.getText().trim();
            String categorie = cbCategorieProduit.getValue();

            if (nom.isEmpty() || description.isEmpty() || tfPrixProduit.getText().trim().isEmpty()
                    || tfQuantiteProduit.getText().trim().isEmpty() || categorie == null) {
                throw new Exception("Veuillez remplir tous les champs obligatoires.");
            }

            double prix = Double.parseDouble(tfPrixProduit.getText().trim());
            int quantite = Integer.parseInt(tfQuantiteProduit.getText().trim());

            String statut = "";
            if (rbDisponibleProduit.isSelected()) {
                statut = "Disponible";
            } else if (rbRuptureProduit.isSelected()) {
                statut = "Rupture";
            } else if (rbIndisponibleProduit.isSelected()) {
                statut = "Indisponible";
            }

            String image = imagePath.isEmpty() ? lblImageProduit.getText() : imagePath;

            Produit produit = new Produit(nom, description, prix, quantite,  image,categorie, statut);

            ProduitService produitService = new ProduitService();
            produitService.ajouter(produit);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Produit ajouté avec succès !");
            alert.showAndWait();

            viderChampsPrivate();

            // si tu veux revenir automatiquement à la page back après ajout,
            // décommente la ligne suivante :
            // retourProduit(event);

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Prix ou quantité invalide.");
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors de l'ajout : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void choisirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", ".jpg", ".png", ".jpeg", ".webp")
        );

        File fichier = fileChooser.showOpenDialog(btnUploadProduit.getScene().getWindow());

        if (fichier != null) {
            imagePath = fichier.getAbsolutePath();
            lblImageProduit.setText(fichier.getName());
        } else {
            lblImageProduit.setText("Aucun fichier choisi");
        }
    }

    @FXML
    void retourProduit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProduitBack.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnResetProduit.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Produits");
            stage.show();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de retourner à la page produits : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void viderChamps(ActionEvent event) {
        viderChampsPrivate();
    }

    private void viderChampsPrivate() {
        tfNomProduit.clear();
        taDescriptionProduit.clear();
        tfPrixProduit.clear();
        tfQuantiteProduit.clear();
        cbCategorieProduit.setValue("Médicaments");
        lblImageProduit.setText("Aucun fichier choisi");
        imagePath = "";
        rbDisponibleProduit.setSelected(true);
    }
}