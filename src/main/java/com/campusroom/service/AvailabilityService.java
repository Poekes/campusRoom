package com.campusroom.service;

import com.campusroom.dao.BookingDao;
import com.campusroom.dao.ScheduleDao;
import com.campusroom.model.Booking;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AvailabilityService - Logika bisnis penentuan ketersediaan ruangan secara real-time.
 * Memeriksa bentrok dengan Jadwal Tetap Perkuliahan (Sekdek) dan Booking Mahasiswa (Disetujui/Divalidasi).
 */
public class AvailabilityService {
    private final ScheduleDao scheduleDao;
    private final BookingDao bookingDao;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public AvailabilityService() {
        this.scheduleDao = new ScheduleDao();
        this.bookingDao = new BookingDao();
    }

    /**
     * Memperbarui status ketersediaan (Hijau/Merah) untuk daftar ruangan berdasarkan tanggal dan slot jam
     */
    public void updateRoomsAvailability(List<Room> rooms, LocalDate date, String startTimeStr, String endTimeStr) {
        String dayOfWeekIndo = convertDayOfWeekToIndonesian(date.getDayOfWeek());

        for (Room room : rooms) {
            AvailabilityResult result = checkAvailability(room.getId(), date, dayOfWeekIndo, startTimeStr, endTimeStr);
            room.setAvailable(result.isAvailable());
            room.setCurrentOccupantInfo(result.getOccupantInfo());
        }
    }

    public AvailabilityResult checkAvailability(int roomId, LocalDate date, String dayOfWeekIndo,
                                                String startTimeStr, String endTimeStr) {
        LocalTime reqStart = LocalTime.parse(startTimeStr, TIME_FORMATTER);
        LocalTime reqEnd = LocalTime.parse(endTimeStr, TIME_FORMATTER);

        if (!reqStart.isBefore(reqEnd)) {
            return new AvailabilityResult(false, "Jam mulai harus lebih awal dari jam selesai!");
        }

        // 1. Cek bentrokan dengan Jadwal Tetap Dekanat (Regular Schedules)
        List<RegularSchedule> regularList = scheduleDao.getSchedulesByRoomAndDay(roomId, dayOfWeekIndo);
        for (RegularSchedule reg : regularList) {
            LocalTime regStart = LocalTime.parse(reg.getStartTime(), TIME_FORMATTER);
            LocalTime regEnd = LocalTime.parse(reg.getEndTime(), TIME_FORMATTER);

            if (isTimeOverlapping(reqStart, reqEnd, regStart, regEnd)) {
                String info = "[Jadwal Tetap " + reg.getDayOfWeek() + " " + reg.getStartTime() + "-" + reg.getEndTime() + "] "
                        + reg.getCourseName() + " (" + reg.getClassName() + ") - Dosen: " + reg.getLecturerName();
                return new AvailabilityResult(false, info);
            }
        }

        // 2. Cek bentrokan dengan Booking Mahasiswa lain yang berstatus DISETUJUI atau DIVALIDASI_SEKDEK
        List<Booking> bookings = bookingDao.getBookingsByRoomAndDate(roomId, date.toString());
        for (Booking b : bookings) {
            LocalTime bookStart = LocalTime.parse(b.getStartTime(), TIME_FORMATTER);
            LocalTime bookEnd = LocalTime.parse(b.getEndTime(), TIME_FORMATTER);

            if (isTimeOverlapping(reqStart, reqEnd, bookStart, bookEnd)) {
                String info = "[Booking Aktif " + b.getStartTime() + "-" + b.getEndTime() + "] "
                        + b.getCourseName() + " (PJ: " + b.getStudentName() + ") - " + b.getStatusBadgeText();
                return new AvailabilityResult(false, info);
            }
        }

        // Jika tidak ada bentrokan sama sekali -> KOSONG (HIJAU)
        return new AvailabilityResult(true, "Ruangan Tersedia / Bebas Penggunaan");
    }

    private boolean isTimeOverlapping(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static String convertDayOfWeekToIndonesian(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Senin";
            case TUESDAY: return "Selasa";
            case WEDNESDAY: return "Rabu";
            case THURSDAY: return "Kamis";
            case FRIDAY: return "Jumat";
            case SATURDAY: return "Sabtu";
            case SUNDAY: return "Minggu";
            default: return "Senin";
        }
    }

    public static class AvailabilityResult {
        private final boolean available;
        private final String occupantInfo;

        public AvailabilityResult(boolean available, String occupantInfo) {
            this.available = available;
            this.occupantInfo = occupantInfo;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getOccupantInfo() {
            return occupantInfo;
        }
    }
}
