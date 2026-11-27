package com.campusroom.service;

import com.campusroom.dao.BookingDao;
import com.campusroom.model.Booking;
import com.campusroom.model.Room;
import com.campusroom.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * BookingService - Layanan proses bisnis untuk siklus hidup peminjaman ruangan.
 * Menangani validasi pengajuan, deteksi bentrok jadwal, pembentukan kode booking,
 * serta alur persetujuan bertingkat (Sekretaris Dekan -> Admin Sarpras).
 */
public class BookingService {
    private final BookingDao bookingDao;
    private final AvailabilityService availabilityService;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public BookingService() {
        this.bookingDao = new BookingDao();
        this.availabilityService = new AvailabilityService();
    }

    public BookingService(BookingDao bookingDao, AvailabilityService availabilityService) {
        this.bookingDao = bookingDao;
        this.availabilityService = availabilityService;
    }

    /**
     * Mengambil seluruh data booking terdaftar
     */
    public List<Booking> getAllBookings() {
        return bookingDao.getAllBookings();
    }

    /**
     * Mengambil daftar booking sesuai peran pengguna aktif.
     * Mahasiswa hanya melihat booking miliknya, peran lain melihat semua.
     */
    public List<Booking> getBookingsForUser(User user) {
        if (user != null && "MAHASISWA".equalsIgnoreCase(user.getRole())) {
            return bookingDao.getBookingsByUserId(user.getId());
        }
        return bookingDao.getAllBookings();
    }

    /**
     * Mengambil data booking berdasarkan ID pengguna
     */
    public List<Booking> getBookingsByUserId(int userId) {
        return bookingDao.getBookingsByUserId(userId);
    }

    /**
     * Mengambil antrean booking berdasarkan status alur persetujuan
     */
    public List<Booking> getBookingsByStatus(String status) {
        return bookingDao.getBookingsByStatus(status);
    }

    /**
     * Mengambil booking aktif pada ruangan dan tanggal tertentu
     */
    public List<Booking> getBookingsByRoomAndDate(int roomId, String date) {
        return bookingDao.getBookingsByRoomAndDate(roomId, date);
    }

    /**
     * Menghitung jumlah permohonan yang menunggu verifikasi Sekretaris Dekan
     */
    public int getPendingSecretaryCount() {
        List<Booking> list = bookingDao.getBookingsByStatus("MENUNGGU_SEKDEK");
        return (list != null) ? list.size() : 0;
    }

    /**
     * Menghitung jumlah permohonan yang siap disetujui Admin Sarpras
     */
    public int getPendingAdminCount() {
        List<Booking> list = bookingDao.getBookingsByStatus("DIVALIDASI_SEKDEK");
        return (list != null) ? list.size() : 0;
    }

