package com.campusroom;

import com.campusroom.dao.*;
import com.campusroom.model.*;
import com.campusroom.service.AuthService;
import com.campusroom.ui.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
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
    public void testFloorPlanRotation() {
        InteractiveFloorPlanPanel canvas = new InteractiveFloorPlanPanel();
        canvas.loadFloorImage("images/kampus2_lantai_3.png");

        RoomDao roomDao = new RoomDao();
        List<Room> rooms = roomDao.getRoomsByBuildingAndFloor(3, 3);
        canvas.setRooms(rooms);

        // 1. Initial rotation should be 0
        Assertions.assertEquals(0, canvas.getRotationDegrees());

        Dimension d0 = canvas.getPreferredSize();
        Assertions.assertTrue(d0.width >= 1250);
        Assertions.assertTrue(d0.height >= 626);

        // 2. Rotate clockwise to 90 degrees
        canvas.rotateClockwise();
        Assertions.assertEquals(90, canvas.getRotationDegrees());
        Dimension d90 = canvas.getPreferredSize();
        // Width and height should be swapped (height >= 1250, width >= 626)
        Assertions.assertEquals(d0.width, d90.height);
        Assertions.assertEquals(d0.height, d90.width);

        // 3. Coordinate conversion at 90 degrees
        // Logical room R.11 at (745, 293)
        // At 90 deg: screenX = H - ly, screenY = lx
        int logicalH = d0.height;
        int expectedScreenX = logicalH - 293;
        int expectedScreenY = 745;
        java.awt.Point pLog = canvas.screenToLogical(expectedScreenX, expectedScreenY);
        Assertions.assertEquals(745, pLog.x, "Logical X should be 745");
        Assertions.assertEquals(293, pLog.y, "Logical Y should be 293");

        // 4. Rotate to 180 and 270 degrees
        canvas.rotateClockwise();
        Assertions.assertEquals(180, canvas.getRotationDegrees());
        Dimension d180 = canvas.getPreferredSize();
        Assertions.assertEquals(d0.width, d180.width);
        Assertions.assertEquals(d0.height, d180.height);

        canvas.rotateClockwise();
        Assertions.assertEquals(270, canvas.getRotationDegrees());
        Dimension d270 = canvas.getPreferredSize();
        Assertions.assertEquals(d0.width, d270.height);
        Assertions.assertEquals(d0.height, d270.width);

        // 5. Reset rotation
        canvas.resetRotation();
        Assertions.assertEquals(0, canvas.getRotationDegrees());

        // 6. Test rotateCounterClockwise from 0 -> 270
        canvas.rotateCounterClockwise();
        Assertions.assertEquals(270, canvas.getRotationDegrees());

        // 7. Verify paintComponent renders without exceptions at 90, 180, 270 degrees
        for (int deg : new int[]{90, 180, 270}) {
            canvas.setRotationDegrees(deg);
            BufferedImage out = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
            canvas.setSize(1000, 800);
            canvas.paint(out.createGraphics());
        }
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

    @Test
    public void testFloorPlanSearchHighlightAndInstantTableSearch() {
        // 1. Uji highlight dan screen bounds ruangan pada canvas
        InteractiveFloorPlanPanel canvas = new InteractiveFloorPlanPanel();
        canvas.loadFloorImage("images/kampus2_lantai_3.png");
        RoomDao roomDao = new RoomDao();
        List<Room> rooms = roomDao.getRoomsByBuildingAndFloor(3, 3);
        canvas.setRooms(rooms);

        Room target = rooms.get(0);
        canvas.setSearchHighlightRoom(target);
        Assertions.assertEquals(target, canvas.getSearchHighlightRoom());

        java.awt.Rectangle bounds0 = canvas.getScreenBoundsForRoom(target);
        Assertions.assertNotNull(bounds0);
        Assertions.assertEquals(target.getPosX(), bounds0.x);
        Assertions.assertEquals(target.getPosY(), bounds0.y);

        canvas.setRotationDegrees(90);
        java.awt.Rectangle bounds90 = canvas.getScreenBoundsForRoom(target);
        Assertions.assertNotNull(bounds90);
        Assertions.assertEquals(target.getWidth(), bounds90.height);

        BufferedImage out = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
        canvas.setSize(1000, 800);
        canvas.paint(out.createGraphics());

        // 2. Uji ModernUIHelper.attachInstantTableSearch (case-insensitive & special regex chars)
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new String[]{"Kode", "Nama", "Status"}, 0);
        model.addRow(new Object[]{"R. 201", "Ruang Teori (Utama)", "Tersedia"});
        model.addRow(new Object[]{"BK-2026-101", "Lab Jaringan", "Menunggu"});
        javax.swing.JTable tbl = new javax.swing.JTable(model);
        javax.swing.JTextField txtSearch = new javax.swing.JTextField();
        ModernUIHelper.attachInstantTableSearch(txtSearch, tbl, 0, 1, 2);

        Assertions.assertEquals(2, tbl.getRowCount());

        // Pencarian dengan karakter spesial regex: "." dan "()"
        txtSearch.setText("r. 201");
        Assertions.assertEquals(1, tbl.getRowCount());

        txtSearch.setText("(Utama)");
        Assertions.assertEquals(1, tbl.getRowCount());

        txtSearch.setText("BK-2026");
        Assertions.assertEquals(1, tbl.getRowCount());

        // Kosongkan teks -> semua baris kembali tampil
        txtSearch.setText("");
        Assertions.assertEquals(2, tbl.getRowCount());
    }

    @Test
    public void testMenuNavigationPreservesUIState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuthService.getInstance().login("admin", "123");
            MainFrame frame = new MainFrame();
            frame.pack();

            // 1. Pilih lokasi Kampus 2 Lantai 3, atur zoom dan rotasi, lalu pilih ruangan
            frame.setSelectedLocation(2, 3, 3);
            InteractiveFloorPlanPanel canvas = frame.getFloorCanvas();
            canvas.setZoomLevel(1.30);
            canvas.setRotationDegrees(90);

            Room r11 = new RoomDao().getRoomsByBuildingAndFloor(3, 3).get(0);
            canvas.setSelectedRoom(r11);
            frame.onRoomSelected(r11);

            // 2. Pindah ke menu lain (Katalog -> Pengajuan -> Admin Rooms -> kembali ke Denah)
            frame.switchToView(SidebarNavigationPanel.NAV_CATALOG);
            frame.switchToView(SidebarNavigationPanel.NAV_BOOKING);
            frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_ROOMS);
            frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);

            // 3. Pastikan seluruh state denah tidak ter-reset
            Assertions.assertEquals(2, frame.getSelectedCampus().getId());
            Assertions.assertEquals(3, frame.getSelectedBuilding().getId());
            Assertions.assertEquals(3, frame.getSelectedFloor());
            Assertions.assertEquals(1.30, canvas.getZoomLevel(), 0.001);
            Assertions.assertEquals(90, canvas.getRotationDegrees());
            Assertions.assertNotNull(frame.getSelectedRoom());
            Assertions.assertEquals(r11.getId(), frame.getSelectedRoom().getId());
            Assertions.assertNotNull(canvas.getSelectedRoom());
            Assertions.assertEquals(r11.getId(), canvas.getSelectedRoom().getId());

            frame.dispose();
        });
    }

    @Test
    public void testAdaptiveRoomTextLODRendering() {
        InteractiveFloorPlanPanel canvas = new InteractiveFloorPlanPanel();

        Room compactRoom = new Room(1, "R.KCL", "Ruang Kecil Sekali Nama Panjang", 1, 15, "Kelas Teori", "AC", 20, 20, 80, 55);
        Room narrowRoom = new Room(1, "R.NRW", "Ruang Sempit Tinggi Nama Panjang Sekali", 1, 20, "Laboratorium", "AC", 120, 20, 88, 166);
        Room mediumRoom = new Room(1, "R.MED", "Ruang Sedang Nama Panjang Banget", 1, 30, "Smart Classroom", "AC", 230, 20, 130, 90);
        Room fullRoom = new Room(1, "R.FUL", "Ruang Besar Standar Nama Sangat Panjang", 1, 45, "Auditorium", "Proyektor", 380, 20, 180, 140);
        fullRoom.setAvailable(false);

        Assertions.assertEquals(InteractiveFloorPlanPanel.RoomRenderTier.COMPACT, canvas.determineRenderTier(compactRoom));
        Assertions.assertEquals(InteractiveFloorPlanPanel.RoomRenderTier.COMPACT, canvas.determineRenderTier(narrowRoom));
        Assertions.assertEquals(InteractiveFloorPlanPanel.RoomRenderTier.MEDIUM, canvas.determineRenderTier(mediumRoom));
        Assertions.assertEquals(InteractiveFloorPlanPanel.RoomRenderTier.FULL, canvas.determineRenderTier(fullRoom));

        canvas.setRooms(java.util.Arrays.asList(compactRoom, narrowRoom, mediumRoom, fullRoom));
        canvas.setLayoutEditMode(true);
        canvas.setSelectedRoom(compactRoom);

        BufferedImage out = new BufferedImage(700, 300, BufferedImage.TYPE_INT_ARGB);
        canvas.setSize(700, 300);
        canvas.paint(out.createGraphics());
    }

    @Test
    public void testRightSidebarScrollableScheduleFeed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AuthService.getInstance().login("admin", "123");
            MainFrame frame = new MainFrame();
            frame.pack();

            JScrollPane scrollPane = frame.getSidebarScheduleScrollPane();
            Assertions.assertNotNull(scrollPane, "Scroll pane jadwal pada sidebar kanan harus tersedia");
            Assertions.assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, scrollPane.getVerticalScrollBarPolicy());
            Assertions.assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, scrollPane.getHorizontalScrollBarPolicy());

            // Pilih ruangan nyata dari database yang memiliki jadwal tetap (Room ID 1 / R. 201 atau ruangan berjadwal pertama)
            RoomDao roomDao = new RoomDao();
            List<Room> allRooms = roomDao.getAllRooms();
            Assertions.assertFalse(allRooms.isEmpty());
            Room roomWithSchedules = allRooms.get(0);

            frame.getSidebarScheduleScopeCombo().setSelectedIndex(1); // "Semua Hari"
            frame.onRoomSelected(roomWithSchedules);
            Assertions.assertFalse(frame.isInspectorCollapsed(), "Sidebar kanan harus terbuka otomatis saat ruangan dipilih");
            Assertions.assertTrue(frame.getSidebarAgendaTitleText().contains("SEMUA JADWAL"),
                    "Header agenda harus menampilkan mode SEMUA JADWAL");
            Assertions.assertTrue(frame.getSidebarScheduleCardCount() >= 0);

            // Jika ada ruangan yang punya jadwal tetap di DB, pastikan card count > 0
            com.campusroom.service.ScheduleService scheduleService = new com.campusroom.service.ScheduleService();
            for (Room r : allRooms) {
                if (!scheduleService.getSchedulesByRoomId(r.getId()).isEmpty()) {
                    frame.onRoomSelected(r);
                    Assertions.assertTrue(frame.getSidebarScheduleCardCount() >= 1,
                            "Ruangan berjadwal (" + r.getCode() + ") harus menampilkan minimal 1 kartu jadwal di sidebar");
                    break;
                }
            }

            // Ganti ke filter "Tanggal Ini"
            frame.getSidebarScheduleScopeCombo().setSelectedIndex(0);
            Assertions.assertTrue(frame.getSidebarAgendaTitleText().contains("JADWAL "),
                    "Header agenda harus menyesuaikan nama hari tanggal terpilih");

            // Uji empty state dengan ruangan dummy tanpa jadwal
            Room emptyRoom = new Room();
            emptyRoom.setId(-999);
            emptyRoom.setCode("TEST-KOSONG");
            emptyRoom.setName("Ruang Uji Kosong");
            emptyRoom.setCapacity(30);
            emptyRoom.setFloor(1);
            emptyRoom.setRoomType("Teori");
            emptyRoom.setFacilities("Proyektor");
            emptyRoom.setAvailable(true);
            emptyRoom.setCurrentOccupantInfo("Tersedia");
            frame.onRoomSelected(emptyRoom);
            Assertions.assertEquals(0, frame.getSidebarScheduleCardCount(),
                    "Ruangan tanpa jadwal harus menampilkan empty state (0 kartu jadwal)");

            // Reset seleksi ruangan
            frame.onRoomSelected(null);
            Assertions.assertEquals(0, frame.getSidebarScheduleCardCount());

            frame.dispose();
        });
    }
}
