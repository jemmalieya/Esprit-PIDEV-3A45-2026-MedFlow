package tn.esprit.tools;

public final class AuthThemeManager {

    private static boolean lightMode;

    private AuthThemeManager() {
        // Utility class
    }

    public static boolean isLightMode() {
        return lightMode;
    }

    public static void setLightMode(boolean enabled) {
        lightMode = enabled;
    }

    public static void toggle() {
        lightMode = !lightMode;
    }
}