    /**
     * Memproses pengajuan pemesanan ruangan baru dari form input mahasiswa.
     * Melakukan validasi kelengkapan data, validasi urutan waktu, pengecekan bentrokan jadwal,
     * serta inisialisasi status awal alur berjenjang.
     */
    public ServiceResult<Booking> createBooking(Room room, LocalDate date, String startTime, String endTime,
                                                String course, String lecturer, String nim, String name,
                                                String phone, String purpose, User currentUser) {
        if (room == null) {
            return ServiceResult.failure("Silakan pilih ruangan terlebih dahulu!");
        }

        if (date == null) {
            return ServiceResult.failure("Tanggal permohonan peminjaman wajib diisi!");
        }

        // Validasi Batas Pemesanan Maksimal H-1
        LocalDate today = LocalDate.now();
        LocalDate minAllowedDate = today.plusDays(1);
        if (date.isBefore(minAllowedDate)) {
            if (date.isEqual(today)) {
                return ServiceResult.failure(
                        "Pemesanan ruangan TIDAK DAPAT dilakukan pada hari H!\n" +
                        "Sesuai ketentuan kampus, pengajuan booking wajib dilakukan paling lambat H-1 sebelum tanggal penggunaan ruangan (minimal besok: " + minAllowedDate + ")."
                );
            } else {
                return ServiceResult.failure(
                        "Tanggal peminjaman (" + date + ") tidak valid karena sudah lampau!\n" +
                        "Silakan pilih tanggal minimal H-1 dari hari ini (" + minAllowedDate + ")."
                );
            }
        }

        if (startTime == null || endTime == null || startTime.trim().isEmpty() || endTime.trim().isEmpty()) {
            return ServiceResult.failure("Jam mulai dan jam selesai perkuliahan wajib ditentukan!");
        }

        try {
            LocalTime start = LocalTime.parse(startTime.trim(), TIME_FORMATTER);
            LocalTime end = LocalTime.parse(endTime.trim(), TIME_FORMATTER);
            if (!start.isBefore(end)) {
                return ServiceResult.failure("Jam mulai (" + startTime + ") harus lebih awal dari jam selesai (" + endTime + ")!");
            }
        } catch (Exception e) {
            return ServiceResult.failure("Format jam tidak valid (contoh format: 08:00)!");
        }

        if (course == null || course.trim().isEmpty()) {
            return ServiceResult.failure("Nama mata kuliah atau kegiatan wajib diisi!");
        }

        if (nim == null || nim.trim().isEmpty()) {
            return ServiceResult.failure("NIM penanggung jawab (PJ) wajib diisi!");
        }

        if (name == null || name.trim().isEmpty()) {
            return ServiceResult.failure("Nama lengkap penanggung jawab (PJ) wajib diisi!");
        }

        // Pengecekan ketersediaan ruangan real-time terhadap jadwal reguler dan booking aktif
        String dayOfWeekIndo = AvailabilityService.convertDayOfWeekToIndonesian(date.getDayOfWeek());
        AvailabilityService.AvailabilityResult availResult = availabilityService.checkAvailability(
                room.getId(), date, dayOfWeekIndo, startTime.trim(), endTime.trim()
        );

        if (!availResult.isAvailable()) {
            return ServiceResult.failure("Ruangan TIDAK DAPAT DIAJUKAN karena terjadi bentrokan waktu!\n\n"
                    + availResult.getOccupantInfo());
        }

        // Generate kode booking unik resmi (Format: BK-YYYYMMDD-XXX)
        String bookingCode = "BK-" + date.toString().replace("-", "") + "-" + (100 + new Random().nextInt(900));
        int userId = (currentUser != null) ? currentUser.getId() : 1;

        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setRoomId(room.getId());
        booking.setUserId(userId);
        booking.setStudentNim(nim.trim());
        booking.setStudentName(name.trim());
        booking.setStudentPhone(phone != null ? phone.trim() : "-");
        booking.setCourseName(course.trim());
        booking.setLecturerName(lecturer != null ? lecturer.trim() : "-");
        booking.setBookingDate(date);
        booking.setStartTime(startTime.trim());
        booking.setEndTime(endTime.trim());
        booking.setPurpose(purpose != null ? purpose.trim() : "-");
        booking.setStatus("MENUNGGU_SEKDEK");

        boolean saved = bookingDao.addBooking(booking);
        if (saved) {
            return ServiceResult.success("Pengajuan Booking Berhasil Dikirim!", booking);
        } else {
            return ServiceResult.failure("Gagal menyimpan pengajuan booking ke database.");
        }
    }

    public Booking getBookingById(int bookingId) {
        return bookingDao.getBookingById(bookingId);
    }

    public List<Booking> getApprovedBookingsForRoom(int roomId) {
        if (roomId <= 0) return java.util.Collections.emptyList();
        return bookingDao.getApprovedBookingsByRoomId(roomId);
    }

    public List<Booking> getAllIncomingBookingsForRoom(int roomId) {
        if (roomId <= 0) return java.util.Collections.emptyList();
        return bookingDao.getAllIncomingBookingsByRoomId(roomId);
    }

    /**
     * Memeriksa apakah tanggal booking memenuhi syarat batas maksimal H-1 (>= besok)
     */
    public boolean isValidBookingDateH1(LocalDate date) {
        return date != null && !date.isBefore(LocalDate.now().plusDays(1));
    }

    /**
     * Memeriksa apakah booking sudah berstatus FIKS (DISETUJUI resmi) sehingga terkunci dari perubahan
     */
    public boolean isBookingFixed(Booking booking) {
        return booking != null && "DISETUJUI".equalsIgnoreCase(booking.getStatus());
    }

