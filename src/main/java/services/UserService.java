package services;

import entities.Roles;
import entities.User;
import org.mindrot.jbcrypt.BCrypt;
import utils.DBConnection;


import javax.management.relation.Role;
import java.sql.*;
import java.util.List;

public class UserService implements ICrud <User>{
    private Connection cnx;

    public UserService() {
        this.cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public User insertOne(User user) throws SQLException {

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

        String req = "INSERT INTO user (email, roles, password, type, google_id, is_blocked, profile_image, totp_secret, is_totp_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getEmail());
            ps.setString(2, "[\"" + user.getRole().name() + "\"]");
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getType());

            // ✅ Handle NULL properly
            if (user.getGoogleId() != null) {
                ps.setString(5, user.getGoogleId());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }

            ps.setBoolean(6, user.isBlocked());
            ps.setString(7, user.getProfileImage());

            if (user.getTotpSecret() != null) {
                ps.setString(8, user.getTotpSecret());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }

            ps.setBoolean(9, user.isTotpEnabled());

            // ⚠️ IMPORTANT: use executeUpdate() normally (this is fine)
            ps.executeUpdate();

            // 🔑 GET GENERATED KEY
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            return user;
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

    public User login(String email, String password) throws SQLException {

        // 🔒 1. Validation
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        // 🧾 2. SQL Query
        String req = "SELECT * FROM user WHERE email = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                // ❌ User not found
                if (!rs.next()) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                // 🔐 3. Get hashed password from DB
                String hashedPassword = rs.getString("password");

                // ❌ Wrong password
                if (!BCrypt.checkpw(password, hashedPassword)) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                // 🚫 Check if blocked
                if (rs.getBoolean("is_blocked")) {
                    throw new IllegalStateException("Account is blocked");
                }

                // ✅ 4. Map ResultSet → User
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setPassword(hashedPassword);

                // ⚠️ roles stored as JSON → extract
                String rolesJson = rs.getString("roles"); // ["ROLE_USER"]
                String roleStr = rolesJson.replace("[\"", "").replace("\"]", "");
                user.setRole(Roles.valueOf(roleStr)); // adapt if needed

                user.setType(rs.getString("type"));
                user.setGoogleId(rs.getString("google_id"));
                user.setBlocked(rs.getBoolean("is_blocked"));
                user.setProfileImage(rs.getString("profile_image"));
                user.setTotpSecret(rs.getString("totp_secret"));
                user.setTotpEnabled(rs.getBoolean("is_totp_enabled"));

                return user;
            }

        } catch (SQLException e) {
            System.err.println("❌ Login error: " + e.getMessage());
            throw e;
        }
    }
}
