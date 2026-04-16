package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.transform.Rotate;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.time.LocalDate;

public class NavigationController {

    @FXML
    private void handleRendezVous(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/ConsultationLayout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Button accueilButton;

    @FXML
    private Button reclamationButton;

    @FXML
    private Button blogButton;

    @FXML
    private Button pharmacieButton;

    @FXML
    private Button evenementButton;

    @FXML
    private Button consultationButton;

    @FXML
    private Button compteButton;

    // Zone profil (présente uniquement sur Compte.fxml)
    @FXML
    private TextField profileFirstNameField;

    @FXML
    private TextField profileLastNameField;

    @FXML
    private TextField profilePhoneField;

    @FXML
    private TextField profileAddressField;

    @FXML
    private TextField profileCinField;

    @FXML
    private DatePicker profileBirthDatePicker;

    @FXML
    private TextField profileEmailField;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label profileMessageLabel;

    @FXML
    private Label profileFirstNameErrorLabel;

    @FXML
    private Label profileLastNameErrorLabel;

    @FXML
    private Label profilePhoneErrorLabel;

    @FXML
    private Label profileAddressErrorLabel;

    @FXML
    private Label profileBirthDateErrorLabel;

    @FXML
    private Label profileInitialsLabel;

    // Sidebar user info (présent sur toutes les pages front)
    @FXML
    private StackPane sidebarCard;

    @FXML
    private VBox sidebarCardFront;

    @FXML
    private HBox sidebarCardBack;

    @FXML
    private ImageView sidebarProfileImage;

    @FXML
    private Label sidebarInitialsLabel;

    @FXML
    private Label sidebarNameLabel;

    @FXML
    private Label sidebarRoleLabel;

    private final UserService userService = new UserService();
    private String selectedProfileImagePath;

    // Etat du flip de la carte sidebar
    private boolean sidebarCardShowingBack = false;
    private boolean sidebarCardAnimating = false;

    // Validation flags for profile form
    private boolean profileFirstNameValid;
    private boolean profileLastNameValid;
    private boolean profilePhoneValid;
    private boolean profileAddressValid;
    private boolean profileCinValid;
    private boolean profileBirthDateValid;

    @FXML
    private void initialize() {
        applyRolePermissions();
        initProfileSection();
        initSidebarUserInfo();
        initSidebarCardFlip();
    }

    private void initProfileSection() {
        // Si on n'est pas sur la page Compte, les champs sont absents
        if (profileFirstNameField == null) {
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecté.", true);
            return;
        }

        profileFirstNameField.setText(current.getPrenom());
        profileLastNameField.setText(current.getNom());
        profilePhoneField.setText(current.getTelephoneUser());
        profileAddressField.setText(current.getAdresseUser());

        if (current.getDateNaissance() != null) {
            profileBirthDatePicker.setValue(current.getDateNaissance());
        }

        profileCinField.setText(current.getCin());
        profileCinField.setEditable(false);

        profileEmailField.setText(current.getEmailUser());
        profileEmailField.setEditable(false);

        updateAvatar(profileImageView, profileInitialsLabel, current);
        clearProfileValidationStyles();
        setProfileMessage("", false);

        // Live validation listeners
        profileFirstNameField.textProperty().addListener((obs, o, n) -> validateProfileFirstName());
        profileLastNameField.textProperty().addListener((obs, o, n) -> validateProfileLastName());
        profilePhoneField.textProperty().addListener((obs, o, n) -> validateProfilePhone());
        profileAddressField.textProperty().addListener((obs, o, n) -> validateProfileAddress());
        profileCinField.textProperty().addListener((obs, o, n) -> validateProfileCin());
        if (profileBirthDatePicker != null) {
            profileBirthDatePicker.valueProperty().addListener((obs, o, n) -> validateProfileBirthDate());
        }
    }

    private String buildInitials(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName.trim();
        String l = lastName == null ? "" : lastName.trim();
        StringBuilder sb = new StringBuilder();
        if (!f.isEmpty()) sb.append(Character.toUpperCase(f.charAt(0)));
        if (!l.isEmpty()) sb.append(Character.toUpperCase(l.charAt(0)));
        String res = sb.toString();
        return res.isEmpty() ? "MF" : res;
    }

    private void updateAvatar(ImageView imageView, Label initialsLabel, User current) {
        if (imageView == null && initialsLabel == null) return;

        String picture = current.getProfilePicture();
        boolean hasImage = false;
        if (imageView != null && picture != null && !picture.isEmpty()) {
            try {
                File file = new File(picture);
                Image img = file.exists() ? new Image(file.toURI().toString(), false) : null;
                if (img != null) {
                    imageView.setImage(img);
                    hasImage = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (initialsLabel != null) {
            if (!hasImage) {
                String initials = buildInitials(current.getPrenom(), current.getNom());
                initialsLabel.setText(initials);
                initialsLabel.setVisible(true);
            } else {
                initialsLabel.setVisible(false);
            }
        }
    }

    private void initSidebarUserInfo() {
        // Si on n'est pas dans un contexte avec sidebar, les champs sont absents
        if (sidebarNameLabel == null && sidebarProfileImage == null) {
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            if (sidebarNameLabel != null) {
                sidebarNameLabel.setText("Invité");
            }
            if (sidebarRoleLabel != null) {
                sidebarRoleLabel.setText("");
            }
            return;
        }

        if (sidebarNameLabel != null) {
            String prenom = current.getPrenom() == null ? "" : current.getPrenom().trim();
            String nom = current.getNom() == null ? "" : current.getNom().trim();
            String fullName = (prenom + " " + nom).trim();
            if (fullName.isEmpty()) {
                fullName = current.getEmailUser() != null ? current.getEmailUser() : "Utilisateur MedFlow";
            }
            sidebarNameLabel.setText(fullName);
        }

        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText(buildDisplayRole(current));
        }

        updateAvatar(sidebarProfileImage, sidebarInitialsLabel, current);
    }

    private void initSidebarCardFlip() {
        if (sidebarCard == null || sidebarCardFront == null || sidebarCardBack == null) {
            return;
        }

        sidebarCardBack.setVisible(false);
        sidebarCardBack.setManaged(false);
        sidebarCardShowingBack = false;

        sidebarCard.setOnMouseEntered(e -> {
            if (!sidebarCardShowingBack && !sidebarCardAnimating) {
                flipCard(true);
            }
        });
        sidebarCard.setOnMouseExited(e -> {
            if (sidebarCardShowingBack && !sidebarCardAnimating) {
                flipCard(false);
            }
        });
    }

    private void flipCard(boolean toBack) {
        if (sidebarCard == null || sidebarCardFront == null || sidebarCardBack == null) return;

        if (sidebarCardAnimating) return;
        sidebarCardAnimating = true;

        Node first = toBack ? sidebarCardFront : sidebarCardBack;
        Node second = toBack ? sidebarCardBack : sidebarCardFront;

        RotateTransition rt1 = new RotateTransition(Duration.millis(160), sidebarCard);
        rt1.setAxis(Rotate.Y_AXIS);
        rt1.setFromAngle(0);
        rt1.setToAngle(90);
        rt1.setOnFinished(ev -> {
            first.setVisible(false);
            first.setManaged(false);
            second.setVisible(true);
            second.setManaged(true);
            sidebarCardShowingBack = toBack;

            RotateTransition rt2 = new RotateTransition(Duration.millis(160), sidebarCard);
            rt2.setAxis(Rotate.Y_AXIS);
            rt2.setFromAngle(-90);
            rt2.setToAngle(0);
            rt2.setOnFinished(e2 -> sidebarCardAnimating = false);
            rt2.play();
        });
        rt1.play();
    }

    private String buildDisplayRole(User current) {
        String role = current.getRoleSysteme();
        String type = current.getTypeStaff();
        if (role != null) role = role.trim().toUpperCase();
        if (type != null) type = type.trim().toUpperCase();

        if (role == null || role.isBlank() || "PATIENT".equals(role)) {
            return "Patient";
        }
        if ("ADMIN".equals(role)) {
            return "Administrateur";
        }
        if ("STAFF".equals(role)) {
            if ("RESP_EVEN".equals(type)) return "Resp. événements";
            if ("RESP_PRODUCTS".equals(type)) return "Resp. produits";
            if ("RESP_BLOG".equals(type) || "RESP_RECLAMATION".equals(type)) return "Resp. réclamations/blog";
            if ("RESP_PATIENTS".equals(type)) return "Resp. patients";
            if ("RESP_USERS".equals(type)) return "Resp. utilisateurs";
            return "Staff";
        }
        return role;
    }

    private void navigate(ActionEvent event, String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyRolePermissions() {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            return;
        }

        String role = current.getRoleSysteme();
        String typeStaff = current.getTypeStaff();
        if (role != null) {
            role = role.trim().toUpperCase();
        }
        if (typeStaff != null) {
            typeStaff = typeStaff.trim().toUpperCase();
        }

        // Front sidebar :
        // - ADMIN : accès complet
        // - PATIENT : accès complet au front uniquement
        // - STAFF (RESP_EVEN, etc.) : accès complet au front + leur back-office dédié
        // Donc on ne restreint pas le menu ici.
        if ("ADMIN".equals(role) || "PATIENT".equals(role) || "STAFF".equals(role)) {
            return;
        }
    }

    private void restrictTo(Button... allowed) {
        Button[] all = new Button[]{
                accueilButton,
                reclamationButton,
                blogButton,
                pharmacieButton,
                evenementButton,
                consultationButton,
                compteButton
        };

        for (Button b : all) {
            if (b == null) continue;
            boolean isAllowed = false;
            for (Button ab : allowed) {
                if (b == ab) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                b.setDisable(true);
                b.setVisible(false);
                b.setManaged(false);
            }
        }
    }

    private void setProfileMessage(String message, boolean error) {
        if (profileMessageLabel == null) return;
        profileMessageLabel.setText(message == null ? "" : message);
        if (message == null || message.isEmpty()) {
            profileMessageLabel.setStyle("");
        } else {
            String color = error ? "#dc2626" : "#16a34a";
            profileMessageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleAccueil(ActionEvent event) {
        navigate(event, "/FrontFXML/Accueil.fxml", "MedFlow - Accueil");
    }
    @FXML
    private void handlePharmacie(ActionEvent event) {
        navigate(event, "/FrontFXML/Pharmacie.fxml", "MedFlow - Pharmacie");
    }
    @FXML
    private void handleCompte(ActionEvent event) {
        navigate(event, "/FrontFXML/Compte.fxml", "MedFlow - Mon profil");
    }
    @FXML
    private void handleRéclamation(ActionEvent event) {
        navigate(event, "/FrontFXML/Réclamation.fxml", "MedFlow - Reclamation");
    }
    @FXML
    private void handleBlog(ActionEvent event) {
        navigate(event, "/FrontFXML/Blog.fxml", "MedFlow - Blog");
    }
    @FXML
    private void handleÉvénement(ActionEvent event) {
        navigate(event, "/FrontFXML/Événement.fxml", "MedFlow - Evenement");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        navigate(event, "/FrontFXML/Login.fxml", "MedFlow - Connexion");
    }

    // Actions profil (page Compte)

    @FXML
    private void handleChooseProfilePhoto(ActionEvent event) {
        if (profileImageView == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedProfileImagePath = file.getAbsolutePath();
            profileImageView.setImage(new Image(file.toURI().toString(), false));
            if (profileInitialsLabel != null) {
                profileInitialsLabel.setVisible(false);
            }
            if (sidebarProfileImage != null) {
                sidebarProfileImage.setImage(new Image(file.toURI().toString(), false));
            }
            if (sidebarInitialsLabel != null) {
                sidebarInitialsLabel.setVisible(false);
            }
        }
    }

    @FXML
    private void handleProfileCancel(ActionEvent event) {
        initProfileSection();
    }

    @FXML
    private void handleProfileSave(ActionEvent event) {
        if (profileFirstNameField == null) return;

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecté.", true);
            return;
        }

        String prenom = profileFirstNameField.getText() == null ? "" : profileFirstNameField.getText().trim();
        String nom = profileLastNameField.getText() == null ? "" : profileLastNameField.getText().trim();
        String tel = profilePhoneField.getText() == null ? "" : profilePhoneField.getText().trim();
        String adresse = profileAddressField.getText() == null ? "" : profileAddressField.getText().trim();
        LocalDate dateNaissance = profileBirthDatePicker != null ? profileBirthDatePicker.getValue() : null;

        if (!validateProfileForm()) {
            setProfileMessage("Veuillez corriger les champs en rouge.", true);
            return;
        }

        current.setPrenom(prenom);
        current.setNom(nom);
        current.setTelephoneUser(tel);
        current.setAdresseUser(adresse);
        if (dateNaissance != null) {
            current.setDateNaissance(dateNaissance);
        }
        if (selectedProfileImagePath != null && !selectedProfileImagePath.isEmpty()) {
            current.setProfilePicture(selectedProfileImagePath);
        }

        try {
            userService.modifier(current);
            setProfileMessage("Profil mis à jour avec succès.", false);
            initSidebarUserInfo();
        } catch (Exception e) {
            e.printStackTrace();
            setProfileMessage("Erreur lors de la mise à jour du profil.", true);
        }
    }

    private void markProfileError(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
    }

    private void markProfileValid(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #16a34a; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
    }

    private void showFieldError(Label label, String message) {
        if (label == null) return;
        label.setText(message == null ? "" : message);
        boolean visible = message != null && !message.isBlank();
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void clearProfileValidationStyles() {
        if (profileFirstNameField != null) profileFirstNameField.setStyle("");
        if (profileLastNameField != null) profileLastNameField.setStyle("");
        if (profilePhoneField != null) profilePhoneField.setStyle("");
        if (profileAddressField != null) profileAddressField.setStyle("");
        if (profileCinField != null) profileCinField.setStyle("");
        showFieldError(profileFirstNameErrorLabel, "");
        showFieldError(profileLastNameErrorLabel, "");
        showFieldError(profilePhoneErrorLabel, "");
        showFieldError(profileAddressErrorLabel, "");
        showFieldError(profileBirthDateErrorLabel, "");
    }

    private void validateProfileFirstName() {
        String value = profileFirstNameField.getText() == null ? "" : profileFirstNameField.getText().trim();
        if (!value.isEmpty()) {
            profileFirstNameValid = true;
            markProfileValid(profileFirstNameField);
            showFieldError(profileFirstNameErrorLabel, "");
        } else {
            profileFirstNameValid = false;
            markProfileError(profileFirstNameField);
            showFieldError(profileFirstNameErrorLabel, "⚠ Prénom obligatoire.");
        }
    }

    private void validateProfileLastName() {
        String value = profileLastNameField.getText() == null ? "" : profileLastNameField.getText().trim();
        if (!value.isEmpty()) {
            profileLastNameValid = true;
            markProfileValid(profileLastNameField);
            showFieldError(profileLastNameErrorLabel, "");
        } else {
            profileLastNameValid = false;
            markProfileError(profileLastNameField);
            showFieldError(profileLastNameErrorLabel, "⚠ Nom obligatoire.");
        }
    }

    private void validateProfilePhone() {
        String value = profilePhoneField.getText() == null ? "" : profilePhoneField.getText().trim();
        if (value.isEmpty()) {
            profilePhoneValid = false;
            markProfileError(profilePhoneField);
            showFieldError(profilePhoneErrorLabel, "⚠ Téléphone obligatoire.");
            return;
        }
        if (value.matches("\\d{8}") || value.matches("\\+216\\d{8}")) {
            profilePhoneValid = true;
            markProfileValid(profilePhoneField);
            showFieldError(profilePhoneErrorLabel, "");
        } else {
            profilePhoneValid = false;
            markProfileError(profilePhoneField);
            showFieldError(profilePhoneErrorLabel, "⚠ Format invalide. Ex\u00a0: 54430709 ou +21654430709");
        }
    }

    private void validateProfileAddress() {
        String value = profileAddressField.getText() == null ? "" : profileAddressField.getText().trim();
        if (value.isEmpty()) {
            profileAddressValid = false;
            markProfileError(profileAddressField);
            showFieldError(profileAddressErrorLabel, "⚠ Adresse obligatoire.");
        } else {
            profileAddressValid = true;
            markProfileValid(profileAddressField);
            showFieldError(profileAddressErrorLabel, "");
        }
    }

    private void validateProfileCin() {
        String value = profileCinField.getText() == null ? "" : profileCinField.getText().trim();
        if (value.matches("\\d{8}")) {
            profileCinValid = true;
            markProfileValid(profileCinField);
        } else {
            profileCinValid = false;
            markProfileError(profileCinField);
        }
    }

    private void validateProfileBirthDate() {
        if (profileBirthDatePicker != null && profileBirthDatePicker.getValue() != null) {
            profileBirthDateValid = true;
            showFieldError(profileBirthDateErrorLabel, "");
        } else {
            profileBirthDateValid = false;
            showFieldError(profileBirthDateErrorLabel, "⚠ Date de naissance obligatoire.");
        }
    }

    private boolean validateProfileForm() {
        validateProfileFirstName();
        validateProfileLastName();
        validateProfilePhone();
        validateProfileAddress();
        validateProfileCin();
        validateProfileBirthDate();

        StringBuilder sb = new StringBuilder();
        if (!profileFirstNameValid) sb.append("- Prénom obligatoire.\n");
        if (!profileLastNameValid) sb.append("- Nom obligatoire.\n");
        if (!profilePhoneValid) sb.append("- Téléphone invalide.\n");
        if (!profileAddressValid) sb.append("- Adresse obligatoire.\n");
        if (!profileCinValid) sb.append("- CIN invalide (8 chiffres).\n");
        if (!profileBirthDateValid) sb.append("- Date de naissance obligatoire.\n");

        if (sb.length() > 0) {
            setProfileMessage(sb.toString(), true);
            return false;
        }

        setProfileMessage("", false);
        return true;
    }

}
