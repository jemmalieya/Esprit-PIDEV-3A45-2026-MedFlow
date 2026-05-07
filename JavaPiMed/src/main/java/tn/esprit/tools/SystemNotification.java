package tn.esprit.tools;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SystemNotification {

    private static TrayIcon trayIcon;

    public static boolean initTray() {
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("SystemTray non supporté");
                return false;
            }

            if (trayIcon != null) {
                return true;
            }

            SystemTray tray = SystemTray.getSystemTray();

            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLUE);
            g.fillOval(0, 0, 16, 16);
            g.dispose();

            trayIcon = new TrayIcon(image, "MedFlow Notifications");
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean showNotification(String title, String message) {
        try {
            if (!initTray()) {
                return false;
            }

            if (trayIcon != null) {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}