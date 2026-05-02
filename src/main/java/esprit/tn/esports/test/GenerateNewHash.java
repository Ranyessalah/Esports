package esprit.tn.esports.test;

import org.mindrot.jbcrypt.BCrypt;

public class GenerateNewHash {
    public static void main(String[] args) {
        String password = "password123";
        String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("=========================================");
        System.out.println("NOUVEAU HASH À UTILISER:");
        System.out.println(newHash);
        System.out.println("\nCOPIEZ-COLLEZ CETTE REQUÊTE SQL:");
        System.out.println("UPDATE user SET password = '" + newHash + "' WHERE email = 'joueur@test.com';");
        System.out.println("=========================================");
    }
}