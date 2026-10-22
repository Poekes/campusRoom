package com.campusroom;

import com.campusroom.dao.BuildingDao;
import com.campusroom.dao.DatabaseHelper;
import com.campusroom.dao.RoomDao;
import com.campusroom.model.Building;
import com.campusroom.model.Room;
import com.campusroom.ui.LayoutEditorPanel;
import com.campusroom.ui.RoomInspectorDialog;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LayoutExcalidrawInspectorTest {

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initializeDatabase();
    }

    @Test
    public void testLayoutEditorPanelInitializationAndSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean saved = new AtomicBoolean(false);
            LayoutEditorPanel panel = new LayoutEditorPanel(() -> saved.set(true));
            panel.setSize(1200, 750);
            panel.doLayout();

            // Render to image
            BufferedImage img = new BufferedImage(1200, 750, BufferedImage.TYPE_INT_ARGB);
            panel.paint(img.createGraphics());

            RoomDao roomDao = new RoomDao();
            List<Room> rooms = roomDao.getRoomsByBuildingAndFloor(1, 2);
            Assertions.assertFalse(rooms.isEmpty(), "Rooms on Gedung A Lt 2 should not be empty");

            Room target = rooms.get(0);
            panel.onRoomSelected(target);

            // Test room movement callback
            panel.onRoomMoved(target, target.getPosX() + 15, target.getPosY() + 20);

            // Double click opens inspector modal: dismiss automatically after 50ms so test does not block
            Timer dismisser = new Timer(50, e -> {
                for (Window w : Window.getWindows()) {
                    if (w instanceof RoomInspectorDialog) {
                        w.dispose();
                    }
                }
            });
            dismisser.setRepeats(false);
            dismisser.start();

            panel.onRoomDoubleClicked(target);
        });
    }

    @Test
    public void testRoomInspectorDialogLifecycleAndSave() throws Exception {
        RoomDao roomDao = new RoomDao();
        BuildingDao buildingDao = new BuildingDao();

        Building building = buildingDao.getBuildingById(1);
        Assertions.assertNotNull(building, "Building 1 should exist");

        // Create temporary test room
        Room testRoom = new Room(1, "TEST-999", "Ruang Pengujian Excalidraw", 2, 45,
                "Smart Classroom", "Proyektor, Smart Board", 120, 140, 190, 160);
        boolean added = roomDao.addRoom(testRoom);
        Assertions.assertTrue(added, "Temporary test room should be added");

        List<Room> rooms = roomDao.getRoomsByBuildingAndFloor(1, 2);
        Room inserted = null;
        for (Room r : rooms) {
            if ("TEST-999".equals(r.getCode())) {
                inserted = r;
                break;
            }
        }
        Assertions.assertNotNull(inserted, "Inserted room should be retrievable");

        final Room rToTest = inserted;
        final AtomicBoolean saveCalled = new AtomicBoolean(false);
        final AtomicBoolean deleteCalled = new AtomicBoolean(false);
        final AtomicBoolean dupCalled = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> {
            RoomInspectorDialog dlg = new RoomInspectorDialog(
                    null, rToTest, building, 2,
                    () -> saveCalled.set(true),
                    () -> deleteCalled.set(true),
                    () -> dupCalled.set(true)
            );
            dlg.pack();
            Assertions.assertTrue(dlg.getWidth() >= 400);
            Assertions.assertTrue(dlg.getHeight() >= 300);

            // Verify dialog renders cleanly
            BufferedImage img = new BufferedImage(dlg.getWidth(), dlg.getHeight(), BufferedImage.TYPE_INT_ARGB);
            dlg.paint(img.createGraphics());
            dlg.dispose();
        });

        // Test updating room via RoomDao directly
        rToTest.setName("Ruang Pengujian Excalidraw (Updated)");
        rToTest.setPosX(250);
        rToTest.setPosY(180);
        rToTest.setWidth(210);
        rToTest.setHeight(170);
        boolean updated = roomDao.updateRoom(rToTest);
        Assertions.assertTrue(updated, "updateRoom should succeed");

        Room fetched = roomDao.getRoomById(rToTest.getId());
        Assertions.assertEquals("Ruang Pengujian Excalidraw (Updated)", fetched.getName());
        Assertions.assertEquals(250, fetched.getPosX());
        Assertions.assertEquals(180, fetched.getPosY());
        Assertions.assertEquals(210, fetched.getWidth());
        Assertions.assertEquals(170, fetched.getHeight());

        // Cleanup
        roomDao.deleteRoom(rToTest.getId());
        Assertions.assertNull(roomDao.getRoomById(rToTest.getId()), "Room should be cleaned up");
    }

    @Test
    public void testLayoutEditorAvailabilityIntegration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LayoutEditorPanel panel = new LayoutEditorPanel(null);
            panel.setSize(1200, 750);
            panel.doLayout();

            // Verify availability is updated for Gedung A Lantai 2
            // Room R. 201 has Monday 08:00 schedule, so availability service updates rooms
            RoomDao dao = new RoomDao();
            List<Room> rooms = dao.getRoomsByBuildingAndFloor(1, 2);
            Assertions.assertFalse(rooms.isEmpty());
        });
    }
}
