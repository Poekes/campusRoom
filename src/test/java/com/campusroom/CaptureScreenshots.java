package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.service.AuthService;
import com.campusroom.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CaptureScreenshots {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception ignored) {}

        DatabaseHelper.initializeDatabase();
        File outDir = new File("screenshots");
        outDir.mkdirs();

        // 1. Admin MainFrame
        AuthService.getInstance().login("admin", "123");
        MainFrame frame = new MainFrame();

        // Floor Plan
        frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
        saveComponent(frame, "screenshots/01_floorplan.png", 1380, 860);

        // Catalog
        frame.switchToView(SidebarNavigationPanel.NAV_CATALOG);
        saveComponent(frame, "screenshots/02_catalog.png", 1380, 860);

        // Booking
        frame.switchToView(SidebarNavigationPanel.NAV_BOOKING);
        saveComponent(frame, "screenshots/03_booking.png", 1380, 860);

        // Secretary Validation
        frame.switchToView(SidebarNavigationPanel.NAV_SEC_VALIDATION);
        saveComponent(frame, "screenshots/04_sec_validation.png", 1380, 860);

        // Secretary Regular Schedule
        frame.switchToView(SidebarNavigationPanel.NAV_SEC_SCHEDULE);
        saveComponent(frame, "screenshots/05_sec_schedule.png", 1380, 860);

        // Admin Approval
        frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_APPROVAL);
        saveComponent(frame, "screenshots/06_admin_approval.png", 1380, 860);

        // Admin Campus & Building
        frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_CAMPUS);
        saveComponent(frame, "screenshots/07_admin_campus.png", 1380, 860);

        // Admin Rooms
        frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_ROOMS);
        saveComponent(frame, "screenshots/08_admin_rooms.png", 1380, 860);

        // Layout Editor
        frame.switchToView(SidebarNavigationPanel.NAV_LAYOUT_EDITOR);
        saveComponent(frame, "screenshots/09_layout_editor.png", 1380, 860);

        // At 1200w resolution
        frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
        saveComponent(frame, "screenshots/10_floorplan_1200w.png", 1200, 750);

        // Student Role view (locked admin menus)
        AuthService.getInstance().login("mahasiswa", "123");
        frame.onUserChanged(AuthService.getInstance().getCurrentUser());
        frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
        saveComponent(frame, "screenshots/11_floorplan_student.png", 1380, 860);

        // Collapsed Sidebar Views (70px rail for maximum screen real estate)
        javax.swing.Action toggleAct = frame.getRootPane().getActionMap().get("toggleSidebar");
        if (toggleAct != null) {
            toggleAct.actionPerformed(new java.awt.event.ActionEvent(frame, 0, "toggle"));
        }
        frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
        saveComponent(frame, "screenshots/12_floorplan_collapsed_sidebar.png", 1380, 860);

        AuthService.getInstance().login("admin", "123");
        frame.onUserChanged(AuthService.getInstance().getCurrentUser());
        frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_APPROVAL);
        saveComponent(frame, "screenshots/13_admin_approval_collapsed_sidebar.png", 1380, 860);

        frame.switchToView(SidebarNavigationPanel.NAV_CATALOG);
        saveComponent(frame, "screenshots/14_catalog_collapsed_sidebar.png", 1380, 860);

        // 15. Room Inspector Modal Dialog
        com.campusroom.dao.RoomDao roomDao = new com.campusroom.dao.RoomDao();
        com.campusroom.dao.BuildingDao buildingDao = new com.campusroom.dao.BuildingDao();
        com.campusroom.model.Room sampleRoom = roomDao.getRoomById(1);
        com.campusroom.model.Building sampleBuilding = buildingDao.getBuildingById(1);
        RoomInspectorDialog modal = new RoomInspectorDialog(frame, sampleRoom, sampleBuilding, 2, null, null, null);
        modal.pack();
        modal.setSize(680, 520);
        modal.validate();
        modal.doLayout();
        BufferedImage imgModal = new BufferedImage(modal.getWidth(), modal.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gModal = imgModal.createGraphics();
        modal.getRootPane().paint(gModal);
        gModal.dispose();
        ImageIO.write(imgModal, "png", new File("screenshots/15_room_inspector_modal.png"));
        modal.dispose();

        System.out.println("All screenshots successfully generated in screenshots/");
        frame.dispose();
    }

    private static void saveComponent(JFrame frame, String filename, int w, int h) throws Exception {
        frame.setSize(w, h);
        frame.addNotify();
        frame.validate();
        frame.doLayout();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        frame.getRootPane().paint(g2);
        g2.dispose();
        ImageIO.write(img, "png", new File(filename));
    }
}
