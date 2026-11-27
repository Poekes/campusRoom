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

    @Test
    public void testDeleteRoomProtectionForFixedSchedules() {
        // Room 1 (R. 201) memiliki jadwal reguler tetap -> harus ditolak penghapusannya
        Assertions.assertTrue(masterDataService.isRoomLocked(1), "Room 1 memiliki jadwal tetap sehingga berstatus terkunci");
        ServiceResult<Void> delLocked = masterDataService.deleteRoom(1);
        Assertions.assertFalse(delLocked.isSuccess(), "Ruangan dengan jadwal/booking fiks tidak boleh dihapus");
        Assertions.assertTrue(delLocked.getMessage().contains("FIKS") || delLocked.getMessage().contains("fiks"));

        // Tambah ruangan sementara tanpa jadwal, pastikan bisa dihapus
        Room tempRoom = new Room(1, "TMP-DEL-01", "Ruang Sementara Uji", 1, 20, "Kelas Teori", "AC", 50, 50, 120, 100);
        ServiceResult<Room> addRes = masterDataService.addRoom(tempRoom);
        Assertions.assertTrue(addRes.isSuccess());

        Room fetched = masterDataService.getRoomsByBuilding(1).stream()
                .filter(r -> "TMP-DEL-01".equals(r.getCode()))
                .findFirst().orElse(null);
        Assertions.assertNotNull(fetched);
        Assertions.assertFalse(masterDataService.isRoomLocked(fetched.getId()));

        ServiceResult<Void> delFree = masterDataService.deleteRoom(fetched.getId());
        Assertions.assertTrue(delFree.isSuccess(), "Ruangan kosong tanpa jadwal harus berhasil dihapus");
    }

    @Test
    public void testUpdateRoomPropertiesViaMasterDataService() {
        Room tempRoom = new Room(1, "TMP-EDT-01", "Ruang Lama", 2, 30, "Kelas Teori", "AC", 100, 100, 180, 140);
        ServiceResult<Room> addRes = masterDataService.addRoom(tempRoom);
        Assertions.assertTrue(addRes.isSuccess());

        Room fetched = masterDataService.getRoomsByBuilding(1).stream()
                .filter(r -> "TMP-EDT-01".equals(r.getCode()))
                .findFirst().orElse(null);
        Assertions.assertNotNull(fetched);

        // Edit atribut ruangan
        fetched.setName("Ruang Baru Diperbarui");
        fetched.setCapacity(45);
        fetched.setRoomType("Smart Classroom");
        fetched.setFacilities("Smart TV, AC, Wi-Fi 6");

        ServiceResult<Room> updRes = masterDataService.updateRoom(fetched);
        Assertions.assertTrue(updRes.isSuccess(), "Edit ruangan harus berhasil: " + updRes.getMessage());

        Room reloaded = masterDataService.getRoomById(fetched.getId());
        Assertions.assertEquals("Ruang Baru Diperbarui", reloaded.getName());
        Assertions.assertEquals(45, reloaded.getCapacity());
        Assertions.assertEquals("Smart Classroom", reloaded.getRoomType());
        Assertions.assertEquals("Smart TV, AC, Wi-Fi 6", reloaded.getFacilities());

        // Cleanup
        Assertions.assertTrue(masterDataService.deleteRoom(fetched.getId()).isSuccess());
    }

    @Test
    public void testDatabasePathResolutionForPackagedBundle() {
        String prevAppPath = System.getProperty("jpackage.app-path");
        try {
            System.clearProperty("jpackage.app-path");
            java.io.File devFile = com.campusroom.dao.DatabaseHelper.resolveDatabaseFile();
            Assertions.assertEquals("campusroom.db", devFile.getName());

            System.setProperty("jpackage.app-path", "/opt/CampusRoom/CampusRoom");
            java.io.File packagedFile = com.campusroom.dao.DatabaseHelper.resolveDatabaseFile();
            Assertions.assertEquals("campusroom.db", packagedFile.getName());
            Assertions.assertTrue(packagedFile.getAbsolutePath().contains("CampusRoom")
                    || packagedFile.getAbsolutePath().contains(".campusroom"));
        } finally {
            if (prevAppPath == null) {
                System.clearProperty("jpackage.app-path");
            } else {
                System.setProperty("jpackage.app-path", prevAppPath);
            }
        }
    }
}
