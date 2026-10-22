package com.campusroom;

import com.campusroom.dao.*;
import com.campusroom.model.*;
import java.util.List;

public class LayoutRibbonFloorTest {

    public static void main(String[] args) {
        System.out.println("--- Testing Multi-Floor Layout and Ribbon Functions ---");
        DatabaseHelper.initializeDatabase();
        BuildingDao buildingDao = new BuildingDao();
        FloorPlanDao floorPlanDao = new FloorPlanDao();
        RoomDao roomDao = new RoomDao();

        // 1. Test adding a new floor to Gedung C (id=3)
        Building b3 = buildingDao.getBuildingById(3);
        int initialFloors = b3.getTotalFloors();
        int newFloor = initialFloors + 1;
        System.out.println("Gedung C initial floors: " + initialFloors + ", adding floor: " + newFloor);

        boolean updated = buildingDao.updateTotalFloors(3, newFloor);
        if (!updated) {
            System.err.println("[FAIL] Failed to update total floors!");
            System.exit(1);
        }

        // Set blueprint for this new floor
        boolean fpSaved = floorPlanDao.saveFloorPlanImage(3, newFloor, "images/floorplan_gedung_a.png");
        String fpFetched = floorPlanDao.getFloorPlanImage(3, newFloor);
        System.out.println("Blueprint for Gedung C Lantai " + newFloor + ": " + fpFetched);

        if (!fpSaved || !"images/floorplan_gedung_a.png".equals(fpFetched)) {
            System.err.println("[FAIL] Floor plan save/fetch failed for new floor!");
            System.exit(1);
        }

        // 2. Add a new room on this new floor
        Room newRoom = new Room(3, "C. " + newFloor + "01", "Ruang Riset Kolaboratif", newFloor, 35,
                "Smart Classroom", "Proyektor 4K, Wi-Fi 6, AC", 100, 100, 180, 150);
        boolean roomAdded = roomDao.addRoom(newRoom);
        if (!roomAdded) {
            System.err.println("[FAIL] Failed to add room on new floor!");
            System.exit(1);
        }

        List<Room> roomsOnNewFloor = roomDao.getRoomsByBuildingAndFloor(3, newFloor);
        System.out.println("Rooms on Gedung C Lantai " + newFloor + ": " + roomsOnNewFloor.size());
        if (roomsOnNewFloor.isEmpty()) {
            System.err.println("[FAIL] No rooms found on new floor!");
            System.exit(1);
        }

        // 3. Test Room Duplication on new floor
        Room source = roomsOnNewFloor.get(0);
        Room dup = new Room(3, source.getCode() + "-COPY", source.getName() + " (Salinan)",
                newFloor, source.getCapacity(), source.getRoomType(), source.getFacilities(),
                source.getPosX() + 40, source.getPosY() + 40, source.getWidth(), source.getHeight());
        boolean dupAdded = roomDao.addRoom(dup);
        if (!dupAdded) {
            System.err.println("[FAIL] Duplicate room failed!");
            System.exit(1);
        }
        List<Room> roomsAfterDup = roomDao.getRoomsByBuildingAndFloor(3, newFloor);
        System.out.println("Rooms after duplicate: " + roomsAfterDup.size());
        if (roomsAfterDup.size() != 2) {
            System.err.println("[FAIL] Expected 2 rooms after duplicate!");
            System.exit(1);
        }

        // 4. Test updating coordinates of duplicated room
        Room dupRoom = roomsAfterDup.get(1);
        boolean coordsUpdated = roomDao.updateRoomCoordinates(dupRoom.getId(), 320, 100, 200, 160);
        Room fetchedDup = roomDao.getRoomById(dupRoom.getId());
        if (!coordsUpdated || fetchedDup.getPosX() != 320 || fetchedDup.getWidth() != 200) {
            throw new AssertionError("Coordinates update failed!");
        }

        // Clean up test data for new floor to leave database clean
        for (Room r : roomsAfterDup) {
            roomDao.deleteRoom(r.getId());
        }
        buildingDao.updateTotalFloors(3, initialFloors);

        System.out.println("[PASS] All Multi-Floor Layout and Ribbon functions passed with 100% success!");
    }

    @org.junit.jupiter.api.Test
    public void testLayoutRibbonFloor() {
        main(new String[]{});
    }
}
