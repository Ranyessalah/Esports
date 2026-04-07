package utils;


import entities.Roles;
import entities.User;

import java.util.prefs.Preferences;

public class PreferencesRepository {

    private static final String NODE_PATH   = "/clutchx/session";
    private final Preferences prefs = Preferences.userRoot().node(NODE_PATH);

    private static final String KEY_IS_CONNECTED   = "isConnected";
    private static final String KEY_USER_ID        = "userId";
    private static final String KEY_EMAIL          = "email";
    private static final String KEY_ROLE           = "role";
    private static final String KEY_TYPE           = "type";
    private static final String KEY_PROFILE_IMAGE  = "profileImage";
    private static final String KEY_IS_BLOCKED     = "isBlocked";
    private static final String KEY_TOTP_ENABLED   = "totpEnabled";
    private static final String KEY_REMEMBER_ME    = "rememberMe";

    private static final String DEFAULT_STRING  = "";
    private static final int    DEFAULT_INT     = -1;
    private static final boolean DEFAULT_BOOL   = false;

    public void saveSession(User user, boolean rememberMe) {
        if (user == null) throw new IllegalArgumentException("User ne peut pas être null");

        prefs.putBoolean(KEY_IS_CONNECTED,  true);
        prefs.putInt    (KEY_USER_ID,       user.getId());
        prefs.put       (KEY_EMAIL,         user.getEmail() != null    ? user.getEmail()        : DEFAULT_STRING);
        prefs.put       (KEY_ROLE,          user.getRole()  != null    ? user.getRole().name()  : DEFAULT_STRING);
        prefs.put       (KEY_TYPE,          user.getType()  != null    ? user.getType()         : DEFAULT_STRING);
        prefs.put       (KEY_PROFILE_IMAGE, user.getProfileImage() != null ? user.getProfileImage() : "default.png");
        prefs.putBoolean(KEY_IS_BLOCKED,    user.isBlocked());
        prefs.putBoolean(KEY_TOTP_ENABLED,  user.isTotpEnabled());
        prefs.putBoolean(KEY_REMEMBER_ME,   rememberMe);

        flush();
        System.out.println("userSaved avec success");
    }

    public void saveSession(User user) {
        saveSession(user, true);
    }

    public User loadSession() {
        if (!isConnected()) return null;

        boolean remember = prefs.getBoolean(KEY_REMEMBER_ME, DEFAULT_BOOL);
        if (!remember) return null;

        User user = new User();
        user.setId          (prefs.getInt    (KEY_USER_ID,       DEFAULT_INT));
        user.setEmail       (prefs.get       (KEY_EMAIL,         DEFAULT_STRING));
        user.setType        (prefs.get       (KEY_TYPE,          DEFAULT_STRING));
        user.setProfileImage(prefs.get       (KEY_PROFILE_IMAGE, "default.png"));
        user.setBlocked     (prefs.getBoolean(KEY_IS_BLOCKED,    DEFAULT_BOOL));
        user.setTotpEnabled (prefs.getBoolean(KEY_TOTP_ENABLED,  DEFAULT_BOOL));

        String roleStr = prefs.get(KEY_ROLE, DEFAULT_STRING);
        if (!roleStr.isEmpty()) {
            try {
                user.setRole(Roles.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                user.setRole(Roles.ROLE_PLAYER); // fallback sécurisé
            }
        }

        return user;
    }

    public void clearSession() {
        try {
            prefs.clear();
            flush();
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la suppression de la session : " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return prefs.getBoolean(KEY_IS_CONNECTED, DEFAULT_BOOL);
    }

    public String getSessionEmail() {
        return prefs.get(KEY_EMAIL, DEFAULT_STRING);
    }

    public int getSessionUserId() {
        return prefs.getInt(KEY_USER_ID, DEFAULT_INT);
    }

    public Roles getSessionRole() {
        String roleStr = prefs.get(KEY_ROLE, DEFAULT_STRING);
        if (roleStr.isEmpty()) return Roles.ROLE_PLAYER;
        try {
            return Roles.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return Roles.ROLE_PLAYER;
        }
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, DEFAULT_BOOL);
    }


    public void updateProfileImage(String imagePath) {
        prefs.put(KEY_PROFILE_IMAGE, imagePath != null ? imagePath : "default.png");
        flush();
    }


    public void updateEmail(String email) {
        if (email != null && !email.isBlank()) {
            prefs.put(KEY_EMAIL, email);
            flush();
        }
    }

    private void flush() {
        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("⚠️ Erreur flush préférences : " + e.getMessage());
        }
    }
}
