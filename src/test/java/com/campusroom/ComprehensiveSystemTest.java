package com.campusroom;

import com.campusroom.dao.*;
import com.campusroom.model.*;
import com.campusroom.service.*;
import java.time.LocalDate;
import java.util.List;

public class ComprehensiveSystemTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING COMPREHENSIVE CAMPUSROOM SYSTEM TEST  ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // 1. Initialize Database
        try {
            DatabaseHelper.initializeDatabase();
            System.out.println("[PASS] DatabaseHelper initialization succeeded.");
            passed++;
        } catch (Exception e) {
            System.err.println("[FAIL] DatabaseHelper failed: " + e.getMessage());
            failed++;
        }

        // 2. Test User Authentication
        UserDao userDao = new UserDao();
        AuthService authService = AuthService.getInstance();

        // Test Student
        User student = userDao.authenticate("mahasiswa", "123");
        if (student != null && "MAHASISWA".equals(student.getRole())) {
            System.out.println("[PASS] Student auth succeeded: " + student.getFullName());
            passed++;
        } else {
            System.err.println("[FAIL] Student auth failed!");
            failed++;
        }

        // Test Sekdek
        User sekdek = userDao.authenticate("sekdek", "123");
        if (sekdek != null && "SEKDEK".equals(sekdek.getRole())) {
            System.out.println("[PASS] Sekdek auth succeeded: " + sekdek.getFullName());
            passed++;
        } else {
            System.err.println("[FAIL] Sekdek auth failed!");
            failed++;
        }

        // Test Admin
        User admin = userDao.authenticate("admin", "123");
        if (admin != null && "ADMIN".equals(admin.getRole())) {
            System.out.println("[PASS] Admin auth succeeded: " + admin.getFullName());
            passed++;
        } else {
            System.err.println("[FAIL] Admin auth failed!");
            failed++;
        }

        // Test Wrong Password
        User wrong = userDao.authenticate("admin", "wrong_pass");
        if (wrong == null) {
            System.out.println("[PASS] Wrong password correctly rejected.");
            passed++;
        } else {
            System.err.println("[FAIL] Wrong password accepted!");
            failed++;
        }

        // 3. Test Multi-Building & Multi-Floor Data
        RoomDao roomDao = new RoomDao();
        FloorPlanDao floorPlanDao = new FloorPlanDao();
        BuildingDao buildingDao = new BuildingDao();

        List<Building> buildings = buildingDao.getAllBuildings();
        System.out.println("Total buildings found: " + buildings.size());
        if (buildings.size() >= 3) {
            System.out.println("[PASS] Multi-building check passed (at least 3 buildings).");
            passed++;
        } else {
            System.err.println("[FAIL] Less than 3 buildings found!");
            failed++;
        }

        // Check Gedung A (buildingId=1) has rooms on Lantai 1, 2, 3, 4
        for (int f = 1; f <= 4; f++) {
            List<Room> rList = roomDao.getRoomsByBuildingAndFloor(1, f);
            String fp = floorPlanDao.getFloorPlanImage(1, f);
            System.out.println("Gedung A Lantai " + f + ": " + rList.size() + " rooms, blueprint: " + fp);
            if (!rList.isEmpty() && fp != null) {
                System.out.println("[PASS] Gedung A Lantai " + f + " has rooms and blueprint.");
                passed++;
            } else {
                System.err.println("[FAIL] Gedung A Lantai " + f + " missing rooms or blueprint!");
                failed++;
            }
        }

        // Check Gedung B (buildingId=2) floors
        for (int f = 1; f <= 3; f++) {
            List<Room> rList = roomDao.getRoomsByBuildingAndFloor(2, f);
            String fp = floorPlanDao.getFloorPlanImage(2, f);
            System.out.println("Gedung B Lantai " + f + ": " + rList.size() + " rooms, blueprint: " + fp);
            if (!rList.isEmpty() && fp != null) {
                System.out.println("[PASS] Gedung B Lantai " + f + " verified.");
                passed++;
            } else {
                System.err.println("[FAIL] Gedung B Lantai " + f + " verification failed!");
                failed++;
            }
        }

        // 4. Test Availability Calculation (Green vs Red)
        AvailabilityService availService = new AvailabilityService();
        // Today or specific Monday
        LocalDate nextMonday = LocalDate.now();
        while (nextMonday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            nextMonday = nextMonday.plusDays(1);
        }

        List<Room> g1f2Rooms = roomDao.getRoomsByBuildingAndFloor(1, 2);
        availService.updateRoomsAvailability(g1f2Rooms, nextMonday, "08:00", "10:30");

        boolean foundOccupied = false;
        boolean foundAvailable = false;
        for (Room r : g1f2Rooms) {
            System.out.println("  Room " + r.getCode() + " (" + r.getName() + "): "
                    + (r.isAvailable() ? "🟢 KOSONG" : "🔴 TERPAKAI") + " - " + r.getCurrentOccupantInfo());
            if (!r.isAvailable()) foundOccupied = true;
            if (r.isAvailable()) foundAvailable = true;
        }

        if (foundOccupied && foundAvailable) {
            System.out.println("[PASS] Availability calculated correctly with both occupied (red) and available (green) rooms.");
            passed++;
        } else {
            System.err.println("[FAIL] Availability check failed: foundOccupied=" + foundOccupied + ", foundAvailable=" + foundAvailable);
            failed++;
        }

        // 5. Test Room Coordinate Update in DAO
        if (!g1f2Rooms.isEmpty()) {
            Room target = g1f2Rooms.get(0);
            int origX = target.getPosX();
            int newX = origX + 15;
            boolean updateOk = roomDao.updateRoomCoordinates(target.getId(), newX, target.getPosY(), target.getWidth(), target.getHeight());
            Room fetched = roomDao.getRoomById(target.getId());
            if (updateOk && fetched != null && fetched.getPosX() == newX) {
                System.out.println("[PASS] updateRoomCoordinates verified.");
                passed++;
                // revert
                roomDao.updateRoomCoordinates(target.getId(), origX, target.getPosY(), target.getWidth(), target.getHeight());
            } else {
                System.err.println("[FAIL] updateRoomCoordinates failed!");
                failed++;
            }
        }

        System.out.println("=================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("=================================================");

        if (failed > 0) {
            throw new AssertionError(failed + " system assertions failed!");
        }
    }

    @org.junit.jupiter.api.Test
    public void testComprehensiveSystem() {
        main(new String[]{});
    }
}
