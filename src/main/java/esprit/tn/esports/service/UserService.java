package esprit.tn.esports.service;

import esprit.tn.esports.entite.User;
import esprit.tn.esports.utils.Database;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    private Connection connection;

    public UserService() {
        connection = Database.getInstance().getConnection();
    }

    /**
     * Authenticates a user by email and password.
     * @param email The user's email
     * @param password The user's plain text password
     * @return User object if successful, null otherwise.
     */
    public User login(String email, String password) {
        String cleanEmail = email.trim();
        String query = "SELECT * FROM user WHERE email = ?";

        System.out.println("--- LOGIN ATTEMPT ---");
        System.out.println("Email being searched: '" + cleanEmail + "'");

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, cleanEmail);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                System.out.println("Email found in database! User ID: " + rs.getInt("id"));
                String dbHash = rs.getString("password");
                System.out.println("Hash in database: " + dbHash);

                if (dbHash == null || dbHash.isEmpty()) {
                    System.out.println("Password hash is null or empty!");
                    return null;
                }

                // Vérifier le mot de passe avec BCrypt
                boolean match = BCrypt.checkpw(password, dbHash);
                System.out.println("Password match result: " + match);

                if (match) {
                    System.out.println("✅ Login successful for: " + cleanEmail);
                    return new User(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("roles"),
                            rs.getString("password"),
                            rs.getString("google_id"),
                            rs.getBoolean("is_blocked"),
                            rs.getString("profile_image"),
                            rs.getString("totp_secret"),
                            rs.getBoolean("is_totp_enabled"),
                            rs.getString("type")
                    );
                } else {
                    System.out.println("❌ Password does NOT match!");
                }
            } else {
                System.out.println("Email NOT FOUND in database.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Returning null (Login Failed)");
        return null;
    }

    /**
     * Creates a new user in the database.
     * @param user The user object to create
     * @return true if successful, false otherwise
     */
    public boolean createUser(User user) {
        String query = "INSERT INTO user (email, roles, password, type, profile_image, is_blocked) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getRoles());
            pst.setString(3, user.getPassword());
            pst.setString(4, user.getType());
            pst.setString(5, user.getProfileImage() != null ? user.getProfileImage() : "default.png");
            pst.setBoolean(6, user.isBlocked());  // CORRIGÉ: isBlocked() au lieu de isIs_blocked()

            int result = pst.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing user.
     * @param user The user object with updated data
     * @return true if successful, false otherwise
     */
    public boolean updateUser(User user) {
        String query = "UPDATE user SET email = ?, roles = ?, password = ?, type = ?, profile_image = ?, is_blocked = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getRoles());
            pst.setString(3, user.getPassword());
            pst.setString(4, user.getType());
            pst.setString(5, user.getProfileImage());
            pst.setBoolean(6, user.isBlocked());  // CORRIGÉ: isBlocked() au lieu de isIs_blocked()
            pst.setInt(7, user.getId());

            int result = pst.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds a user by ID.
     * @param id The user ID
     * @return User object if found, null otherwise
     */
    public User findById(int id) {
        String query = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("google_id"),
                        rs.getBoolean("is_blocked"),
                        rs.getString("profile_image"),
                        rs.getString("totp_secret"),
                        rs.getBoolean("is_totp_enabled"),
                        rs.getString("type")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Finds a user by email.
     * @param email The user's email
     * @return User object if found, null otherwise
     */
    public User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("google_id"),
                        rs.getBoolean("is_blocked"),
                        rs.getString("profile_image"),
                        rs.getString("totp_secret"),
                        rs.getBoolean("is_totp_enabled"),
                        rs.getString("type")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates user's password.
     * @param userId The user ID
     * @param newPassword The new plain text password
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(int userId, String newPassword) {
        String hashedPassword = hashPassword(newPassword);
        String query = "UPDATE user SET password = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, hashedPassword);
            pst.setInt(2, userId);

            int result = pst.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Optional utility to hash a raw password for initial insertion.
     * @param plainPassword The plain text password
     * @return BCrypt hashed password
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Main method for testing login with different users
     */
    public static void main(String[] args) {
        UserService service = new UserService();

        // Test avec les comptes de votre base de données
        // Pour chaque compte, générez d'abord un nouveau hash avec le mot de passe souhaité
        System.out.println("=== GÉNÉRATION DE HASHES POUR VOS COMPTES ===");

        // Exemple: Générer un hash pour un nouveau mot de passe
        String testPassword = "password123";
        String newHash = hashPassword(testPassword);
        System.out.println("Pour utiliser le mot de passe '" + testPassword + "', mettez ce hash dans la BD:");
        System.out.println(newHash);
        System.out.println();

        // Test avec admin@esports.com
        System.out.println("--- Testing: admin@esports.com ---");
        User loggedIn = service.login("admin@esports.com", "admin123");
        if (loggedIn != null) {
            System.out.println("✅ Success! User type: " + loggedIn.getType());
        } else {
            System.out.println("❌ Failed to login - Mettez à jour le hash dans la base de données");
        }

        // Test avec joueur@test.com
        System.out.println("\n--- Testing: joueur@test.com ---");
        loggedIn = service.login("joueur@test.com", "password123");
        if (loggedIn != null) {
            System.out.println("✅ Success! User type: " + loggedIn.getType());
        } else {
            System.out.println("❌ Failed to login - Mettez à jour le hash dans la base de données");
        }
    }
}