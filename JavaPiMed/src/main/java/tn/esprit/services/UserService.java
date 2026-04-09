package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IGeneralService<User> {

    private final Connection cnx;

    public UserService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(User user) {
        String sql = "INSERT INTO user(cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getCin());
            ps.setString(2, user.getProfilePicture());
            ps.setString(3, user.getNom());
            ps.setString(4, user.getPrenom());

            if (user.getDateNaissance() != null) {
                ps.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            ps.setString(6, user.getTelephoneUser());
            ps.setString(7, user.getEmailUser());
            ps.setString(8, user.getAdresseUser());
            ps.setString(9, user.getPassword());
            ps.setBoolean(10, user.isVerified());
            ps.setString(11, user.getStatutCompte());
            ps.setString(12, user.getRoleSysteme());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }

            System.out.println("Utilisateur ajouté avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de l'utilisateur : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(User user) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            System.out.println("Utilisateur supprimé avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression de l'utilisateur : " + e.getMessage());
        }
    }

    @Override
    public void modifier(User user) {
        String sql = "UPDATE user SET cin = ?, profile_picture = ?, nom = ?, prenom = ?, date_naissance = ?, " +
                "telephone_user = ?, email_user = ?, adresse_user = ?, password = ?, statut_compte = ?, role_systeme = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, user.getCin());
            ps.setString(2, user.getProfilePicture());
            ps.setString(3, user.getNom());
            ps.setString(4, user.getPrenom());

            if (user.getDateNaissance() != null) {
                ps.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            ps.setString(6, user.getTelephoneUser());
            ps.setString(7, user.getEmailUser());
            ps.setString(8, user.getAdresseUser());
            ps.setString(9, user.getPassword());
            ps.setString(10, user.getStatutCompte());
            ps.setString(11, user.getRoleSysteme());
            ps.setInt(12, user.getId());

            ps.executeUpdate();
            System.out.println("Utilisateur modifié avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification de l'utilisateur : " + e.getMessage());
        }
    }

    @Override
    public List<User> recuperer() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des utilisateurs : " + e.getMessage());
        }
        return users;
    }

    public User findById(int id) {
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recherche de l'utilisateur : " + e.getMessage());
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCin(rs.getString("cin"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));

        Date dateNaissance = rs.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }

        user.setTelephoneUser(rs.getString("telephone_user"));
        user.setEmailUser(rs.getString("email_user"));
        user.setAdresseUser(rs.getString("adresse_user"));
        user.setPassword(rs.getString("password"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setStatutCompte(rs.getString("statut_compte"));
        user.setRoleSysteme(rs.getString("role_systeme"));

        return user;
    }
}
