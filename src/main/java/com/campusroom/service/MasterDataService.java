package com.campusroom.service;

import com.campusroom.dao.BookingDao;
import com.campusroom.dao.BuildingDao;
import com.campusroom.dao.CampusDao;
import com.campusroom.dao.FloorPlanDao;
import com.campusroom.dao.RoomDao;
import com.campusroom.dao.ScheduleDao;
import com.campusroom.model.Booking;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import java.util.List;

/**
 * MasterDataService - Layanan proses bisnis untuk master data kampus, gedung, ruangan, dan denah blueprint.
 * Menjamin integritas referensial dan validasi keunikan kode entitas.
 */
public class MasterDataService {
    private final CampusDao campusDao;
    private final BuildingDao buildingDao;
    private final RoomDao roomDao;
    private final FloorPlanDao floorPlanDao;
    private final ScheduleDao scheduleDao;
    private final BookingDao bookingDao;

    public MasterDataService() {
        this.campusDao = new CampusDao();
        this.buildingDao = new BuildingDao();
        this.roomDao = new RoomDao();
        this.floorPlanDao = new FloorPlanDao();
        this.scheduleDao = new ScheduleDao();
        this.bookingDao = new BookingDao();
    }

    public MasterDataService(CampusDao campusDao, BuildingDao buildingDao, RoomDao roomDao, FloorPlanDao floorPlanDao) {
        this.campusDao = campusDao;
        this.buildingDao = buildingDao;
        this.roomDao = roomDao;
        this.floorPlanDao = floorPlanDao;
        this.scheduleDao = new ScheduleDao();
        this.bookingDao = new BookingDao();
    }

    // ==========================================
    // KAMPUS (CAMPUS)
    // ==========================================

    public List<Campus> getAllCampuses() {
        return campusDao.getAllCampuses();
    }

    public Campus getCampusById(int id) {
        return campusDao.getCampusById(id);
    }

    public ServiceResult<Campus> addCampus(Campus campus) {
        if (campus == null) {
            return ServiceResult.failure("Data kampus tidak boleh kosong!");
        }
        if (campus.getCode() == null || campus.getCode().trim().isEmpty()) {
            return ServiceResult.failure("Kode kampus tidak boleh kosong!");
        }
        if (campus.getName() == null || campus.getName().trim().isEmpty()) {
            return ServiceResult.failure("Nama kampus tidak boleh kosong!");
        }

        boolean ok = campusDao.addCampus(campus);
        if (ok) {
            return ServiceResult.success("Kampus baru berhasil ditambahkan!", campus);
        } else {
            return ServiceResult.failure("Gagal menambahkan kampus ke database.");
        }
    }

    public ServiceResult<Campus> updateCampus(Campus campus) {
        if (campus == null || campus.getId() <= 0) {
            return ServiceResult.failure("ID kampus tidak valid!");
        }
        boolean ok = campusDao.updateCampus(campus);
        if (ok) {
            return ServiceResult.success("Data kampus berhasil diperbarui!", campus);
        } else {
            return ServiceResult.failure("Gagal memperbarui data kampus.");
        }
    }

    public ServiceResult<Void> deleteCampus(int id) {
        if (id <= 0) {
            return ServiceResult.failure("ID kampus tidak valid!");
        }
        boolean ok = campusDao.deleteCampus(id);
        if (ok) {
            return ServiceResult.success("Kampus berhasil dihapus.");
        } else {
            return ServiceResult.failure("Gagal menghapus kampus.");
        }
    }

    // ==========================================
    // GEDUNG (BUILDING)
    // ==========================================

    public List<Building> getAllBuildings() {
        return buildingDao.getAllBuildings();
    }

    public List<Building> getBuildingsByCampus(int campusId) {
        return buildingDao.getBuildingsByCampusId(campusId);
    }

    public Building getBuildingById(int id) {
        return buildingDao.getBuildingById(id);
    }

