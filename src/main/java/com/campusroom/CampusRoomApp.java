package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.service.AuthService;
import com.campusroom.ui.LoginDialog;
import com.campusroom.ui.MainFrame;
import javax.swing.*;

/**
 * CampusRoomApp - Main Entry Point untuk Aplikasi CampusRoom.
 */
public class CampusRoomApp {

    public static void main(String[] args) {
        // Terapkan Modern FlatLaf Look and Feel jika tersedia
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            // Modern UI Styling Enhancements
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.gridColor", new java.awt.Color(241, 245, 249));
            UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
            UIManager.put("TableHeader.bottomSeparatorColor", new java.awt.Color(226, 232, 240));
            UIManager.put("TableHeader.separatorColor", new java.awt.Color(241, 245, 249));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 34);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Inisialisasi Database SQLite & Seed Data
        DatabaseHelper.initializeDatabase();

        // Jalankan UI Autentikasi di EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null, user -> {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            });
            loginDialog.setVisible(true);

            // Jika jendela login ditutup tanpa login dan sesi masih kosong, hentikan aplikasi
            if (!AuthService.getInstance().isLoggedIn()) {
                System.exit(0);
            }
        });
    }
}
