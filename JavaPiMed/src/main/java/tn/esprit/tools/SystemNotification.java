package tn.esprit.tools;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SystemNotification {

    private static TrayIcon trayIcon;

    public static void initTray() {
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("SystemTray non supporté");
                return;
            }

            if (trayIcon != null) {
                return;
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showNotification(String title, String message) {
        try {
            initTray();

            if (trayIcon != null) {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}