# Spesifikasi Revisi 3: Validasi Batas Booking H-1 & Penguncian Status Fiks

> **ID Butir**: REV-03  
> **Komponen**: `BookingService`, `BookingDialog`, `StudentBookingPanel`, `AdminPanel`, `MasterDataService`  
> **Tujuan**: Menerapkan aturan bisnis ketat bahwa pemesanan ruangan hanya dapat diajukan maksimal H-1 (minimal 1 hari sebelum tanggal penggunaan), serta mengunci data peminjaman dan master ruangan yang statusnya sudah disetujui resmi (*FIKS*) agar tidak dapat diubah-ubah sembarangan.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
1. **Validasi Tanggal Lemah**: Mahasiswa dapat memilih tanggal booking hari ini (H-0) atau bahkan tanggal lampau. Hal ini bertentangan dengan Standar Operasional Prosedur (SOP) kampus yang mewajibkan verifikasi dekanat minimal 1 hari sebelumnya.
2. **Tidak Ada Proteksi Jadwal Fiks**:
   - Booking yang statusnya sudah `DISETUJUI` (Approved oleh Admin Sarpras) secara visual masih terlihat seperti baris biasa dan tidak ada mekanisme proteksi *read-only*.
   - Ruangan yang sudah memiliki jadwal tetap perkuliahan atau permohonan yang sudah disetujui masih dapat dihapus secara ceroboh oleh admin melalui tabel master ruangan tanpa pengecekan integritas jadwal aktif.

### Kondisi yang Diharapkan (To-Be)
1. **Aturan Bisnis H-1**:
   - Peminjaman ruangan wajib diajukan paling lambat **H-1** (tanggal peminjaman $\ge$ `LocalDate.now().plusDays(1)`).
   - Pengajuan pada hari H atau tanggal lampau akan ditolak langsung di level service dan form antarmuka dengan pesan penolakan yang jelas.
   - Pada form `BookingDialog`, terdapat label/peringatan visual panduan batas waktu H-1.
2. **Penguncian Data Fiks (Immutable Guard)**:
   - Permohonan ruangan yang sudah berstatus `DISETUJUI` berstatus **FIKS**:
     - Mahasiswa tidak dapat mengubah data, membatalkan sepihak, atau memindahkan ruangan.
     - Badge tabel bertuliskan `DISETUJUI (FIKS / RESMI)` dengan warna hijau tua solid.
   - Master Ruangan yang memiliki jadwal tetap atau booking fiks yang masih aktif **dikunci dari penghapusan**:
     - Jika admin mencoba menghapus ruangan yang memiliki jadwal aktif, sistem menampilkan modal penolakan: *"Ruangan tidak dapat dihapus karena memiliki X jadwal perkuliahan tetap dan Y booking aktif yang sudah fiks."*

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi Service Layer (`BookingService.java`)

Tambahkan validasi aturan batas waktu H-1 pada method `createBooking`:
```java
// Validasi Batas Pemesanan Maksimal H-1
LocalDate today = LocalDate.now();
LocalDate minAllowedDate = today.plusDays(1);

if (date.isBefore(minAllowedDate)) {
    if (date.isEqual(today)) {
        return ServiceResult.failure(
            "Pemesanan ruangan TIDAK DAPAT dilakukan pada hari H!\n" +
            "Sesuai regulasi operasional kampus, pengajuan permohonan ruangan wajib diajukan paling lambat H-1 (minimal 24 jam sebelum jadwal sesi perkuliahan)."
        );
    } else {
        return ServiceResult.failure(
            "Tanggal peminjaman (" + date + ") tidak valid karena telah lampau!\n" +
            "Silakan pilih tanggal di masa depan minimal H-1 dari hari ini."
        );
    }
}
```

Tambahkan metode proteksi integritas pembatalan/penghapusan booking:
```java
public boolean isBookingFixed(Booking booking) {
    return booking != null && "DISETUJUI".equalsIgnoreCase(booking.getStatus());
}
```

### B. Modifikasi Validasi Master Ruangan (`MasterDataService.java`)

Cegah penghapusan ruangan yang telah memiliki jadwal tetap perkuliahan atau booking fiks:
```java
public ServiceResult<Void> deleteRoom(int roomId) {
    // 1. Cek jadwal reguler aktif
    List<RegularSchedule> schedules = scheduleDao.getSchedulesByRoom(roomId);
    if (!schedules.isEmpty()) {
        return ServiceResult.failure(
            "Gagal menghapus ruangan!\nRuangan ini masih memiliki " + schedules.size() + 
            " jadwal perkuliahan tetap semester. Hapus atau pindahkan jadwal terlebih dahulu."
        );
    }

    // 2. Cek booking aktif/fiks
    List<Booking> activeBookings = bookingDao.getActiveBookingsByRoom(roomId);
    if (!activeBookings.isEmpty()) {
        return ServiceResult.failure(
            "Gagal menghapus ruangan!\nRuangan ini memiliki " + activeBookings.size() + 
            " permohonan peminjaman yang sudah fiks/disetujui."
        );
    }

    boolean ok = roomDao.deleteRoom(roomId);
    return ok ? ServiceResult.success("Ruangan berhasil dihapus.") 
              : ServiceResult.failure("Gagal menghapus data ruangan dari database.");
}
```

### C. Modifikasi UI Layer (`BookingDialog.java`)

1. Pada bagian atas form `BookingDialog`, tambahkan **Banner Informasi Kebijakan H-1**:
   ```java
   JPanel alertBanner = ModernUIHelper.createNoticeBanner(
       "ℹ️ Ketentuan Reservasi: Pengajuan ruangan wajib dilakukan minimal H-1 sebelum kegiatan. Ruangan yang telah disetujui (FIKS) tidak dapat diubah kembali."
   );
   ```
2. Set tanggal default pada field tanggal menjadi `LocalDate.now().plusDays(1)` (bukan hari ini).
3. Tambahkan validasi instan pada *FocusLost* atau saat tombol "Kirim Pengajuan" ditekan.

### D. Modifikasi UI Layer (`StudentBookingPanel.java`)

- Untuk baris booking yang berstatus `DISETUJUI`:
  - Tambahkan tooltip pada tabel: `"Permohonan ini telah disetujui resmi oleh Dekanat & Sarpras (Status FIKS). Ruangan telah dialokasikan dan tidak dapat diubah."`
  - Tombol aksi batal / ubah dinonaktifkan jika permohonan berstatus `DISETUJUI`.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-03-1**: Mengajukan booking dengan tanggal hari ini ($H-0$) menghasilkan pesan peringatan bahwa pemesanan wajib maksimal H-1.
2. **AC-03-2**: Mengajukan booking dengan tanggal kemarin (lampau) menghasilkan pesan kesalahan tanggal tidak valid.
3. **AC-03-3**: Mengajukan booking untuk besok ($H+1$) atau hari-hari berikutnya berhasil lolos validasi tanggal.
4. **AC-03-4**: Data booking yang berstatus `DISETUJUI` memiliki penanda visual `[FIKS / RESMI]` dan tidak dapat diedit atau dibatalkan sepihak oleh mahasiswa.
5. **AC-03-5**: Mencoba menghapus ruangan di Admin Master yang memiliki jadwal reguler atau booking fiks menghasilkan pesan penolakan dan data tetap terlindungi.
