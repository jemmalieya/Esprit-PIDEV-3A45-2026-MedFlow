package tn.esprit.tools;

import tn.esprit.entities.User;

/**
 * Simple in-memory session holder for the currently connected user.
 * This is JVM-local and resets when the application restarts.
 */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
        // utility class
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