    public ServiceResult<Building> addBuilding(Building building) {
        if (building == null) {
            return ServiceResult.failure("Data gedung tidak boleh kosong!");
        }
        if (building.getCode() == null || building.getCode().trim().isEmpty()) {
            return ServiceResult.failure("Kode gedung tidak boleh kosong!");
        }
        if (building.getName() == null || building.getName().trim().isEmpty()) {
            return ServiceResult.failure("Nama gedung tidak boleh kosong!");
        }
        if (building.getTotalFloors() <= 0) {
            return ServiceResult.failure("Jumlah lantai harus minimal 1!");
        }

        boolean ok = buildingDao.addBuilding(building);
        if (ok) {
            return ServiceResult.success("Gedung baru berhasil disimpan!", building);
        } else {
            return ServiceResult.failure("Gagal menyimpan gedung baru ke database.");
        }
    }

    public ServiceResult<Void> updateTotalFloors(int buildingId, int totalFloors) {
        if (buildingId <= 0 || totalFloors <= 0) {
            return ServiceResult.failure("Nomor lantai harus lebih besar dari 0!");
        }

        boolean ok = buildingDao.updateTotalFloors(buildingId, totalFloors);
        if (ok) {
            return ServiceResult.success("Jumlah lantai gedung berhasil diperbarui.");
        } else {
            return ServiceResult.failure("Gagal memperbarui jumlah lantai gedung.");
        }
    }

    public ServiceResult<Void> updateBuildingFloorPlan(int buildingId, String imagePath) {
        boolean ok = buildingDao.updateFloorPlanImage(buildingId, imagePath);
        if (ok) {
            return ServiceResult.success("Blueprint gedung berhasil diperbarui.");
        } else {
            return ServiceResult.failure("Gagal memperbarui blueprint gedung.");
        }
    }

    // ==========================================
    // RUANGAN (ROOM)
    // ==========================================

    public List<Room> getAllRooms() {
        return roomDao.getAllRooms();
    }

    public List<Room> getRoomsByBuilding(int buildingId) {
        return roomDao.getRoomsByBuildingId(buildingId);
    }

    public List<Room> getRoomsByBuildingAndFloor(int buildingId, int floor) {
        return roomDao.getRoomsByBuildingAndFloor(buildingId, floor);
    }

    public List<Integer> getDistinctFloorsByBuilding(int buildingId) {
        return roomDao.getDistinctFloorsByBuilding(buildingId);
    }

    public Room getRoomById(int id) {
        return roomDao.getRoomById(id);
    }

    public boolean isRoomCodeUnique(int buildingId, int roomIdToExclude, String code) {
        if (code == null || code.trim().isEmpty()) return false;
        List<Room> existing = roomDao.getRoomsByBuildingId(buildingId);
        for (Room r : existing) {
            if (r.getId() != roomIdToExclude && r.getCode().equalsIgnoreCase(code.trim())) {
                return false;
            }
        }
        return true;
    }

    public ServiceResult<Room> addRoom(Room room) {
        if (room == null) {
            return ServiceResult.failure("Data ruangan tidak boleh kosong!");
        }
        if (room.getCode() == null || room.getCode().trim().isEmpty()) {
            return ServiceResult.failure("Kode ruangan tidak boleh kosong!");
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return ServiceResult.failure("Nama ruangan tidak boleh kosong!");
        }
        if (room.getCapacity() <= 0) {
            return ServiceResult.failure("Kapasitas ruangan harus lebih besar dari 0!");
        }

        if (!isRoomCodeUnique(room.getBuildingId(), 0, room.getCode())) {
            return ServiceResult.failure("Kode ruangan '" + room.getCode() + "' sudah digunakan oleh ruangan lain di gedung ini!");
        }

        boolean ok = roomDao.addRoom(room);
        if (ok) {
            return ServiceResult.success("Ruangan " + room.getCode() + " berhasil ditambahkan!", room);
        } else {
            return ServiceResult.failure("Gagal menambahkan ruangan ke database.");
        }
    }

