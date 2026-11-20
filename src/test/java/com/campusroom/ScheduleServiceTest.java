package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.model.RegularSchedule;
import com.campusroom.service.ScheduleService;
import com.campusroom.service.ServiceResult;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ScheduleServiceTest {
    private static ScheduleService scheduleService;

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initializeDatabase();
        scheduleService = new ScheduleService();
    }

    @Test
    public void testGetAllSchedules() {
        List<RegularSchedule> list = scheduleService.getAllSchedules();
        Assertions.assertNotNull(list);
        Assertions.assertFalse(list.isEmpty());
    }

    @Test
    public void testAddScheduleValidation() {
        // Test 1: Null schedule
        ServiceResult<RegularSchedule> r1 = scheduleService.addSchedule(null);
        Assertions.assertFalse(r1.isSuccess());

        // Test 2: Invalid room
        RegularSchedule s2 = new RegularSchedule(0, "Senin", "08:00", "10:00", "MK-01", "Matkul", "Dosen", "Kelas");
        ServiceResult<RegularSchedule> r2 = scheduleService.addSchedule(s2);
        Assertions.assertFalse(r2.isSuccess());

        // Test 3: Invalid time order (start >= end)
        RegularSchedule s3 = new RegularSchedule(1, "Senin", "10:00", "08:00", "MK-01", "Matkul", "Dosen", "Kelas");
        ServiceResult<RegularSchedule> r3 = scheduleService.addSchedule(s3);
        Assertions.assertFalse(r3.isSuccess());
    }

    @Test
    public void testScheduleAddConflictAndLifecycle() {
        // Ambil jadwal yang sudah ada di database untuk Room 1 pada hari Senin
        List<RegularSchedule> existing = scheduleService.getSchedulesByRoomAndDay(1, "Senin");
        Assertions.assertFalse(existing.isEmpty(), "Harus ada jadwal tetap bawaan di Room 1");
        RegularSchedule existingItem = existing.get(0);

        // Coba masukkan jadwal baru yang bertabrakan dengan jadwal existing tersebut
        RegularSchedule clashing = new RegularSchedule(
                1, "Senin", existingItem.getStartTime(), existingItem.getEndTime(),
                "MK-CLASH", "Matkul Bentrok", "Dosen Bentrok", "Kelas-X"
        );
        ServiceResult<RegularSchedule> clashResult = scheduleService.addSchedule(clashing);
        Assertions.assertFalse(clashResult.isSuccess(), "Service layer harus menolak jadwal bentrok!");
        Assertions.assertTrue(clashResult.getMessage().contains("Bentrokan terdeteksi"));

        // Masukkan jadwal pada slot yang aman (contoh: Sabtu malam 19:00 - 21:00)
        RegularSchedule validSchedule = new RegularSchedule(
                1, "Sabtu", "19:00", "21:00",
                "MK-TEST-SEC", "Pemrograman Layanan", "Dr. Guru", "TI-3X"
        );
        ServiceResult<RegularSchedule> addResult = scheduleService.addSchedule(validSchedule);
        Assertions.assertTrue(addResult.isSuccess(), "Jadwal tanpa bentrokan harus sukses: " + addResult.getMessage());

        // Temukan ID jadwal yang baru ditambahkan
        List<RegularSchedule> afterAdd = scheduleService.getSchedulesByRoomAndDay(1, "Sabtu");
        RegularSchedule addedTarget = null;
        for (RegularSchedule s : afterAdd) {
            if ("MK-TEST-SEC".equals(s.getCourseCode())) {
                addedTarget = s;
                break;
            }
        }
        Assertions.assertNotNull(addedTarget);

        // Hapus kembali jadwal uji coba
        ServiceResult<Void> delResult = scheduleService.deleteSchedule(addedTarget.getId());
        Assertions.assertTrue(delResult.isSuccess());
    }
}
