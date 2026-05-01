package esprit.tn.esports.test;

import org.mindrot.jbcrypt.BCrypt;

public class FixAllUsers {
    public static void main(String[] args) {
        System.out.println("=== REQUÊTES SQL À EXÉCUTER DANS phpMyAdmin ===\n");

        // 1. Pour admin@clutchx.com (mot de passe: admin123)
        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt());
        System.out.println("-- Admin (admin123)");
        System.out.println("UPDATE user SET password = '" + adminHash + "' WHERE id = 23;\n");

        // 2. Pour player@clutchx.com (mot de passe: password123)
        String playerHash = BCrypt.hashpw("password123", BCrypt.gensalt());
        System.out.println("-- Player (password123)");
        System.out.println("UPDATE user SET password = '" + playerHash + "' WHERE id = 24;\n");

        // 3. Pour coach@clutchx.com (mot de passe: coach123)
        String coachHash = BCrypt.hashpw("coach123", BCrypt.gensalt());
        System.out.println("-- Coach (coach123)");
        System.out.println("UPDATE user SET password = '" + coachHash + "' WHERE id = 25;\n");

        // 4. Pour user@clutchx.com (mot de passe: user123)
        String userHash = BCrypt.hashpw("user123", BCrypt.gensalt());
        System.out.println("-- User (user123)");
        System.out.println("UPDATE user SET password = '" + userHash + "' WHERE id = 26;\n");
    }
}