package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.animation.RotateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.transform.Rotate;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.services.TotpService;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tn.esprit.services.UserProfileGamificationService;

public class NavigationController {
    private static final double APP_WIDTH = 1400;
    private static final double APP_HEIGHT = 820;

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
    private Label totpStatusLabel;

    @FXML
    private Label faceLoginStatusLabel;

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

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private ProgressBar activationScoreProgress;

    @FXML
    private Label activationScoreValueLabel;

    @FXML
    private Label activationLevelValueLabel;

    @FXML
    private VBox nextActionsContainer;

    @FXML
    private VBox badgesContainer;

    @FXML
    private CheckBox prefEmailSecurityAlertsCheck;

    @FXML
    private CheckBox prefPushLoginAlertsCheck;

    @FXML
    private Button prefSaveButton;

    @FXML
    private Label prefStatusLabel;

    @FXML
    private Label prefSummaryLabel;

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
    private final TotpService totpService = new TotpService();
    private final UserProfileGamificationService userProfileGamificationService = new UserProfileGamificationService();
    private File selectedProfileImageFile;

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
    private boolean gamificationAnimated;
    private boolean loadingPreferences;
    private boolean preferenceListenersAttached;
    private boolean preferencesDirty;

    @FXML
    private void initialize() {
        try {
            applyRolePermissions();
            initProfileSection();
            initSidebarUserInfo();
            initSidebarCardFlip();
        } catch (Throwable t) {
            // Fail-safe: never let profile/sidebar init crash the full login navigation.
            System.err.println("[NavigationController] initialize fallback: " + t.getMessage());
            t.printStackTrace();
        }
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
        refreshSecurityStatus(current);
        initializePreferenceOptions();
        refreshProfileGamification(current);
        selectedProfileImageFile = null;
        clearPasswordChangeForm();
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
                String imageUrl = resolveStoredImageUrl(picture);
                Image img = imageUrl == null ? null : new Image(imageUrl, true);
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

    private String resolveStoredImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalized = storedPath.trim().replace("\\", "/");
        String uploadsSubdir = resolveSymfonyUploadsSubdir();
        boolean looksLikeBareFileName = !normalized.contains("/");
        String lower = normalized.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return normalized;
        }

        try {
            Path raw = Paths.get(storedPath);
            if (raw.isAbsolute() && Files.exists(raw)) {
                return raw.toUri().toString();
            }
        } catch (Exception ignored) {
        }

        Path symfonyPublicDir = resolveSymfonyPublicDirPath();
        if (symfonyPublicDir != null) {
            Path direct = symfonyPublicDir.resolve(normalized).normalize();
            if (Files.exists(direct)) {
                return direct.toUri().toString();
            }

            if (looksLikeBareFileName) {
                Path prefixed = symfonyPublicDir.resolve(uploadsSubdir).resolve(normalized).normalize();
                if (Files.exists(prefixed)) {
                    return prefixed.toUri().toString();
                }
            }
        }

