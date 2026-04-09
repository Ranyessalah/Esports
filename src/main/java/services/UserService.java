package services;

import entities.Roles;
import entities.User;
import org.mindrot.jbcrypt.BCrypt;
import utils.DBConnection;
import utils.PreferencesRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements ICrud<User> {
    private final PlayerService playerService;
    private final CoachService coachService;
    private Connection cnx;
    public UserService() {
        this.playerService = new PlayerService();
        this.coachService = new CoachService();
        this.cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public User insertOne(User user) throws SQLException {

        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (user.getEmail() == null || user.getEmail().isBlank()) throw new IllegalArgumentException("Email is required");
        if (user.getPassword() == null || user.getPassword().length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (user.getRole() == null) throw new IllegalArgumentException("Role is required");

        if (emailExists(user.getEmail())) throw new IllegalArgumentException("Email already exists");

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        if (user.getRole() == Roles.ROLE_ADMIN) {
            user.setType("user");
        } else if (user.getRole() == Roles.ROLE_COACH) {
            user.setType("coach");
        } else {
            user.setType("player");
        }

        if (user.getProfileImage() == null) user.setProfileImage("default.png");
        if (user.getGoogleId() == null) user.setGoogleId(null);
        user.setBlocked(false);
        if (user.getTotpSecret() == null) user.setTotpSecret(null);
        user.setTotpEnabled(false);

        String req = "INSERT INTO user (email, roles, password, type, google_id, is_blocked, profile_image, totp_secret, is_totp_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, "[\"" + user.getRole().name() + "\"]");
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getType());
            if (user.getGoogleId() != null) ps.setString(5, user.getGoogleId());
            else ps.setNull(5, Types.VARCHAR);
            ps.setBoolean(6, user.isBlocked());
            ps.setString(7, user.getProfileImage());
            if (user.getTotpSecret() != null) ps.setString(8, user.getTotpSecret());
            else ps.setNull(8, Types.VARCHAR);
            ps.setBoolean(9, user.isTotpEnabled());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
                else throw new SQLException("Creating user failed, no ID obtained.");
            }
            return user;
        }
    }


    @Override
    public List<User> selectAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM user ORDER BY id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }


    @Override
    public void updateOne(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Valid user with ID is required for update");
        }

        String req = "UPDATE user SET email = ?, roles = ?, is_blocked = ?, profile_image = ?, type = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, "[\"" + user.getRole().name() + "\"]");
            ps.setBoolean(3, user.isBlocked());
            ps.setString(4, user.getProfileImage() != null ? user.getProfileImage() : "default.png");
            ps.setString(5, user.getType() != null ? user.getType() : "player");
            ps.setInt(6, user.getId());
            ps.executeUpdate();
        }
    }


    @Override
    public void deleteOne(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Valid user with ID is required for deletion");
        }
        String req = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            if (user.getRole() == Roles.ROLE_COACH) {
               coachService.supprimer(user.getId());
            } else if (user.getRole()==Roles.ROLE_PLAYER) {
                playerService.supprimer(user.getId());

            }
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        return false;
    }

    public User login(String email, String password) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        String req = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Invalid email or password");

                String hashedPassword = rs.getString("password");
                if (!BCrypt.checkpw(password, hashedPassword)) throw new IllegalArgumentException("Invalid email or password");
                if (rs.getBoolean("is_blocked")) throw new IllegalStateException("Account is blocked");
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Login error: " + e.getMessage());
            throw e;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));

        String rolesJson = rs.getString("roles");
        if (rolesJson != null) {
            String roleStr = rolesJson.replace("[\"", "").replace("\"]", "").trim();
            try {
                user.setRole(Roles.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                user.setRole(Roles.ROLE_PLAYER); // fallback
            }
        }

        user.setType(rs.getString("type"));
        user.setGoogleId(rs.getString("google_id"));
        user.setBlocked(rs.getBoolean("is_blocked"));
        user.setProfileImage(rs.getString("profile_image"));
        user.setTotpSecret(rs.getString("totp_secret"));
        user.setTotpEnabled(rs.getBoolean("is_totp_enabled"));
        return user;
    }
}