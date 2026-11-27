# Spesifikasi Revisi 2: Tempat Melihat Detail Jadwal per Dosen untuk Akun Sekre

> **ID Butir**: REV-02  
> **Komponen**: `SecretaryPanel`, `ScheduleService`, `ScheduleDao`, `RegularSchedule`  
> **Tujuan**: Menyediakan portal/sub-panel khusus bagi Sekretaris Dekan (Sekdek) untuk mencari, memfilter, dan memeriksa rincian jadwal perkuliahan mingguan per dosen pengampu secara lengkap dan komprehensif.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- `SecretaryPanel` saat ini hanya memiliki dua tab:
  1. *Antrean Validasi Pengajuan* (tabel permohonan booking mahasiswa).
  2. *Master Jadwal Tetap Perkuliahan* (tabel datar seluruh jadwal semester tanpa pengelompokan dosen).
- Sekretaris tidak memiliki fasilitas untuk melihat jadwal mengajar seorang dosen tertentu.
- Jika ada dosen yang menanyakan jadwalnya atau dekanat ingin mencocokkan jadwal pengganti, sekretaris harus mencari manual satu per satu dari ratusan baris jadwal.

### Kondisi yang Diharapkan (To-Be)
- Terdapat tab/sub-view baru di portal Sekretaris: **"Jadwal per Dosen"** (Lecturer Schedule Timetable).
- Dilengkapi dengan:
  1. **Selector / Autocomplete Pencarian Dosen**: Dropdown atau input pencarian dengan daftar seluruh dosen pengampu yang terdaftar.
  2. **Ringkasan Profil Dosen (*Lecturer Summary Card*)**:
     - Nama Dosen & NIDN / Gelar
     - Total SKS / Total Jam Mengajar per minggu
     - Jumlah Mata Kuliah yang diampu
     - Hari-hari aktif mengajar (misal: *Senin, Rabu, Jumat*)
  3. **Tabel Jadwal Mengajar Terstruktur**:
     - Kolom: Hari, Jam Mulai, Jam Selesai, Gedung & Ruangan, Kode MK, Nama Mata Kuliah, Kelas, Tipe Sesi (Reguler / Kelas Pengganti Terverifikasi).
  4. **Deteksi Bentrok Jadwal Mengajar Dosen**:
     - Sistem mendeteksi otomatis jika terdapat tumpang tindih waktu mengajar bagi dosen yang sama di dua ruangan berbeda dan menandainya dengan badge peringatan merah (*BENTROK WAKTU*).

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi Data Layer (`ScheduleDao.java`)

Tambahkan query baru untuk mendukung agregasi jadwal per pengajar:
```java
/**
 * Mengambil daftar nama unik seluruh dosen pengampu yang memiliki jadwal aktif
 */
public List<String> getAllUniqueLecturers() {
    List<String> list = new ArrayList<>();
    String sql = "SELECT DISTINCT lecturer_name FROM regular_schedules " +
                 "WHERE lecturer_name IS NOT NULL AND TRIM(lecturer_name) != '' " +
                 "ORDER BY lecturer_name ASC";
    // Eksekusi query JDBC PreparedStatement dan mapping ke List<String>
    return list;
}

/**
 * Mengambil seluruh jadwal kuliah rutin yang diampu oleh dosen tertentu
 */
public List<RegularSchedule> getSchedulesByLecturer(String lecturerName) {
    List<RegularSchedule> list = new ArrayList<>();
    String sql = "SELECT s.*, r.code as room_code, r.name as room_name, b.name as building_name " +
                 "FROM regular_schedules s " +
                 "JOIN rooms r ON s.room_id = r.id " +
                 "JOIN buildings b ON r.building_id = b.id " +
                 "WHERE LOWER(s.lecturer_name) = LOWER(?) " +
                 "ORDER BY CASE s.day_of_week " +
                 "  WHEN 'Senin' THEN 1 WHEN 'Selasa' THEN 2 WHEN 'Rabu' THEN 3 " +
                 "  WHEN 'Kamis' THEN 4 WHEN 'Jumat' THEN 5 WHEN 'Sabtu' THEN 6 ELSE 7 END, " +
                 "s.start_time ASC";
    // Eksekusi query dengan parameter lecturerName
    return list;
}
```

### B. Modifikasi Service Layer (`ScheduleService.java`)

```java
public List<String> getAllLecturers() {
    return scheduleDao.getAllUniqueLecturers();
}

public List<RegularSchedule> getLecturerSchedule(String lecturerName) {
    if (lecturerName == null || lecturerName.trim().isEmpty()) {
        return Collections.emptyList();
    }
    return scheduleDao.getSchedulesByLecturer(lecturerName.trim());
}

public static class LecturerSummary {
    private String lecturerName;
    private int totalSessions;
    private int totalHoursMinutes;
    private Set<String> activeDays;
    private Set<String> courses;
    // Getters, Setters, dan method kalkulasi durasi jam mengajar
}

public LecturerSummary calculateLecturerSummary(String lecturerName) {
    List<RegularSchedule> schedules = getLecturerSchedule(lecturerName);
    // Hitung total jam, kelas, dan hari aktif
    return summary;
}
```

### C. Modifikasi UI Layer (`SecretaryPanel.java`)

1. Tambahkan tab ke-3 pada `tabbedPane` di `SecretaryPanel`:
   ```java
   tabbedPane.addTab("Jadwal per Dosen", createLecturerSchedulePanel());
   ```
2. Desain Layout `createLecturerSchedulePanel()`:
   - **Header Card**:
     - Label "Pilih Dosen:" + `JComboBox<String> cbLecturers` (dengan pencarian live / filter text).
     - Tombol "Muat Ulang" dan tombol "Cetak / Ekspor Jadwal Dosen (PDF/TXT)".
   - **Summary Card**:
     - Menampilkan chip informasi: `Total Sesi: 6 Kelas`, `Total Jam: 15 Jam / Minggu`, `Hari: Senin, Rabu, Kamis`.
   - **Center Table Card**:
     - `JTable tblLecturerSchedule` dengan model terstruktur.
     - Custom Renderer: Pewarnaan baris berdasarkan hari kuliah agar nyaman dibaca.
   - **Bottom Indicator**:
     - Label status ketersediaan dosen saat ini.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-02-1**: Akun sekretaris dekan dapat membuka tab "Jadwal per Dosen" di portal sekretaris.
2. **AC-02-2**: Dropdown dosen menampilkan seluruh pengajar yang ada di database secara alfabetis tanpa duplikasi.
3. **AC-02-3**: Memilih salah satu nama dosen (misal: "Dr. Ir. Hendra Saputra, M.T.") memunculkan seluruh daftar mata kuliah yang dia ampu terurut berdasarkan hari (Senin s/d Sabtu) dan jam perkuliahan.
4. **AC-02-4**: Kartu ringkasan menghitung total jam mengajar dan daftar hari aktif secara akurat sesuai data database.
5. **AC-02-5**: Jika dosen tersebut tidak memiliki jadwal, tabel menampilkan *empty state placeholder* dengan pesan informatif: *"Tidak ada jadwal perkuliahan terdaftar untuk dosen ini."*
