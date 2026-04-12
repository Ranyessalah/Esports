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
        String cleanEmail = email.trim(); // Remove accidential spaces!
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
                
                // Symfony/PHP sometimes saves hashes starting with $2y$. jBCrypt expects $2a$.
                if (dbHash != null && dbHash.startsWith("$2y$")) {
                    dbHash = dbHash.replaceFirst("^\\$2y\\$", "\\$2a\\$");
                    System.out.println("Hash converted for Java: " + dbHash);
                }

                // Temporary debug test if jBCrypt fails
                try {
                    boolean match = dbHash != null && BCrypt.checkpw(password, dbHash);
                    System.out.println("Password match result: " + match);
                    
                    if (!match) {
                        System.out.println(">>> HEY! The password didn't match. If you want this password to work, put this exact text in your database password column: " + BCrypt.hashpw(password, BCrypt.gensalt()));
                    }

                    if (match) {
                        return new User(
                                rs.getInt("id"),
                                rs.getString("email"),
                                rs.getString("roles"),
                                rs.getString("password"),
                                null, false, null, null, false,
                                rs.getString("type")
                        );
                    }
                } catch (Exception e) {
                    System.err.println("JBCrypt threw an exception: " + e.getMessage());
                }
            } else {
                System.out.println("Email NOT FOUND in database.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("CRITICAL: Database connection is null! Make sure your MySQL is running.");
            e.printStackTrace();
        }

        System.out.println("Returning null (Login Failed)");
        return null; // Login failed
    }

    /**
     * Optional utility to hash an raw password for initial insertion.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}
