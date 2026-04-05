package tn.esprit.entities;

import java.sql.Timestamp;
import java.time.LocalDate;

public class User {
    private int id;
    private String cin;
    private String profilePicture;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String telephoneUser;
    private String emailUser;
    private String adresseUser;
    private String password;
    private Timestamp derniereConnexion;
    private boolean isVerified;
    private String statutCompte;
    private String roleSysteme;
    private String typeStaff;
    private String verificationToken;
    private Timestamp tokenExpiresAt;
    private String staffRequestStatus;
    private String staffRequestType;
    private String staffRequestMessage;
    private Timestamp staffRequestedAt;
    private Timestamp staffReviewedAt;
    private Integer staffReviewedBy;
    private String googleId;
    private String resetToken;
    private Timestamp resetTokenExpiresAt;
    private String banReason;
    private Timestamp bannedAt;
    private String staffRequestProofPath;
    private String staffDocuments;
    private String staffRequestReason;
    private String totpSecret;
    private boolean totpEnabled;
    private String lastLoginIp;
    private String lastLoginCountry;
    private Timestamp lastLoginAt;
    private boolean faceLoginEnabled;
    private Timestamp faceEnrolledAt;
    private Timestamp faceLastVerifiedAt;
    private int faceFailedAttempts;
    private Timestamp faceLockedUntil;
    private String faceReferenceEmbedding;

    public User() {
    }

