package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.model.Booking;
import com.campusroom.model.Room;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.BookingService;
import com.campusroom.service.MasterDataService;
import com.campusroom.service.ServiceResult;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BookingServiceTest {
    private static BookingService bookingService;
    private static MasterDataService masterDataService;

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initializeDatabase();
        bookingService = new BookingService();
        masterDataService = new MasterDataService();
    }

    @BeforeEach
    @AfterEach
    public void cleanupTestBookings() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM bookings WHERE course_name LIKE '%Uji Coba Layanan Bisnis%';");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testGetBookingsAndCounts() {
        List<Booking> all = bookingService.getAllBookings();
        Assertions.assertNotNull(all);

        int secCount = bookingService.getPendingSecretaryCount();
        int adminCount = bookingService.getPendingAdminCount();
        Assertions.assertTrue(secCount >= 0);
        Assertions.assertTrue(adminCount >= 0);

        User student = AuthService.getInstance().getCurrentUser();
        List<Booking> userBookings = bookingService.getBookingsForUser(student);
        Assertions.assertNotNull(userBookings);
    }

    @Test
    public void testCreateBookingValidation() {
        Room room = masterDataService.getRoomById(1);
        Assertions.assertNotNull(room);

        // Test 1: Null room
        ServiceResult<Booking> r1 = bookingService.createBooking(null, LocalDate.now().plusDays(2), "08:00", "10:30", "Test MK", "Dosen", "123", "Akbar", "081", "Keperluan", null);
        Assertions.assertFalse(r1.isSuccess());

        // Test 2: Invalid time order (start >= end)
        ServiceResult<Booking> r2 = bookingService.createBooking(room, LocalDate.now().plusDays(2), "10:30", "08:00", "Test MK", "Dosen", "123", "Akbar", "081", "Keperluan", null);
        Assertions.assertFalse(r2.isSuccess());

        // Test 3: Missing required student / course info
        ServiceResult<Booking> r3 = bookingService.createBooking(room, LocalDate.now().plusDays(2), "08:00", "10:30", "", "Dosen", "", "", "081", "Keperluan", null);
        Assertions.assertFalse(r3.isSuccess());
    }

    @Test
    public void testBookingWorkflowLifeCycle() {
        Room room = masterDataService.getRoomById(2); // R. 202
        Assertions.assertNotNull(room);

        // Pilih tanggal 28 hari ke depan di hari Minggu agar pasti bebas dari jadwal reguler Senin-Jumat
        LocalDate futureDate = LocalDate.now().plusDays(28);
        while (futureDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            futureDate = futureDate.plusDays(1);
        }

        // 1. Mahasiswa mengajukan booking
        ServiceResult<Booking> createResult = bookingService.createBooking(
                room, futureDate, "08:00", "10:00",
                "Uji Coba Layanan Bisnis", "Dr. Test", "250111099",
                "Tester Mahasiswa", "081299998888", "Simulasi Service Layer", null
        );

        Assertions.assertTrue(createResult.isSuccess(), "Pengajuan booking harus berhasil: " + createResult.getMessage());
        Booking created = createResult.getData();
        Assertions.assertNotNull(created);
        Assertions.assertTrue(created.getBookingCode().startsWith("BK-"));
        Assertions.assertEquals("MENUNGGU_SEKDEK", created.getStatus());

        // Ambil ID dari database
        List<Booking> pendingSec = bookingService.getBookingsByStatus("MENUNGGU_SEKDEK");
        Booking target = null;
        for (Booking b : pendingSec) {
            if (created.getBookingCode().equals(b.getBookingCode())) {
                target = b;
                break;
            }
        }
        Assertions.assertNotNull(target, "Booking yang baru diajukan harus ada di antrean MENUNGGU_SEKDEK");

        // 2. Sekretaris Dekan memvalidasi
        ServiceResult<Void> secResult = bookingService.validateBySecretary(target.getId(), true, "Disetujui Sekdek untuk pengujian service layer");
        Assertions.assertTrue(secResult.isSuccess());

        List<Booking> pendingAdmin = bookingService.getBookingsByStatus("DIVALIDASI_SEKDEK");
        boolean foundInAdmin = false;
        for (Booking b : pendingAdmin) {
            if (b.getId() == target.getId()) {
                foundInAdmin = true;
                break;
            }
        }
        Assertions.assertTrue(foundInAdmin, "Booking harus berpindah ke antrean DIVALIDASI_SEKDEK");

        // 3. Admin Sarpras menyetujui
        ServiceResult<Void> adminResult = bookingService.approveByAdmin(target.getId(), true, "Approval final oleh Admin Sarpras");
        Assertions.assertTrue(adminResult.isSuccess());

        // 4. Verifikasi Status FIKS (DISETUJUI) tidak dapat diubah / dibatalkan
        Booking fixedBooking = bookingService.getBookingById(target.getId());
        Assertions.assertNotNull(fixedBooking);
        Assertions.assertTrue(bookingService.isBookingFixed(fixedBooking));
        Assertions.assertTrue(fixedBooking.getStatusBadgeText().contains("FIKS"));

        ServiceResult<Void> reValidate = bookingService.validateBySecretary(target.getId(), false, "Coba ubah");
        Assertions.assertFalse(reValidate.isSuccess(), "Booking FIKS tidak boleh diubah oleh sekdek");

        ServiceResult<Void> reApprove = bookingService.approveByAdmin(target.getId(), false, "Coba tolak setelah fiks");
        Assertions.assertFalse(reApprove.isSuccess(), "Booking FIKS tidak boleh diubah oleh admin");

        ServiceResult<Void> cancelFixed = bookingService.cancelBooking(target.getId(), null);
        Assertions.assertFalse(cancelFixed.isSuccess(), "Booking FIKS tidak boleh dibatalkan");
    }

    @Test
    public void testBookingH1RuleAndCancelPending() {
        Room room = masterDataService.getRoomById(2);
        Assertions.assertNotNull(room);

        // 1. Booking pada hari H (hari ini) harus ditolak
        ServiceResult<Booking> sameDay = bookingService.createBooking(
                room, LocalDate.now(), "19:00", "21:00",
                "Uji Coba Layanan Bisnis H0", "Dr. Test", "250111099",
                "Tester", "0812", "Hari H", null
        );
        Assertions.assertFalse(sameDay.isSuccess(), "Booking hari H wajib ditolak (maks H-1)");
        Assertions.assertTrue(sameDay.getMessage().contains("H-1"));

        // 2. Booking pada tanggal lampau harus ditolak
        ServiceResult<Booking> pastDay = bookingService.createBooking(
                room, LocalDate.now().minusDays(1), "19:00", "21:00",
                "Uji Coba Layanan Bisnis Past", "Dr. Test", "250111099",
                "Tester", "0812", "Lampau", null
        );
        Assertions.assertFalse(pastDay.isSuccess(), "Booking tanggal lampau wajib ditolak");

        // 3. Booking pada H+1 (atau Minggu depan) berhasil dan bisa dibatalkan saat masih MENUNGGU_SEKDEK
        LocalDate nextSunday = LocalDate.now().plusDays(1);
        while (nextSunday.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            nextSunday = nextSunday.plusDays(1);
        }
        ServiceResult<Booking> validFuture = bookingService.createBooking(
                room, nextSunday, "19:00", "21:00",
                "Uji Coba Layanan Bisnis Cancel", "Dr. Test", "250111099",
                "Tester", "0812", "Pending", null
        );
        Assertions.assertTrue(validFuture.isSuccess(), "Booking >= H+1 harus berhasil: " + validFuture.getMessage());

        // Cari di DB lalu batalkan
        List<Booking> pendingList = bookingService.getBookingsByStatus("MENUNGGU_SEKDEK");
        Booking created = pendingList.stream()
                .filter(b -> validFuture.getData().getBookingCode().equals(b.getBookingCode()))
                .findFirst().orElse(null);
        Assertions.assertNotNull(created);

        ServiceResult<Void> cancelOk = bookingService.cancelBooking(created.getId(), null);
        Assertions.assertTrue(cancelOk.isSuccess(), "Booking yang belum fiks harus bisa dibatalkan");
    }
}
