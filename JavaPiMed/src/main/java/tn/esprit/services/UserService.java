package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection cn;

    public UserService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    public List<User> getStaffByRoleAndType(String roleSysteme, String typeStaff) {
        List<User> users = new ArrayList<>();

        // Try snake_case columns first.
        String querySnakeCase = "SELECT id, nom, prenom, role_systeme, type_staff FROM user WHERE role_systeme = ? AND type_staff = ? ORDER BY nom, prenom";
        // Fallback for schemas using camelCase columns.
        String queryCamelCase = "SELECT id, nom, prenom, roleSysteme, typeStaff FROM user WHERE roleSysteme = ? AND typeStaff = ? ORDER BY nom, prenom";

        boolean loaded = loadUsersWithQuery(users, querySnakeCase, roleSysteme, typeStaff, "role_systeme", "type_staff");
        if (!loaded) {
            loadUsersWithQuery(users, queryCamelCase, roleSysteme, typeStaff, "roleSysteme", "typeStaff");
        }

        return users;
    }

    private boolean loadUsersWithQuery(
            List<User> target,
            String sql,
            String roleSysteme,
            String typeStaff,
            String roleColumn,
            String typeColumn
    ) {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, roleSysteme);
            ps.setString(2, typeStaff);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setRoleSysteme(rs.getString(roleColumn));
                    user.setTypeStaff(rs.getString(typeColumn));
                    target.add(user);
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