    public User(int id, String cin, String profilePicture, String nom, String prenom, LocalDate dateNaissance,
                String telephoneUser, String emailUser, String adresseUser, Timestamp derniereConnexion,
                String password, boolean isVerified, String statutCompte, String roleSysteme, String typeStaff,
                String verificationToken, Timestamp tokenExpiresAt, String staffRequestStatus,
                String staffRequestType, String staffRequestMessage, Timestamp staffRequestedAt,
                Timestamp staffReviewedAt, Integer staffReviewedBy, String googleId, String resetToken,
                Timestamp resetTokenExpiresAt, String banReason, Timestamp bannedAt,
                String staffRequestProofPath, String staffDocuments, String staffRequestReason,
                String totpSecret, boolean totpEnabled, String lastLoginIp, String lastLoginCountry,
                Timestamp lastLoginAt, boolean faceLoginEnabled, Timestamp faceEnrolledAt,
                Timestamp faceLastVerifiedAt, int faceFailedAttempts, Timestamp faceLockedUntil,
                String faceReferenceEmbedding) {
        this.id = id;
        this.cin = cin;
        this.profilePicture = profilePicture;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.telephoneUser = telephoneUser;
        this.emailUser = emailUser;
        this.adresseUser = adresseUser;
        this.derniereConnexion = derniereConnexion;
        this.password = password;
        this.isVerified = isVerified;
        this.statutCompte = statutCompte;
        this.roleSysteme = roleSysteme;
        this.typeStaff = typeStaff;
        this.verificationToken = verificationToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.staffRequestStatus = staffRequestStatus;
        this.staffRequestType = staffRequestType;
        this.staffRequestMessage = staffRequestMessage;
        this.staffRequestedAt = staffRequestedAt;
        this.staffReviewedAt = staffReviewedAt;
        this.staffReviewedBy = staffReviewedBy;
        this.googleId = googleId;
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
        this.banReason = banReason;
        this.bannedAt = bannedAt;
        this.staffRequestProofPath = staffRequestProofPath;
        this.staffDocuments = staffDocuments;
        this.staffRequestReason = staffRequestReason;
        this.totpSecret = totpSecret;
        this.totpEnabled = totpEnabled;
        this.lastLoginIp = lastLoginIp;
        this.lastLoginCountry = lastLoginCountry;
        this.lastLoginAt = lastLoginAt;
        this.faceLoginEnabled = faceLoginEnabled;
        this.faceEnrolledAt = faceEnrolledAt;
        this.faceLastVerifiedAt = faceLastVerifiedAt;
        this.faceFailedAttempts = faceFailedAttempts;
        this.faceLockedUntil = faceLockedUntil;
        this.faceReferenceEmbedding = faceReferenceEmbedding;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getTelephoneUser() {
        return telephoneUser;
    }

    public void setTelephoneUser(String telephoneUser) {
        this.telephoneUser = telephoneUser;
    }

    public String getEmailUser() {
        return emailUser;
    }

    public void setEmailUser(String emailUser) {
        this.emailUser = emailUser;
    }

    public String getAdresseUser() {
        return adresseUser;
    }

    public void setAdresseUser(String adresseUser) {
        this.adresseUser = adresseUser;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(Timestamp derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(String statutCompte) {
        this.statutCompte = statutCompte;
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public String getTypeStaff() {
        return typeStaff;
    }

    public void setTypeStaff(String typeStaff) {
        this.typeStaff = typeStaff;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public Timestamp getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Timestamp tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getStaffRequestStatus() {
        return staffRequestStatus;
    }

    public void setStaffRequestStatus(String staffRequestStatus) {
        this.staffRequestStatus = staffRequestStatus;
    }

    public String getStaffRequestType() {
        return staffRequestType;
    }

    public void setStaffRequestType(String staffRequestType) {
        this.staffRequestType = staffRequestType;
    }

    public String getStaffRequestMessage() {
        return staffRequestMessage;
    }

    public void setStaffRequestMessage(String staffRequestMessage) {
        this.staffRequestMessage = staffRequestMessage;
    }

    public Timestamp getStaffRequestedAt() {
        return staffRequestedAt;
    }

    public void setStaffRequestedAt(Timestamp staffRequestedAt) {
        this.staffRequestedAt = staffRequestedAt;
    }

    public Timestamp getStaffReviewedAt() {
        return staffReviewedAt;
    }

    public void setStaffReviewedAt(Timestamp staffReviewedAt) {
        this.staffReviewedAt = staffReviewedAt;
    }

    public Integer getStaffReviewedBy() {
        return staffReviewedBy;
    }

    public void setStaffReviewedBy(Integer staffReviewedBy) {
        this.staffReviewedBy = staffReviewedBy;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Timestamp getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(Timestamp resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public Timestamp getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Timestamp bannedAt) {
        this.bannedAt = bannedAt;
    }

    public String getStaffRequestProofPath() {
        return staffRequestProofPath;
    }

    public void setStaffRequestProofPath(String staffRequestProofPath) {
        this.staffRequestProofPath = staffRequestProofPath;
    }

    public String getStaffDocuments() {
        return staffDocuments;
    }

    public void setStaffDocuments(String staffDocuments) {
        this.staffDocuments = staffDocuments;
    }

    public String getStaffRequestReason() {
        return staffRequestReason;
    }

    public void setStaffRequestReason(String staffRequestReason) {
        this.staffRequestReason = staffRequestReason;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getLastLoginCountry() {
        return lastLoginCountry;
    }

    public void setLastLoginCountry(String lastLoginCountry) {
        this.lastLoginCountry = lastLoginCountry;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isFaceLoginEnabled() {
        return faceLoginEnabled;
    }

    public void setFaceLoginEnabled(boolean faceLoginEnabled) {
        this.faceLoginEnabled = faceLoginEnabled;
    }

    public Timestamp getFaceEnrolledAt() {
        return faceEnrolledAt;
    }

    public void setFaceEnrolledAt(Timestamp faceEnrolledAt) {
        this.faceEnrolledAt = faceEnrolledAt;
    }

    public Timestamp getFaceLastVerifiedAt() {
        return faceLastVerifiedAt;
    }

    public void setFaceLastVerifiedAt(Timestamp faceLastVerifiedAt) {
        this.faceLastVerifiedAt = faceLastVerifiedAt;
    }

    public int getFaceFailedAttempts() {
        return faceFailedAttempts;
    }

    public void setFaceFailedAttempts(int faceFailedAttempts) {
        this.faceFailedAttempts = faceFailedAttempts;
    }

    public Timestamp getFaceLockedUntil() {
        return faceLockedUntil;
    }

    public void setFaceLockedUntil(Timestamp faceLockedUntil) {
        this.faceLockedUntil = faceLockedUntil;
    }

    public String getFaceReferenceEmbedding() {
        return faceReferenceEmbedding;
    }

    public void setFaceReferenceEmbedding(String faceReferenceEmbedding) {
        this.faceReferenceEmbedding = faceReferenceEmbedding;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", cin='" + cin + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", telephoneUser='" + telephoneUser + '\'' +
                ", emailUser='" + emailUser + '\'' +
                ", adresseUser='" + adresseUser + '\'' +
                ", password='" + password + '\'' +
                ", derniereConnexion=" + derniereConnexion +
                ", isVerified=" + isVerified +
                ", statutCompte='" + statutCompte + '\'' +
                ", roleSysteme='" + roleSysteme + '\'' +
                ", typeStaff='" + typeStaff + '\'' +
                ", verificationToken='" + verificationToken + '\'' +
                ", tokenExpiresAt=" + tokenExpiresAt +
                ", staffRequestStatus='" + staffRequestStatus + '\'' +
                ", staffRequestType='" + staffRequestType + '\'' +
                ", staffRequestMessage='" + staffRequestMessage + '\'' +
                ", staffRequestedAt=" + staffRequestedAt +
                ", staffReviewedAt=" + staffReviewedAt +
                ", staffReviewedBy=" + staffReviewedBy +
                ", googleId='" + googleId + '\'' +
                ", resetToken='" + resetToken + '\'' +
                ", resetTokenExpiresAt=" + resetTokenExpiresAt +
                ", banReason='" + banReason + '\'' +
                ", bannedAt=" + bannedAt +
                ", staffRequestProofPath='" + staffRequestProofPath + '\'' +
                ", staffDocuments='" + staffDocuments + '\'' +
                ", staffRequestReason='" + staffRequestReason + '\'' +
                ", totpSecret='" + totpSecret + '\'' +
                ", totpEnabled=" + totpEnabled +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", lastLoginCountry='" + lastLoginCountry + '\'' +
                ", lastLoginAt=" + lastLoginAt +
                ", faceLoginEnabled=" + faceLoginEnabled +
                ", faceEnrolledAt=" + faceEnrolledAt +
                ", faceLastVerifiedAt=" + faceLastVerifiedAt +
                ", faceFailedAttempts=" + faceFailedAttempts +
                ", faceLockedUntil=" + faceLockedUntil +
                ", faceReferenceEmbedding='" + faceReferenceEmbedding + '\'' +
                '}';
    }
}