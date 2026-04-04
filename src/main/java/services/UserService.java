package services;

import entities.Roles;
import entities.User;
import org.mindrot.jbcrypt.BCrypt;
import utils.DBConnection;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserService implements ICrud <User>{
    private Connection cnx;

    public UserService() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(User user) throws SQLException {

        // 🔒 1. Validation des données
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        // 🔍 2. Vérifier unicité email
        if (emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 🔐 3. Hash du mot de passe
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        // 📌 4. Valeurs par défaut (si null)
        if (user.getRole()== Roles.ROLE_ADMIN) {
            user.setType("user");
        }else if (user.getRole()== Roles.ROLE_COACH) {
            user.setType("coach");
        }else{
            user.setType("player");
        }

        if (user.getProfileImage() == null) {
            user.setProfileImage("default.png");
        }

        if (user.getGoogleId() == null) {
            user.setGoogleId(null);
        }

        user.setBlocked(false);

        if (user.getTotpSecret() == null) {
            user.setTotpSecret(null);
        }

        user.setTotpEnabled(false);

        // 🧾 5. Requête SQL
        String req = "INSERT INTO user (email, roles, password, type, google_id, is_blocked, profile_image, totp_secret, is_totp_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setString(1, user.getEmail());

            // enum → JSON
            ps.setString(2, "[\"" + user.getRole().name() + "\"]");

            ps.setString(3, user.getPassword());
            ps.setString(4, user.getType());
            ps.setString(5, user.getGoogleId());
            ps.setBoolean(6, user.isBlocked());
            ps.setString(7, user.getProfileImage());
            ps.setString(8, user.getTotpSecret());
            ps.setBoolean(9, user.isTotpEnabled());

            ps.executeUpdate();

            System.out.println("✅ User inserted successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error inserting user: " + e.getMessage());
            throw e;
        }
    }
    @Override
    public void updateOne(User user) throws SQLException {

    }

    @Override
    public void deleteOne(User user) throws SQLException {

    }

    @Override
    public List<User> selectAll() throws SQLException {
        return List.of();
    }
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0; // If count > 0, email exists
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        return false; // Default to false in case of exceptions or no result
    }
}
