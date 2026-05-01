package esprit.tn.esports.entite;

import java.util.List;

public class User {

    private int id;
    private String email;
    private String roles;
    private String password;
    private String googleId;
    private boolean isBlocked;
    private String profileImage;
    private String totpSecret;
    private boolean isTotpEnabled;
    private String type;

    // Constructor
    public User() {}

    public User(int id, String email, String roles, String password, String googleId,
                boolean isBlocked, String profileImage, String totpSecret,
                boolean isTotpEnabled, String type) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.googleId = googleId;
        this.isBlocked = isBlocked;
        this.profileImage = profileImage;
        this.totpSecret = totpSecret;
        this.isTotpEnabled = isTotpEnabled;
        this.type = type;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public boolean isTotpEnabled() { return isTotpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { isTotpEnabled = totpEnabled; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}