    public ServiceResult<Room> updateRoom(Room room) {
        if (room == null || room.getId() <= 0) {
            return ServiceResult.failure("ID ruangan tidak valid!");
        }
        if (room.getCode() == null || room.getCode().trim().isEmpty()) {
            return ServiceResult.failure("Kode ruangan tidak boleh kosong!");
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return ServiceResult.failure("Nama ruangan tidak boleh kosong!");
        }
        if (room.getCapacity() <= 0) {
            return ServiceResult.failure("Kapasitas ruangan harus lebih besar dari 0!");
        }

        if (!isRoomCodeUnique(room.getBuildingId(), room.getId(), room.getCode())) {
            return ServiceResult.failure("Kode ruangan '" + room.getCode() + "' sudah digunakan oleh ruangan lain di gedung ini!");
        }

        boolean ok = roomDao.updateRoom(room);
        if (ok) {
            return ServiceResult.success("Data ruangan " + room.getCode() + " berhasil disimpan!", room);
        } else {
            return ServiceResult.failure("Gagal memperbarui data ruangan di database.");
        }
    }

    public ServiceResult<Void> updateRoomCoordinates(int roomId, int posX, int posY, int width, int height) {
        if (roomId <= 0) {
            return ServiceResult.failure("ID ruangan tidak valid!");
        }
        if (width <= 0 || height <= 0) {
            return ServiceResult.failure("Dimensi ukuran ruangan (width/height) harus lebih besar dari 0!");
        }

        boolean ok = roomDao.updateRoomCoordinates(roomId, posX, posY, width, height);
        if (ok) {
            return ServiceResult.success("Koordinat ruangan berhasil diperbarui.");
        } else {
            return ServiceResult.failure("Gagal memperbarui koordinat ruangan.");
        }
    }

    public boolean isRoomLocked(int roomId) {
        if (roomId <= 0) return false;
        List<RegularSchedule> schedules = scheduleDao.getSchedulesByRoomId(roomId);
        List<Booking> activeBookings = bookingDao.getActiveBookingsByRoom(roomId);
        return !schedules.isEmpty() || !activeBookings.isEmpty();
    }

    public ServiceResult<Void> deleteRoom(int id) {
        if (id <= 0) {
            return ServiceResult.failure("ID ruangan tidak valid!");
        }

        // Proteksi integritas data: ruangan yang sudah memiliki jadwal tetap atau booking fiks/aktif tidak bisa dihapus
        List<RegularSchedule> schedules = scheduleDao.getSchedulesByRoomId(id);
        List<Booking> activeBookings = bookingDao.getActiveBookingsByRoom(id);
        if (!schedules.isEmpty() || !activeBookings.isEmpty()) {
            return ServiceResult.failure(
                    "Ruangan TIDAK DAPAT DIHAPUS karena memiliki jadwal/booking aktif yang sudah fiks!\n" +
                    "• Jadwal perkuliahan tetap: " + schedules.size() + "\n" +
                    "• Booking aktif / disetujui (FIKS): " + activeBookings.size() + "\n\n" +
                    "Hapus atau pindahkan jadwal terkait terlebih dahulu."
            );
        }

        boolean ok = roomDao.deleteRoom(id);
        if (ok) {
            return ServiceResult.success("Ruangan berhasil dihapus.");
        } else {
            return ServiceResult.failure("Gagal menghapus ruangan dari database.");
        }
    }

    // ==========================================
    // DENAH BLUEPRINT (FLOOR PLAN)
    // ==========================================

    public String getFloorPlanImage(int buildingId, int floor) {
        return floorPlanDao.getFloorPlanImage(buildingId, floor);
    }

    public ServiceResult<Void> saveFloorPlanImage(int buildingId, int floor, String imagePath) {
        if (buildingId <= 0 || floor <= 0) {
            return ServiceResult.failure("Parameter gedung atau lantai tidak valid!");
        }

        boolean ok = floorPlanDao.saveFloorPlanImage(buildingId, floor, imagePath);
        if (ok) {
            return ServiceResult.success("Gambar denah blueprint berhasil disimpan.");
        } else {
            return ServiceResult.failure("Gagal menyimpan gambar denah blueprint ke database.");
        }
    }
}
