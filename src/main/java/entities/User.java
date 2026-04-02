package entities;

public class User {
    private int id;
    private String email;
    private Roles role;
    private String type;
    private String googleId;
    private boolean isBlocked;
    private String profileImage;
    private String totpSecret;
    private boolean isTotpEnabled;
    private String password;

    public User(boolean isTotpEnabled, String totpSecret, String profileImage, boolean isBlocked, String googleId, String type, Roles role, String email, String password) {
        this.isTotpEnabled = isTotpEnabled;
        this.totpSecret = totpSecret;
        this.profileImage = profileImage;
        this.isBlocked = isBlocked;
        this.googleId = googleId;
        this.type = type;
        this.role = role;
        this.email = email;
        this.password = password;
    }

    public User() {
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Roles getRole() {
        return role;
    }

    public String getType() {
        return type;
    }

    public String getGoogleId() {
        return googleId;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public boolean isTotpEnabled() {
        return isTotpEnabled;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        isTotpEnabled = totpEnabled;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", type='" + type + '\'' +
                ", googleId='" + googleId + '\'' +
                ", isBlocked=" + isBlocked +
                ", profileImage='" + profileImage + '\'' +
                ", totpSecret='" + totpSecret + '\'' +
                ", isTotpEnabled=" + isTotpEnabled +
                '}';
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
