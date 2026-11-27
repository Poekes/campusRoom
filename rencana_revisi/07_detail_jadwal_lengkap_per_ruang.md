# Spesifikasi Revisi 7: Detail Jadwal Terpadu dalam Dialog Ruangan

> **ID Butir**: REV-07  
> **Komponen**: `RoomDetailDialog`, `ScheduleService`, `BookingService`, `ScheduleDao`, `BookingDao`  
> **Tujuan**: Memperkaya dialog modal detail ruangan (`RoomDetailDialog`) yang terbuka saat ruangan diklik ganda atau tombol "Detail" ditekan, agar menyajikan daftar seluruh agenda perkuliahan (jadwal tetap mingguan semester dan permohonan booking yang sudah disetujui) yang berlangsung di ruangan tersebut.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- `RoomDetailDialog` saat ini hanya berupa form dialog kecil (93 baris kode) yang hanya memuat informasi statis:
  - Kapasitas, Tipe, Lantai, Fasilitas, Posisi (X,Y).
  - Status terkini hanya berupa satu baris teks sederhana: `room.getCurrentOccupantInfo()`.
- Dialog tidak memberikan visibilitas jadwal menyeluruh bagi pengguna. Mahasiswa atau dosen tidak dapat mengetahui:
  - Jam berapa saja ruangan ini terisi pada hari Senin, Selasa, Rabu, Kamis, Jumat, Sabtu.
  - Mata kuliah apa saja yang memakai ruangan ini dan siapa dosen pengampunya.
  - Apakah ada permohonan peminjaman lain yang sudah disetujui di tanggal mendatang.

### Kondisi yang Diharapkan (To-Be)
- Rombak `RoomDetailDialog` menjadi dialog komprehensif bergaya tabbed atau split layout:
  1. **Tab 1 / Panel Kiri: Informasi Fasilitas & Spesifikasi Ruangan**:
     - Kode, Nama, Gedung, Lantai, Tipe Ruangan.
     - Kapasitas kursi dengan visual icon.
     - Fasilitas lengkap (AC, LCD Proyektor, Wi-Fi, Audio Sound System, Whiteboard).
     - Status real-time sesi saat ini (Kosong / Terpakai).
  2. **Tab 2: Master Jadwal Perkuliahan Tetap (Jadwal Reguler Semester)**:
     - Tabel berisi seluruh jadwal rutin yang terjadwal di ruangan ini: Hari, Waktu (Mulai - Selesai), Kode MK, Nama Mata Kuliah, Dosen Pengampu, Kelas.
  3. **Tab 3: Jadwal Pemakaian Khusus / Kelas Pengganti (Bookings Aktif)**:
     - Daftar permohonan peminjaman ruangan yang telah berstatus `DISETUJUI` di ruangan ini (Tanggal, Sesi Jam, Keperluan, Pemohon).

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi Data Layer (`ScheduleDao.java` & `BookingDao.java`)

1. **Pada `ScheduleDao.java`**:
   ```java
   /**
    * Mengambil seluruh jadwal kuliah rutin semester untuk satu ruangan tertentu
    */
   public List<RegularSchedule> getSchedulesByRoomId(int roomId) {
       List<RegularSchedule> list = new ArrayList<>();
       String sql = "SELECT * FROM regular_schedules WHERE room_id = ? " +
                    "ORDER BY CASE day_of_week " +
                    "  WHEN 'Senin' THEN 1 WHEN 'Selasa' THEN 2 WHEN 'Rabu' THEN 3 " +
                    "  WHEN 'Kamis' THEN 4 WHEN 'Jumat' THEN 5 WHEN 'Sabtu' THEN 6 ELSE 7 END, " +
                    "start_time ASC";
       // Eksekusi JDBC PreparedStatement dan mapping ke RegularSchedule
       return list;
   }
   ```

2. **Pada `BookingDao.java`**:
   ```java
   /**
    * Mengambil seluruh permohonan booking yang disetujui untuk ruangan tertentu
    */
   public List<Booking> getApprovedBookingsByRoomId(int roomId) {
       List<Booking> list = new ArrayList<>();
       String sql = "SELECT * FROM bookings WHERE room_id = ? AND status = 'DISETUJUI' " +
                    "ORDER BY booking_date ASC, start_time ASC";
       // Eksekusi JDBC PreparedStatement
       return list;
   }
   ```

### B. Modifikasi Service Layer (`ScheduleService.java` & `BookingService.java`)

```java
// Di ScheduleService.java:
public List<RegularSchedule> getSchedulesForRoom(int roomId) {
    if (roomId <= 0) return Collections.emptyList();
    return scheduleDao.getSchedulesByRoomId(roomId);
}

// Di BookingService.java:
public List<Booking> getApprovedBookingsForRoom(int roomId) {
    if (roomId <= 0) return Collections.emptyList();
    return bookingDao.getApprovedBookingsByRoomId(roomId);
}
```

### C. Redesain `RoomDetailDialog.java`

Ubah ukuran dialog menjadi lebih representatif (misal $780 \times 540$ px) dengan `JTabbedPane`:
1. **Header Dialog**:
   - Menampilkan Kode Ruang, Nama Ruangan, dan Banner Status Ketersediaan Sesi Terpilih.
2. **Tabbed Pane**:
   - **Tab 1: 🏢 Fasilitas & Spesifikasi**: Grid ringkas atribut ruangan dan fasilitas.
   - **Tab 2: 📅 Jadwal Tetap Perkuliahan**:
     - `JTable` modern dengan kolom: `Hari`, `Waktu`, `Kode MK`, `Mata Kuliah`, `Dosen Pengampu`, `Kelas`.
     - Filter cepat hari: `Semua Hari`, `Senin`, `Selasa`, dst.
   - **Tab 3: 📌 Reservasi Kelas Pengganti (Approved Bookings)**:
     - `JTable` permohonan aktif yang sudah disetujui: `Tanggal`, `Waktu`, `Acara / Mata Kuliah`, `Dosen`, `PJ Mahasiswa`.
3. **Footer Actions**:
   - Tombol "Tutup"
   - Tombol "Ajukan Booking Ruangan Ini" (jika sesi yang dipilih tersedia).

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-07-1**: Mengklik tombol "Detail & Jadwal Lengkap" atau mengklik ganda ruangan pada denah membuka dialog detail yang memiliki tab jadwal lengkap.
2. **AC-07-2**: Tab "Jadwal Tetap Perkuliahan" menampilkan seluruh mata kuliah yang memakai ruangan tersebut, terurut rapi dari hari Senin hingga Sabtu.
3. **AC-07-3**: Tab "Reservasi Khusus" menampilkan seluruh booking dengan status `DISETUJUI` untuk ruangan tersebut di masa mendatang.
4. **AC-07-4**: Jika ruangan tidak memiliki jadwal reguler atau booking, sistem menampilkan keterangan tabel kosong (*"Belum ada jadwal terdaftar untuk ruangan ini"*).
5. **AC-07-5**: Tombol "Ajukan Booking" pada footer dialog tetap berfungsi mengarahkan pengguna ke form pemesanan dengan data ruangan yang sudah terisi otomatis.