    /**
     * Memproses tahapan validasi oleh Sekretaris Dekan (Tingkat 1)
     */
    public ServiceResult<Void> validateBySecretary(int bookingId, boolean approve, String note) {
        if (bookingId <= 0) {
            return ServiceResult.failure("ID pengajuan booking tidak valid!");
        }

        Booking existing = bookingDao.getBookingById(bookingId);
        if (existing == null) {
            return ServiceResult.failure("Data pengajuan booking tidak ditemukan!");
        }
        if (isBookingFixed(existing)) {
            return ServiceResult.failure("Pengajuan " + existing.getBookingCode() + " sudah berstatus FIKS (DISETUJUI) dan tidak dapat diubah lagi!");
        }

        String targetStatus = approve ? "DIVALIDASI_SEKDEK" : "DITOLAK";
        String finalNote = note;
        if (finalNote == null || finalNote.trim().isEmpty()) {
            finalNote = approve ? "Diverifikasi tidak ada bentrokan dengan jadwal tetap dekanat." : "Ditolak.";
        }

        boolean ok = bookingDao.updateSecretaryValidation(bookingId, targetStatus, finalNote.trim());
        if (ok) {
            String msg = approve
                    ? "Pengajuan berhasil divalidasi dan diteruskan ke Admin Sarpras."
                    : "Pengajuan berhasil ditolak.";
            return ServiceResult.success(msg);
        } else {
            return ServiceResult.failure("Gagal memperbarui status validasi pengajuan di database.");
        }
    }

    /**
     * Memproses tahapan persetujuan akhir oleh Admin Sarpras (Tingkat 2)
     */
    public ServiceResult<Void> approveByAdmin(int bookingId, boolean approve, String note) {
        if (bookingId <= 0) {
            return ServiceResult.failure("ID pengajuan booking tidak valid!");
        }

        Booking existing = bookingDao.getBookingById(bookingId);
        if (existing == null) {
            return ServiceResult.failure("Data pengajuan booking tidak ditemukan!");
        }
        if (isBookingFixed(existing)) {
            return ServiceResult.failure("Pengajuan " + existing.getBookingCode() + " sudah berstatus FIKS (DISETUJUI) dan tidak dapat diubah lagi!");
        }

        String targetStatus = approve ? "DISETUJUI" : "DITOLAK";
        String finalNote = note;
        if (finalNote == null || finalNote.trim().isEmpty()) {
            finalNote = approve ? "Disetujui Admin Master. Ruangan telah dialokasikan resmi." : "Ditolak oleh admin.";
        }

        boolean ok = bookingDao.updateAdminApproval(bookingId, targetStatus, finalNote.trim());
        if (ok) {
            String msg = approve
                    ? "Permohonan booking disetujui dan ruangan resmi dialokasikan!"
                    : "Permohonan booking ditolak oleh admin.";
            return ServiceResult.success(msg);
        } else {
            return ServiceResult.failure("Gagal memperbarui status approval booking di database.");
        }
    }

    /**
     * Membatalkan pengajuan booking yang masih menunggu verifikasi.
     * Booking yang sudah berstatus DISETUJUI (FIKS) ditolak dari pembatalan/perubahan.
     */
    public ServiceResult<Void> cancelBooking(int bookingId, User currentUser) {
        if (bookingId <= 0) {
            return ServiceResult.failure("ID pengajuan booking tidak valid!");
        }

        Booking existing = bookingDao.getBookingById(bookingId);
        if (existing == null) {
            return ServiceResult.failure("Data pengajuan booking tidak ditemukan!");
        }

        if (isBookingFixed(existing)) {
            return ServiceResult.failure(
                    "Pengajuan " + existing.getBookingCode() + " sudah berstatus FIKS (DISETUJUI Resmi) dan tidak dapat diubah atau dibatalkan!"
            );
        }

        if ("DITOLAK".equalsIgnoreCase(existing.getStatus())) {
            return ServiceResult.failure("Pengajuan yang sudah ditolak tidak dapat dibatalkan.");
        }

        if (currentUser != null && "MAHASISWA".equalsIgnoreCase(currentUser.getRole())
                && existing.getUserId() != currentUser.getId()) {
            return ServiceResult.failure("Anda tidak memiliki hak untuk membatalkan pengajuan milik pengguna lain!");
        }

        boolean deleted = bookingDao.deleteBooking(bookingId);
        if (deleted) {
            return ServiceResult.success("Pengajuan " + existing.getBookingCode() + " berhasil dibatalkan.");
        } else {
            return ServiceResult.failure("Gagal membatalkan pengajuan di database.");
        }
    }
}
