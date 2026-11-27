package com.campusroom.service;

import com.campusroom.dao.ScheduleDao;
import com.campusroom.model.RegularSchedule;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ScheduleService - Layanan proses bisnis untuk master jadwal perkuliahan tetap semester (Dekanat).
 * Menangani validasi jadwal, pencegahan tumpang tindih slot jam kuliah di ruangan yang sama,
 * serta penghapusan jadwal.
 */
public class ScheduleService {
    private final ScheduleDao scheduleDao;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ScheduleService() {
        this.scheduleDao = new ScheduleDao();
    }

    public ScheduleService(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    /**
     * Mengambil semua jadwal tetap perkuliahan
     */
    public List<RegularSchedule> getAllSchedules() {
        return scheduleDao.getAllSchedules();
    }

    /**
     * Mengambil jadwal perkuliahan pada ruangan dan hari tertentu
     */
    public List<RegularSchedule> getSchedulesByRoomAndDay(int roomId, String dayOfWeek) {
        return scheduleDao.getSchedulesByRoomAndDay(roomId, dayOfWeek);
    }

    /**
     * Mengambil seluruh jadwal perkuliahan untuk satu ruangan tertentu
     */
    public List<RegularSchedule> getSchedulesByRoomId(int roomId) {
        if (roomId <= 0) return java.util.Collections.emptyList();
        return scheduleDao.getSchedulesByRoomId(roomId);
    }

    /**
     * Mengambil daftar seluruh nama unik dosen pengampu terdaftar
     */
    public List<String> getAllLecturers() {
        return scheduleDao.getAllUniqueLecturers();
    }

    /**
     * Mengambil seluruh jadwal perkuliahan rutin yang diampu oleh dosen tertentu
     */
    public List<RegularSchedule> getSchedulesByLecturer(String lecturerName) {
        if (lecturerName == null || lecturerName.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return scheduleDao.getSchedulesByLecturer(lecturerName.trim());
    }

    public static class LecturerSummary {
        private final String lecturerName;
        private final int totalSessions;
        private final int totalMinutes;
        private final java.util.Set<String> activeDays;
        private final java.util.Set<String> uniqueCourses;
        private final boolean hasConflict;

        public LecturerSummary(String lecturerName, int totalSessions, int totalMinutes,
                               java.util.Set<String> activeDays, java.util.Set<String> uniqueCourses, boolean hasConflict) {
            this.lecturerName = lecturerName;
            this.totalSessions = totalSessions;
            this.totalMinutes = totalMinutes;
            this.activeDays = activeDays;
            this.uniqueCourses = uniqueCourses;
            this.hasConflict = hasConflict;
        }

        public String getLecturerName() { return lecturerName; }
        public int getTotalSessions() { return totalSessions; }
        public int getTotalMinutes() { return totalMinutes; }
        public String getFormattedDuration() {
            int hours = totalMinutes / 60;
            int mins = totalMinutes % 60;
            if (mins == 0) return hours + " Jam";
            return hours + " Jam " + mins + " Menit";
        }
        public java.util.Set<String> getActiveDays() { return activeDays; }
        public java.util.Set<String> getUniqueCourses() { return uniqueCourses; }
        public boolean hasConflict() { return hasConflict; }
    }

    /**
     * Menghitung ringkasan beban mengajar dan mendeteksi potensi bentrokan jadwal dosen
     */
    public LecturerSummary getLecturerSummary(String lecturerName) {
        List<RegularSchedule> schedules = getSchedulesByLecturer(lecturerName);
        int totalMinutes = 0;
        java.util.Set<String> activeDays = new java.util.LinkedHashSet<>();
        java.util.Set<String> courses = new java.util.HashSet<>();
        boolean conflict = false;

        for (int i = 0; i < schedules.size(); i++) {
            RegularSchedule s = schedules.get(i);
            activeDays.add(s.getDayOfWeek());
            courses.add(s.getCourseName());
            try {
                LocalTime start = LocalTime.parse(s.getStartTime(), TIME_FORMATTER);
                LocalTime end = LocalTime.parse(s.getEndTime(), TIME_FORMATTER);
                long minutes = java.time.Duration.between(start, end).toMinutes();
                totalMinutes += Math.max(0, (int) minutes);

                // Cek bentrok antar jadwal dosen pada hari yang sama
                for (int j = i + 1; j < schedules.size(); j++) {
                    RegularSchedule other = schedules.get(j);
                    if (s.getDayOfWeek().equalsIgnoreCase(other.getDayOfWeek())) {
                        LocalTime oStart = LocalTime.parse(other.getStartTime(), TIME_FORMATTER);
                        LocalTime oEnd = LocalTime.parse(other.getEndTime(), TIME_FORMATTER);
                        if (start.isBefore(oEnd) && oStart.isBefore(end)) {
                            conflict = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return new LecturerSummary(lecturerName, schedules.size(), totalMinutes, activeDays, courses, conflict);
    }

    /**
     * Menambahkan jadwal perkuliahan tetap baru dengan validasi bentrokan waktu
     */
    public ServiceResult<RegularSchedule> addSchedule(RegularSchedule schedule) {
        if (schedule == null) {
            return ServiceResult.failure("Data jadwal tidak boleh kosong!");
        }

        if (schedule.getRoomId() <= 0) {
            return ServiceResult.failure("Ruangan harus dipilih!");
        }

        if (schedule.getDayOfWeek() == null || schedule.getDayOfWeek().trim().isEmpty()) {
            return ServiceResult.failure("Hari perkuliahan wajib ditentukan!");
        }

        if (schedule.getStartTime() == null || schedule.getEndTime() == null ||
                schedule.getStartTime().trim().isEmpty() || schedule.getEndTime().trim().isEmpty()) {
            return ServiceResult.failure("Jam mulai dan jam selesai wajib diisi!");
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(schedule.getStartTime().trim(), TIME_FORMATTER);
            end = LocalTime.parse(schedule.getEndTime().trim(), TIME_FORMATTER);
            if (!start.isBefore(end)) {
                return ServiceResult.failure("Jam mulai harus lebih awal dari jam selesai!");
            }
        } catch (Exception e) {
            return ServiceResult.failure("Format jam tidak valid (contoh: 08:00)!");
        }

        if (schedule.getCourseName() == null || schedule.getCourseName().trim().isEmpty()) {
            return ServiceResult.failure("Nama mata kuliah wajib diisi!");
        }

        // Cek apakah ada jadwal tetap lain yang bertabrakan di ruangan dan hari yang sama
        List<RegularSchedule> existingList = scheduleDao.getSchedulesByRoomAndDay(
                schedule.getRoomId(), schedule.getDayOfWeek().trim()
        );
        for (RegularSchedule existing : existingList) {
            try {
                LocalTime exStart = LocalTime.parse(existing.getStartTime(), TIME_FORMATTER);
                LocalTime exEnd = LocalTime.parse(existing.getEndTime(), TIME_FORMATTER);

                if (start.isBefore(exEnd) && exStart.isBefore(end)) {
                    return ServiceResult.failure("Bentrokan terdeteksi dengan jadwal tetap yang sudah ada:\n" +
                            existing.getCourseName() + " (" + existing.getClassName() + ") " +
                            existing.getStartTime() + " - " + existing.getEndTime() + " (Dosen: " + existing.getLecturerName() + ")");
                }
            } catch (Exception ignored) {
            }
        }

        boolean saved = scheduleDao.addSchedule(schedule);
        if (saved) {
            return ServiceResult.success("Jadwal tetap perkuliahan berhasil disimpan!", schedule);
        } else {
            return ServiceResult.failure("Gagal menyimpan jadwal perkuliahan ke database.");
        }
    }

    /**
     * Menghapus jadwal perkuliahan tetap berdasarkan ID
     */
    public ServiceResult<Void> deleteSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            return ServiceResult.failure("ID jadwal tidak valid!");
        }

        boolean ok = scheduleDao.deleteSchedule(scheduleId);
        if (ok) {
            return ServiceResult.success("Jadwal perkuliahan berhasil dihapus.");
        } else {
            return ServiceResult.failure("Gagal menghapus jadwal dari database.");
        }
    }
}
