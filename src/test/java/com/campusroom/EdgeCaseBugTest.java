package com.campusroom;

import com.campusroom.dao.*;
import com.campusroom.model.*;
import com.campusroom.service.*;
import java.time.LocalDate;
import java.util.List;

public class EdgeCaseBugTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("          RUNNING EDGE CASE BUG TESTS            ");
        System.out.println("=================================================");

        DatabaseHelper.initializeDatabase();
        RoomDao roomDao = new RoomDao();
        AvailabilityService availService = new AvailabilityService();

        // TEST 1: Check Catalog Room Availability issue
        System.out.println("\n--- Test 1: Catalog Room Availability ---");
        LocalDate monday = LocalDate.now();
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }

        // Suppose we get all rooms for building 1 (as MainFrame line 562 currently does):
        List<Room> rawCatalogRooms = roomDao.getRoomsByBuildingId(1);
        int occupiedCountBeforeService = 0;
        for (Room r : rawCatalogRooms) {
            if (!r.isAvailable()) occupiedCountBeforeService++;
        }
        System.out.println("Occupied rooms in raw getRoomsByBuildingId before updateRoomsAvailability: " + occupiedCountBeforeService);
        if (occupiedCountBeforeService == 0) {
            System.out.println("[CONFIRMED BUG] Raw rooms from roomDao have isAvailable=true for all rooms! If not passed to availabilityService, Catalog will falsely show all rooms as available!");
        }

        // Now test after calling updateRoomsAvailability:
        availService.updateRoomsAvailability(rawCatalogRooms, monday, "08:00", "10:30");
        int occupiedCountAfterService = 0;
        for (Room r : rawCatalogRooms) {
            if (!r.isAvailable()) occupiedCountAfterService++;
        }
        System.out.println("Occupied rooms in rawCatalogRooms after updateRoomsAvailability: " + occupiedCountAfterService);
        if (occupiedCountAfterService > 0) {
            System.out.println("[FIX VERIFIED] With updateRoomsAvailability, Catalog will correctly show occupied rooms (" + occupiedCountAfterService + " occupied).");
        }

        // TEST 2: Case-insensitive login
        System.out.println("\n--- Test 2: Case-insensitive Username in Login ---");
        UserDao userDao = new UserDao();
        User uCaps = userDao.authenticate("MAHASISWA", "123");
        User uMixed = userDao.authenticate("Mahasiswa", "123");
        System.out.println("Login with MAHASISWA: " + (uCaps != null ? "SUCCESS (" + uCaps.getFullName() + ")" : "FAILED (Null)"));
        System.out.println("Login with Mahasiswa: " + (uMixed != null ? "SUCCESS (" + uMixed.getFullName() + ")" : "FAILED (Null)"));

        // TEST 3: Add new floor level to a building
        System.out.println("\n--- Test 3: Floor Plan Multi-Floor Addition ---");
        BuildingDao bDao = new BuildingDao();
        Building b2 = bDao.getBuildingById(2);
        System.out.println("Building 2 current floors: " + b2.getTotalFloors());
        
        FloorPlanDao fpDao = new FloorPlanDao();
        String fpNewFloor = fpDao.getFloorPlanImage(2, 4);
        System.out.println("Floor plan for Building 2 Floor 4 before adding: " + fpNewFloor);

        // TEST 4: Room coordinates boundary check
        System.out.println("\n--- Test 4: Room coordinates update boundary ---");
        boolean boundaryTest = roomDao.updateRoomCoordinates(1, 10, 10, 200, 160);
        System.out.println("Update coordinates to (10, 10): " + boundaryTest);
        Room r1 = roomDao.getRoomById(1);
        System.out.println("Fetched Room 1: pos_x=" + r1.getPosX() + ", pos_y=" + r1.getPosY());

        System.out.println("=================================================");
    }

    @org.junit.jupiter.api.Test
    public void testEdgeCases() {
        main(new String[]{});
    }
}
