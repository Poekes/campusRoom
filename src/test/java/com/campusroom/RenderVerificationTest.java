package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.service.AuthService;
import com.campusroom.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class RenderVerificationTest {

    public static void main(String[] args) {
        System.out.println("Testing GUI rendering in headless/virtual frame mode...");
        try {
            // Apply FlatLaf
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } catch (Exception ignored) {
            }

            DatabaseHelper.initializeDatabase();
            AuthService.getInstance().login("admin", "123");

            // Test 1: Render LoginDialog
            System.out.println("Rendering LoginDialog...");
            LoginDialog loginDlg = new LoginDialog(null, null);
            loginDlg.pack();
            BufferedImage imgLogin = new BufferedImage(Math.max(1, loginDlg.getWidth()), Math.max(1, loginDlg.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gLogin = imgLogin.createGraphics();
            loginDlg.paint(gLogin);
            gLogin.dispose();
            loginDlg.dispose();
            System.out.println("[PASS] LoginDialog rendered successfully (" + imgLogin.getWidth() + "x" + imgLogin.getHeight() + ").");

            // Test 2: Render MainFrame
            System.out.println("Rendering MainFrame...");
            MainFrame mainFrame = new MainFrame();
            mainFrame.pack();
            BufferedImage imgMain = new BufferedImage(Math.max(1, mainFrame.getWidth()), Math.max(1, mainFrame.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gMain = imgMain.createGraphics();
            mainFrame.paint(gMain);
            gMain.dispose();
            System.out.println("[PASS] MainFrame rendered successfully (" + imgMain.getWidth() + "x" + imgMain.getHeight() + ").");

            // Test 3: Render LayoutEditorPanel
            System.out.println("Rendering LayoutEditorPanel...");
            LayoutEditorPanel editor = new LayoutEditorPanel(null);
            editor.setSize(1200, 700);
            editor.doLayout();
            BufferedImage imgEditor = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gEditor = imgEditor.createGraphics();
            editor.paint(gEditor);
            gEditor.dispose();
            System.out.println("[PASS] LayoutEditorPanel rendered successfully (" + imgEditor.getWidth() + "x" + imgEditor.getHeight() + ").");

            // Test 4: Render RoomInspectorDialog
            System.out.println("Rendering RoomInspectorDialog...");
            com.campusroom.dao.RoomDao roomDao = new com.campusroom.dao.RoomDao();
            com.campusroom.dao.BuildingDao buildingDao = new com.campusroom.dao.BuildingDao();
            com.campusroom.model.Room sampleRoom = roomDao.getRoomById(1);
            com.campusroom.model.Building sampleBuilding = buildingDao.getBuildingById(1);
            RoomInspectorDialog inspectorDlg = new RoomInspectorDialog(null, sampleRoom, sampleBuilding, 2, null, null, null);
            inspectorDlg.pack();
            BufferedImage imgInspector = new BufferedImage(Math.max(1, inspectorDlg.getWidth()), Math.max(1, inspectorDlg.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gInspector = imgInspector.createGraphics();
            inspectorDlg.paint(gInspector);
            gInspector.dispose();
            inspectorDlg.dispose();
            System.out.println("[PASS] RoomInspectorDialog rendered successfully (" + imgInspector.getWidth() + "x" + imgInspector.getHeight() + ").");

            mainFrame.dispose();
            System.out.println("ALL GUI RENDERING TESTS PASSED WITH ZERO EXCEPTIONS!");
        } catch (Throwable t) {
            throw new RuntimeException("GUI Rendering test failed", t);
        }
    }

    @org.junit.jupiter.api.Test
    public void testGuiRendering() {
        main(new String[]{});
    }
}
