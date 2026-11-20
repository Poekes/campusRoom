package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.Room;
import com.campusroom.service.MasterDataService;
import com.campusroom.service.ServiceResult;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MasterDataServiceTest {
    private static MasterDataService masterDataService;

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initializeDatabase();
        masterDataService = new MasterDataService();
    }

    @Test
    public void testCampusOperations() {
        List<Campus> campuses = masterDataService.getAllCampuses();
        Assertions.assertNotNull(campuses);
        Assertions.assertFalse(campuses.isEmpty());

        Campus c1 = masterDataService.getCampusById(campuses.get(0).getId());
        Assertions.assertNotNull(c1);

        // Validation: Empty code / name
        ServiceResult<Campus> rInvalid = masterDataService.addCampus(new Campus("", "", "Alamat", "Kota"));
        Assertions.assertFalse(rInvalid.isSuccess());
    }

    @Test
    public void testBuildingOperations() {
        List<Building> buildings = masterDataService.getAllBuildings();
        Assertions.assertNotNull(buildings);
        Assertions.assertFalse(buildings.isEmpty());

        Building b1 = masterDataService.getBuildingById(buildings.get(0).getId());
        Assertions.assertNotNull(b1);

        List<Building> byCampus = masterDataService.getBuildingsByCampus(buildings.get(0).getCampusId());
        Assertions.assertNotNull(byCampus);
        Assertions.assertFalse(byCampus.isEmpty());
    }

    @Test
    public void testRoomOperationsAndCodeUniqueness() {
        List<Room> allRooms = masterDataService.getAllRooms();
        Assertions.assertNotNull(allRooms);
        Assertions.assertFalse(allRooms.isEmpty());

        Room r1 = masterDataService.getRoomById(1);
        Assertions.assertNotNull(r1);

        // Uji keunikan kode ruangan di gedung yang sama
        boolean unique1 = masterDataService.isRoomCodeUnique(1, r1.getId(), r1.getCode());
        Assertions.assertTrue(unique1, "Kode ruangan milik dirinya sendiri harus dianggap valid saat update");

        boolean uniqueConflict = masterDataService.isRoomCodeUnique(1, 9999, r1.getCode());
        Assertions.assertFalse(uniqueConflict, "Kode ruangan yang sudah ada di gedung harus terdeteksi konflik");

        // Coba tambah ruangan dengan kode yang sudah ada
        Room dup = new Room(1, r1.getCode(), "Duplikat Name", 1, 30, "Kelas Teori", "AC", 100, 100, 180, 140);
        ServiceResult<Room> addDupResult = masterDataService.addRoom(dup);
        Assertions.assertFalse(addDupResult.isSuccess(), "Service layer harus menolak duplikasi kode ruangan di gedung yang sama");
    }

    @Test
    public void testFloorPlanService() {
        String fp = masterDataService.getFloorPlanImage(1, 1);
        Assertions.assertNotNull(fp);
        Assertions.assertEquals("images/floorplan_gedung_a.png", fp);
    }
}