        String baseUrl = resolveSymfonyBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            if (looksLikeBareFileName) {
                return cleanBase + "/" + uploadsSubdir + "/" + normalized;
            }
            return cleanBase + "/" + normalized;
        }

        return null;
    }

    private String resolveSymfonyBaseUrl() {
        String[] keys = {"symfony.base.url", "SYMFONY_BASE_URL", "symfony_base_url"};
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                value = System.getenv(key);
            }
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Path resolveSymfonyPublicDirPath() {
        String[] keys = {"symfony.public.dir", "SYMFONY_PUBLIC_DIR", "symfony_public_dir"};
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                value = System.getenv(key);
            }
            if (value != null && !value.isBlank()) {
                try {
                    Path p = Paths.get(value.trim()).toAbsolutePath().normalize();
                    if (Files.exists(p)) {
                        return p;
                    }
                } catch (RuntimeException ignored) {
                    // Ignore invalid OS path values and try next alias.
                }
            }
        }
        return null;
    }

    private String resolveSymfonyUploadsSubdir() {
        String[] keys = {"symfony.uploads.subdir", "SYMFONY_UPLOADS_SUBDIR", "symfony_uploads_subdir"};
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                value = System.getenv(key);
            }
            if (value != null && !value.isBlank()) {
                String normalized = value.trim().replace("\\", "/");
                normalized = normalized.replaceAll("^/+", "").replaceAll("/+$", "");
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        return "uploads";
    }

    private String copyProfileImageToSymfonyUploads(User current, File sourceFile) throws IOException {
        if (current == null || sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Fichier image introuvable.");
        }

        Path publicDir = resolveSymfonyPublicDirPath();
        if (publicDir == null) {
            throw new IOException("SYMFONY_PUBLIC_DIR non configure ou inaccessible.");
        }

        String uploadsSubdir = resolveSymfonyUploadsSubdir();
        Path uploadsDir = publicDir.resolve(uploadsSubdir).normalize();
        Files.createDirectories(uploadsDir);

        String originalName = sourceFile.getName();
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            extension = originalName.substring(dot).toLowerCase();
        }

        String fileName = "profile-" + current.getId() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path target = uploadsDir.resolve(fileName).normalize();

        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return uploadsSubdir + "/" + fileName;
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

        try {
            updateAvatar(sidebarProfileImage, sidebarInitialsLabel, current);
        } catch (Throwable ignored) {
            // Keep navigation usable even if avatar resolution fails.
        }
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


    private void applyStandardWindowSize(Stage stage) {
        stage.setMinWidth(APP_WIDTH);
        stage.setMinHeight(APP_HEIGHT);
        if (!stage.isMaximized()) {
            stage.setWidth(APP_WIDTH);
            stage.setHeight(APP_HEIGHT);
            stage.centerOnScreen();
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
            selectedProfileImageFile = file;
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
        current.setDateNaissance(dateNaissance);
        if (selectedProfileImageFile != null) {
            try {
                String relativePath = copyProfileImageToSymfonyUploads(current, selectedProfileImageFile);
                current.setProfilePicture(relativePath);
            } catch (IOException ioException) {
                setProfileMessage("Echec copie image vers Symfony public/uploads: " + ioException.getMessage(), true);
                return;
            }
        }

        try {
            userService.modifier(current);
            setProfileMessage("Profil enregistre avec succes.", false);
            selectedProfileImageFile = null;
            initSidebarUserInfo();
            refreshProfileGamification(current);
        } catch (Exception e) {
            setProfileMessage("Echec de sauvegarde profil: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handlePasswordChange(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }

        String currentPassword = currentPasswordField == null ? "" : String.valueOf(currentPasswordField.getText());
        String newPassword = newPasswordField == null ? "" : String.valueOf(newPasswordField.getText());
        String confirmPassword = confirmPasswordField == null ? "" : String.valueOf(confirmPasswordField.getText());

        if (currentPassword.isBlank()) {
            showFieldError(passwordErrorLabel, "Le mot de passe actuel est obligatoire.");
            setProfileMessage("Veuillez renseigner le mot de passe actuel.", true);
            return;
        }

        String passwordError = validateNewPassword(newPassword);
        if (passwordError != null) {
            showFieldError(passwordErrorLabel, passwordError);
            setProfileMessage("Le nouveau mot de passe ne respecte pas les contraintes.", true);
            return;
        }

        if (newPassword.equals(currentPassword)) {
            showFieldError(passwordErrorLabel, "Le nouveau mot de passe doit etre different de l'actuel.");
            setProfileMessage("Choisissez un mot de passe different.", true);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showFieldError(passwordErrorLabel, "La confirmation ne correspond pas au nouveau mot de passe.");
            setProfileMessage("Confirmation du mot de passe invalide.", true);
            return;
        }

        if (current.getEmailUser() == null || current.getEmailUser().isBlank()) {
            showFieldError(passwordErrorLabel, "Impossible de verifier le compte courant.");
            setProfileMessage("Compte invalide: email manquant.", true);
            return;
        }

        User auth = userService.authenticate(current.getEmailUser(), currentPassword);
        if (auth == null) {
            showFieldError(passwordErrorLabel, "Le mot de passe actuel est incorrect.");
            setProfileMessage("Echec de verification du mot de passe actuel.", true);
            return;
        }

        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        if (!userService.updateUserPassword(current.getId(), hashed)) {
            showFieldError(passwordErrorLabel, "Erreur lors de la mise a jour du mot de passe.");
            setProfileMessage("Impossible de mettre a jour le mot de passe.", true);
            return;
        }

        current.setPassword(hashed);
        showFieldError(passwordErrorLabel, "");
        clearPasswordChangeForm();
        setProfileMessage("Mot de passe mis a jour avec succes.", false);
    }

    private void initializePreferenceOptions() {
        attachPreferenceListeners();
        updatePreferencesSummary();
        setPreferencesDirty(false);
    }

    private void attachPreferenceListeners() {
        if (preferenceListenersAttached) {
            return;
        }

        if (prefEmailSecurityAlertsCheck != null) {
            prefEmailSecurityAlertsCheck.selectedProperty().addListener((obs, oldValue, newValue) -> onPreferenceControlChanged());
        }
        if (prefPushLoginAlertsCheck != null) {
            prefPushLoginAlertsCheck.selectedProperty().addListener((obs, oldValue, newValue) -> onPreferenceControlChanged());
        }

        preferenceListenersAttached = true;
    }

    private void onPreferenceControlChanged() {
        if (loadingPreferences) {
            return;
        }
        setPreferencesDirty(true);
        updatePreferencesSummary();
    }

    private void setPreferencesDirty(boolean dirty) {
        preferencesDirty = dirty;
        if (prefSaveButton != null) {
            prefSaveButton.setDisable(!dirty);
        }
        if (prefStatusLabel != null) {
            prefStatusLabel.setText(dirty ? "Modifications non enregistrees" : "Preferences synchronisees");
            prefStatusLabel.getStyleClass().setAll("pref-status-label", dirty ? "pref-status-dirty" : "pref-status-clean");
        }
    }

    private void updatePreferencesSummary() {
        if (prefSummaryLabel == null) {
            return;
        }
        String sec = prefEmailSecurityAlertsCheck != null && prefEmailSecurityAlertsCheck.isSelected() ? "ON" : "OFF";
        String push = prefPushLoginAlertsCheck != null && prefPushLoginAlertsCheck.isSelected() ? "ON" : "OFF";
        prefSummaryLabel.setText("Alertes securite e-mail: " + sec + " | Alertes login push: " + push);
    }

    private static final class GamificationSnapshot {
        private final UserProfileGamificationService.ActivationScoreResponse score;
        private final UserProfileGamificationService.NextBestActionsResponse actions;
        private final UserProfileGamificationService.BadgesResponse badges;
        private final UserProfileGamificationService.PreferencesResponse preferences;

        private GamificationSnapshot(
                UserProfileGamificationService.ActivationScoreResponse score,
                UserProfileGamificationService.NextBestActionsResponse actions,
                UserProfileGamificationService.BadgesResponse badges,
                UserProfileGamificationService.PreferencesResponse preferences
        ) {
            this.score = score;
            this.actions = actions;
            this.badges = badges;
            this.preferences = preferences;
        }
    }

    private void refreshProfileGamification(User user) {
        if (user == null || activationScoreProgress == null) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        UserProfileGamificationService.ActivationScoreResponse scoreResponse =
                                userProfileGamificationService.getActivationScore(user.getId());
                        UserProfileGamificationService.NextBestActionsResponse actionsResponse =
                                userProfileGamificationService.getNextBestActions(user.getId());
                        UserProfileGamificationService.BadgesResponse badgesResponse =
                                userProfileGamificationService.getBadges(user.getId());
                        UserProfileGamificationService.PreferencesResponse preferencesResponse =
                                userProfileGamificationService.patchPreferences(
                                        user.getId(),
                                        new UserProfileGamificationService.PreferencesPatch(null, null, null, null, null, null)
                                );

                        return new GamificationSnapshot(scoreResponse, actionsResponse, badgesResponse, preferencesResponse);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .thenAccept(snapshot -> Platform.runLater(() -> {
                    if (snapshot == null) {
                        setProfileMessage("Impossible de charger les donnees de securite profile.", true);
                        return;
                    }

                    double targetProgress = Math.max(0, Math.min(100, snapshot.score.activationScore())) / 100.0;
                    animateActivationProgress(targetProgress);
                    if (activationScoreValueLabel != null) {
                        activationScoreValueLabel.setText(snapshot.score.activationScore() + "%");
                    }
                    if (activationLevelValueLabel != null) {
                        activationLevelValueLabel.setText(snapshot.score.level());
                    }

                    List<UserProfileGamificationService.NextAction> actions = snapshot.actions.actions();
                    renderNextActions(actions);

                    List<UserProfileGamificationService.BadgeStatus> badges = snapshot.badges.badges();
                    renderBadges(badges);

                    if (!gamificationAnimated) {
                        animateGamificationPanels();
                        gamificationAnimated = true;
                    }

                    applyPreferencesToControls(snapshot.preferences.preferences());
                }));
    }

    private void animateActivationProgress(double targetProgress) {
        if (activationScoreProgress == null) {
            return;
        }
        KeyValue kv = new KeyValue(activationScoreProgress.progressProperty(), targetProgress, javafx.animation.Interpolator.EASE_BOTH);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(700), kv));
        timeline.play();
    }

    private void animateGamificationPanels() {
        animateAppear(nextActionsContainer, 60);
        animateAppear(badgesContainer, 140);
        animateAppear(prefEmailSecurityAlertsCheck, 220);
        animateAppear(prefPushLoginAlertsCheck, 250);
        animateAppear(prefSaveButton, 280);
    }

    private void animateAppear(Node node, int delayMs) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setDelay(Duration.millis(delayMs));
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void renderNextActions(java.util.List<UserProfileGamificationService.NextAction> actions) {
        if (nextActionsContainer == null) {
            return;
        }
        nextActionsContainer.getChildren().clear();

        if (actions == null || actions.isEmpty()) {
            Label doneLabel = new Label("Excellent ! Aucun point bloquant, votre profil est complet.");
            doneLabel.getStyleClass().add("profile-help-text");
            nextActionsContainer.getChildren().add(doneLabel);
            return;
        }

        int max = Math.min(actions.size(), 5);
        for (int i = 0; i < max; i++) {
            UserProfileGamificationService.NextAction action = actions.get(i);
            HBox actionRow = new HBox(10);
            actionRow.getStyleClass().add("action-item-row");

            Label titleLabel = new Label(action.title());
            titleLabel.getStyleClass().add("action-item-title");

            Label pointsLabel = new Label("+" + action.rewardPoints() + " pts");
            pointsLabel.getStyleClass().add("action-item-points");

            Label priorityLabel = new Label(action.priority());
            String prio = action.priority() == null ? "LOW" : action.priority().trim().toUpperCase();
            if ("HIGH".equals(prio)) {
                priorityLabel.getStyleClass().add("action-priority-high");
            } else if ("MEDIUM".equals(prio)) {
                priorityLabel.getStyleClass().add("action-priority-medium");
            } else {
                priorityLabel.getStyleClass().add("action-priority-low");
            }

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            actionRow.getChildren().addAll(titleLabel, spacer, pointsLabel, priorityLabel);

            actionRow.setOpacity(0);
            nextActionsContainer.getChildren().add(actionRow);
            FadeTransition rowFade = new FadeTransition(Duration.millis(240), actionRow);
            rowFade.setDelay(Duration.millis(i * 70L));
            rowFade.setFromValue(0);
            rowFade.setToValue(1);
            rowFade.play();
        }
    }

    private void renderBadges(java.util.List<UserProfileGamificationService.BadgeStatus> badges) {
        if (badgesContainer == null) {
            return;
        }
        badgesContainer.getChildren().clear();

        if (badges == null || badges.isEmpty()) {
            Label noBadge = new Label("Aucun badge pour le moment.");
            noBadge.getStyleClass().add("profile-help-text");
            badgesContainer.getChildren().add(noBadge);
            return;
        }

        for (int i = 0; i < badges.size(); i++) {
            UserProfileGamificationService.BadgeStatus badge = badges.get(i);

            HBox badgeRow = new HBox(8);
            badgeRow.getStyleClass().add("badge-row");

            Label icon = new Label(badge.unlocked() ? "✓" : "•");
            icon.getStyleClass().add(badge.unlocked() ? "badge-icon-ok" : "badge-icon-pending");

            Label title = new Label(badge.title());
            title.getStyleClass().add(badge.unlocked() ? "badge-chip-unlocked" : "badge-chip-locked");

            String progressTxt = (int) Math.round(badge.progress() * 100) + "%";
            Label progressLabel = new Label(progressTxt);
            progressLabel.getStyleClass().add("badge-progress-text");

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            badgeRow.getChildren().addAll(icon, title, spacer, progressLabel);

            badgeRow.setOpacity(0);
            badgesContainer.getChildren().add(badgeRow);
            FadeTransition rowFade = new FadeTransition(Duration.millis(260), badgeRow);
            rowFade.setDelay(Duration.millis(i * 55L));
            rowFade.setFromValue(0);
            rowFade.setToValue(1);
            rowFade.play();
        }
    }

    private void applyPreferencesToControls(UserProfileGamificationService.UserPreferences preferences) {
        if (preferences == null) {
            return;
        }
        loadingPreferences = true;
        if (prefEmailSecurityAlertsCheck != null) {
            prefEmailSecurityAlertsCheck.setSelected(preferences.emailSecurityAlerts());
        }
        if (prefPushLoginAlertsCheck != null) {
            prefPushLoginAlertsCheck.setSelected(preferences.pushLoginAlerts());
        }
        loadingPreferences = false;
        updatePreferencesSummary();
        setPreferencesDirty(false);
    }

    @FXML
    private void handleSaveUserPreferences(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }

        try {
            UserProfileGamificationService.PreferencesPatch patch = new UserProfileGamificationService.PreferencesPatch(
                    null,
                    null,
                    prefEmailSecurityAlertsCheck == null ? null : prefEmailSecurityAlertsCheck.isSelected(),
                    null,
                    prefPushLoginAlertsCheck == null ? null : prefPushLoginAlertsCheck.isSelected(),
                    null
            );

            UserProfileGamificationService.PreferencesResponse response =
                    userProfileGamificationService.patchPreferences(current.getId(), patch);

            applyPreferencesToControls(response.preferences());
            refreshProfileGamification(current);
            setProfileMessage("Preferences enregistrees.", false);
        } catch (Exception e) {
            setProfileMessage("Erreur lors de l'enregistrement des preferences.", true);
        }
    }


    private void refreshSecurityStatus(User user) {
        if (user == null) {
            return;
        }
        if (totpStatusLabel != null) {
            totpStatusLabel.setText(isTotpEligibleRole(user)
                    ? (user.isTotpEnabled() ? "Activee" : "Desactivee")
                    : "Reservee STAFF/ADMIN");
        }
        if (faceLoginStatusLabel != null) {
            faceLoginStatusLabel.setText(user.isFaceLoginEnabled() ? "Activee" : "Desactivee");
        }
    }

    private boolean isTotpEligibleRole(User user) {
        if (user == null || user.getRoleSysteme() == null) {
            return false;
        }
        String role = user.getRoleSysteme().trim().toUpperCase();
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    private Image generateQrCodeImage(String content, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
        return SwingFXUtils.toFXImage(bufferedImage, null);
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
        // Date de naissance optionnelle: on ne bloque plus la sauvegarde si absente.
        profileBirthDateValid = true;
        showFieldError(profileBirthDateErrorLabel, "");
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

        if (sb.length() > 0) {
            setProfileMessage(sb.toString(), true);
            return false;
        }

        setProfileMessage("", false);
        return true;
    }

    private String validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            return "Le nouveau mot de passe est obligatoire.";
        }
        if (password.length() < 8) {
            return "Minimum 8 caracteres requis.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Ajoutez au moins 1 lettre majuscule.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Ajoutez au moins 1 lettre minuscule.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Ajoutez au moins 1 chiffre.";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Ajoutez au moins 1 caractere special.";
        }
        return null;
    }

    private void clearPasswordChangeForm() {
        if (currentPasswordField != null) {
            currentPasswordField.clear();
        }
        if (newPasswordField != null) {
            newPasswordField.clear();
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.clear();
        }
        showFieldError(passwordErrorLabel, "");
    }

    @FXML
    public void handleEnableTotp(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }
        if (!isTotpEligibleRole(current)) {
            setProfileMessage("2FA reservee aux comptes STAFF/ADMIN.", true);
            return;
        }

        String secret = totpService.generateSecret();
        String account = current.getEmailUser() == null || current.getEmailUser().isBlank()
                ? ("user-" + current.getId())
                : current.getEmailUser();
        String otpAuth = totpService.buildOtpAuthUrl("MedFlow", account, secret);

        Image qrImage;
        try {
            qrImage = generateQrCodeImage(otpAuth, 240);
        } catch (Exception ex) {
            setProfileMessage("Generation QR 2FA impossible.", true);
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Activer 2FA");
        dialog.setHeaderText("Scannez le QR code avec Google Authenticator puis entrez le code.");
        ButtonType verifyButton = new ButtonType("Verifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(verifyButton, cancelButton);

        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(240);
        qrView.setFitHeight(240);
        qrView.setPreserveRatio(true);

        Label secretLabel = new Label("Secret manuel: " + secret);
        Label serverHintLabel = new Label("Diagnostic serveur (debug): "
                + totpService.getCodeAtOffset(secret, -1) + " | "
                + totpService.getCurrentCode(secret) + " | "
                + totpService.getCodeAtOffset(secret, 1));
        serverHintLabel.setWrapText(true);
        TextField codeField = new TextField();
        codeField.setPromptText("Code a 6 chiffres");

        VBox content = new VBox(10, qrView, secretLabel, serverHintLabel, codeField);
        dialog.getDialogPane().setContent(content);

        Node verifyNode = dialog.getDialogPane().lookupButton(verifyButton);
        verifyNode.setDisable(true);
        codeField.textProperty().addListener((obs, o, n) -> verifyNode.setDisable(!n.matches("\\d{6}")));

        dialog.setResultConverter(btn -> btn == verifyButton ? codeField.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            setProfileMessage("Activation 2FA annulee.", true);
            return;
        }

        if (!totpService.verifyCode(secret, result.get())) {
            String prev = totpService.getCodeAtOffset(secret, -1);
            String now = totpService.getCurrentCode(secret);
            String next = totpService.getCodeAtOffset(secret, 1);
            setProfileMessage("Code 2FA invalide. Verifiez l'heure auto du telephone."
                    + " Codes serveur: [" + prev + ", " + now + ", " + next + "]", true);
            return;
        }

        if (!userService.saveTotpSettings(current.getId(), secret, true)) {
            setProfileMessage("Impossible de sauvegarder la 2FA.", true);
            return;
        }

        current.setTotpSecret(secret);
        current.setTotpEnabled(true);
        refreshSecurityStatus(current);
        refreshProfileGamification(current);
        setProfileMessage("2FA activee avec succes.", false);
    }

    @FXML
    public void handleDisableTotp(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }
        if (!isTotpEligibleRole(current)) {
            setProfileMessage("2FA reservee aux comptes STAFF/ADMIN.", true);
            return;
        }

        if (!userService.saveTotpSettings(current.getId(), null, false)) {
            setProfileMessage("Impossible de desactiver la 2FA.", true);
            return;
        }

        current.setTotpSecret(null);
        current.setTotpEnabled(false);
        refreshSecurityStatus(current);
        refreshProfileGamification(current);
        setProfileMessage("2FA desactivee.", false);
    }

    @FXML
    public void handleEnableFaceLogin(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }

        if (current.getFaceReferenceEmbedding() == null || current.getFaceReferenceEmbedding().isBlank()) {
            setProfileMessage("Aucun visage enregistre. Cliquez sur 'Enroller visage'.", true);
            return;
        }

        if (!userService.setFaceLoginEnabled(current.getId(), true)) {
            setProfileMessage("Impossible d'activer Face Login.", true);
            return;
        }

        current.setFaceLoginEnabled(true);
        refreshSecurityStatus(current);
        refreshProfileGamification(current);
        setProfileMessage("Face Login active.", false);
    }

    @FXML
    public void handleEnrollFaceFromCompte(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/FaceEnrollment.fxml"));
            Parent root = loader.load();

            FaceEnrollmentController controller = loader.getController();
            controller.setOnEnrollmentComplete(new FaceEnrollmentController.OnEnrollmentComplete() {
                @Override
                public void onSuccess(String faceEmbedding) {
                    boolean saved = userService.updateFaceEnrollment(current.getId(), faceEmbedding);
                    if (!saved) {
                        setProfileMessage("Enrolement facial echec: " + userService.getLastFaceEnrollmentError(), true);
                        return;
                    }
                    current.setFaceReferenceEmbedding(faceEmbedding);
                    current.setFaceLoginEnabled(true);
                    refreshSecurityStatus(current);
                    refreshProfileGamification(current);
                    setProfileMessage("Visage enregistre avec succes. Face Login active.", false);
                }

                @Override
                public void onSkip() {
                    setProfileMessage("Enrolement facial annule.", true);
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("MedFlow - Enrolement facial");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            setProfileMessage("Impossible d'ouvrir l'enrolement facial.", true);
        }
    }

    @FXML
    public void handleDisableFaceLogin(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            setProfileMessage("Aucun utilisateur connecte.", true);
            return;
        }

        if (!userService.setFaceLoginEnabled(current.getId(), false)) {
            setProfileMessage("Impossible de desactiver Face Login.", true);
            return;
        }

        current.setFaceLoginEnabled(false);
        refreshSecurityStatus(current);
        refreshProfileGamification(current);
        setProfileMessage("Face Login desactive.", false);
    }
}
