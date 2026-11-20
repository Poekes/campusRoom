package com.campusroom;

import com.campusroom.dao.*;
import com.campusroom.model.*;
import com.campusroom.service.AuthService;
import com.campusroom.ui.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FloorPlanConsistencyTest {

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initializeDatabase();
    }

    @Test
    public void testSeedingConsistency() {
        RoomDao roomDao = new RoomDao();
        BuildingDao buildingDao = new BuildingDao();
        CampusDao campusDao = new CampusDao();
        FloorPlanDao floorPlanDao = new FloorPlanDao();

        // 1. Verify Campuses (Currently only Kampus 2 active as requested, extensible via admin)
        List<Campus> campuses = campusDao.getAllCampuses();
        Assertions.assertEquals(1, campuses.size(), "Should have 1 campus (Kampus 2)");
        Assertions.assertEquals("Kampus 2", campuses.get(0).getName());

        // 2. Verify Buildings
        List<Building> buildings = buildingDao.getAllBuildings();
        Assertions.assertEquals(3, buildings.size(), "Should have 3 buildings");

        // 3. Verify Floor Plans
        Assertions.assertEquals("images/floorplan_gedung_a.png", floorPlanDao.getFloorPlanImage(1, 1));
        Assertions.assertEquals("images/floorplan_gedung_b.png", floorPlanDao.getFloorPlanImage(2, 1));
        Assertions.assertEquals("images/kampus2_lantai_1.png", floorPlanDao.getFloorPlanImage(3, 1));
        Assertions.assertEquals("images/kampus2_lantai_2.png", floorPlanDao.getFloorPlanImage(3, 2));
        Assertions.assertEquals("images/kampus2_lantai_3.png", floorPlanDao.getFloorPlanImage(3, 3));

        // 4. Verify Total Rooms in DB (All 43 real rooms preserved)
        List<Room> allG1Rooms = roomDao.getRoomsByBuildingId(1);
        List<Room> allG2Rooms = roomDao.getRoomsByBuildingId(2);
        List<Room> allG3Rooms = roomDao.getRoomsByBuildingId(3);
        int total = allG1Rooms.size() + allG2Rooms.size() + allG3Rooms.size();
        Assertions.assertEquals(43, total, "Expected exactly 43 real rooms in seed");

        // 5. Verify Kampus 2 Lantai 3 has 13 rooms matching the blueprint (R. 1 to R. 13)
        List<Room> k2l3Rooms = roomDao.getRoomsByBuildingAndFloor(3, 3);
        Assertions.assertEquals(13, k2l3Rooms.size(), "Kampus 2 Lantai 3 must have 13 rooms");

        // Verify R. 11 is properly positioned on canvas (x=745, y=293, w=150, h=180)
        Room r11 = null;
        for (Room r : k2l3Rooms) {
            if ("R. 11".equals(r.getCode())) {
                r11 = r;
                break;
            }
        }
        Assertions.assertNotNull(r11, "Room R. 11 must exist");
        Assertions.assertEquals(745, r11.getPosX(), "R. 11 pos_x should be 745");
        Assertions.assertEquals(293, r11.getPosY(), "R. 11 pos_y should be 293");
        Assertions.assertEquals(150, r11.getWidth(), "R. 11 width should be 150");
        Assertions.assertEquals(180, r11.getHeight(), "R. 11 height should be 180");
    }

    @Test
    public void testFloorPlanCanvasScalingAndDimensions() {
        InteractiveFloorPlanPanel canvas = new InteractiveFloorPlanPanel();
        canvas.loadFloorImage("images/kampus2_lantai_3.png");

        BufferedImage img = canvas.getFloorImage();
        Assertions.assertNotNull(img, "Blueprint image should be loaded");
        Assertions.assertEquals(1250, img.getWidth(), "Image width should be 1250");
        Assertions.assertEquals(626, img.getHeight(), "Image height should be 626");

        // Load rooms
        RoomDao roomDao = new RoomDao();
        List<Room> rooms = roomDao.getRoomsByBuildingAndFloor(3, 3);
        canvas.setRooms(rooms);

        // Canvas preferred size should match or accommodate image without distortion
        Dimension pref = canvas.getPreferredSize();
        Assertions.assertTrue(pref.width >= 1250, "Preferred width must be at least 1250");
        Assertions.assertTrue(pref.height >= 626, "Preferred height must be at least 626");

        // Verify paintComponent renders without exceptions at various sizes
        BufferedImage out1 = new BufferedImage(850, 600, BufferedImage.TYPE_INT_ARGB);
        canvas.setSize(850, 600);
        canvas.paint(out1.createGraphics());

        BufferedImage out2 = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_ARGB);
        canvas.setSize(1600, 900);
        canvas.paint(out2.createGraphics());
    }

    @Test
    public void testLocationSynchronizationBetweenEditorAndMainFrame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuthService.getInstance().login("admin", "123");
            MainFrame frame = new MainFrame();
            frame.pack();

            // Set location to Kampus 2 (id=2), Gedung Kampus 2 (id=3), Lantai 3
            frame.setSelectedLocation(2, 3, 3);
            Assertions.assertEquals(2, frame.getSelectedCampus().getId());
            Assertions.assertEquals(3, frame.getSelectedBuilding().getId());
            Assertions.assertEquals(3, frame.getSelectedFloor());

            // Switch to Layout Editor view -> location should sync to Editor
            frame.switchToView(SidebarNavigationPanel.NAV_LAYOUT_EDITOR);

            // Switch back to Floor Plan view -> location should remain Kampus 2 Lantai 3
            frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
            Assertions.assertEquals(2, frame.getSelectedCampus().getId());
            Assertions.assertEquals(3, frame.getSelectedBuilding().getId());
            Assertions.assertEquals(3, frame.getSelectedFloor());

            frame.dispose();
        });
    }
}
