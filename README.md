# CampusRoom — Aplikasi Manajemen Booking Ruangan Kampus
> Berbasis Java Swing, SQLite, dan Menerapkan Konsep Object-Oriented Programming (OOP)

---

## 📌 Ringkasan Proyek
**CampusRoom** adalah aplikasi desktop modern yang dirancang untuk mengatasi permasalahan nyata yang sering dihadapi oleh Penanggung Jawab (PJ) Mata Kuliah dan mahasiswa: **bentrokan jadwal kuliah** dan **kesulitan mencari ruangan kosong untuk perkuliahan pengganti**.

Dengan CampusRoom, pencarian dan pemesanan ruangan dilakukan secara visual melalui **denah interaktif** yang menampilkan ketersediaan ruangan secara real-time (warna **Hijau** untuk ruangan kosong, dan **Merah** untuk ruangan yang sedang dipakai), didukung alur verifikasi resmi berjenjang antara **Mahasiswa (PJ)**, **Sekretaris Dekan**, dan **Administrator Master**.

---

## 🏛️ Fitur Utama Aplikasi
1. **Sistem Login Modern dengan 3 Contoh Akun Demo Bawaan**:
   - Layar login interaktif saat aplikasi dibuka.
   - Dilengkapi kartu **Quick Login 1-Klik** untuk masing-masing peran:
     - 🎓 **Mahasiswa (PJ Mata Kuliah)**: `mahasiswa` / `123` (Muhamad Nur Akbar, NIM: 250111019)
     - 🏛️ **Sekretaris Dekan (Sekdek)**: `sekdek` / `123` (Ibu Nurul Hidayah, S.Kom.)
     - ⚙️ **Administrator Master**: `admin` / `123` (Bpk. Rahmat Santoso)
   - Dukungan login manual dengan validasi input, case-insensitive username, toggle tampilkan password, serta tombol **Logout** resmi di header aplikasi yang kembali ke dialog login.

2. **Denah Visual Interaktif Multi-Gedung & Multi-Lantai**:
   - Menampilkan layout blueprint lantai gedung secara spesifik per tingkat (**Lantai 1, 2, 3, 4, dst.**).
   - Status ruangan diwarnai secara dinamis:
     - 🟢 **HIJAU**: Ruangan Kosong / Tersedia pada tanggal dan slot jam yang dipilih.
     - 🔴 **MERAH**: Ruangan Terpakai / Sudah dipesan atau sedang ada jadwal kuliah tetap.
   - Tooltip interaktif saat mouse hover (kapasitas, tipe, fasilitas, dan detail pemakaian/dosen).
   - Klik untuk memilih dan double click untuk melihat modal detail spesifikasi ruangan.

3. **Editor Layout Denah Visual dengan Ribbon Header ala Microsoft Word**:
   - Tools edit ditempatkan di **header bagian atas berbentuk Ribbon Toolbar** (seperti Microsoft Word/Office):
     - **🏢 Kelompok Lokasi & Tingkat Lantai**: Pilihan Kampus, Gedung, dan Tingkat Lantai dinamis dengan tombol **➕ Tambah Lantai Baru** untuk menambah tingkat lantai berapa pun pada gedung.
     - **🗺️ Kelompok Blueprint**: Unggah gambar blueprint denah custom (PNG/JPG) khusus per tingkat lantai gedung, atau reset ke gambar standar.
     - **📐 Kelompok Posisi & Dimensi**: Form koordinat (X, Y, Lebar/W, Tinggi/H) yang tersinkronisasi dua arah secara realtime dengan drag-and-drop dan corner resize di canvas.
     - **🔲 Kelompok Tata Letak & Perataan**: Auto Grid (4 Kolom / 2 Kolom), Rata Sisi Kiri, dan Standarkan Ukuran objek ruangan.
     - **🚪 Kelompok Manajemen Ruangan**: Tambah Ruangan Baru di lantai aktif dengan rekomendasi kode cerdas, Duplikasi Ruangan, dan Hapus Ruangan.
     - **💾 Kelompok Dokumen & Simpan**: Indikator status perubahan realtime, Simpan permanen koordinat layout ke database SQLite, atau Batal / Muat Ulang.
   - Setiap lantai gedung dapat memiliki banyak layout denah yang independen dan tersimpan permanen.

4. **Multi-Kampus, Multi-Gedung & Multi-Lantai**:
   - Sistem mendukung kampus pusat maupun kampus cabang/terpadu.
   - Setiap gedung dapat memiliki jumlah lantai bertingkat yang berbeda-beda, dengan blueprint dan pemetaan ruangan masing-masing.

5. **Kapasitas & Fasilitas Terperinci**:
   - Informasi lengkap setiap ruangan: kapasitas tempat duduk mahasiswa, AC, LCD Proyektor, Smart TV, Whiteboard, Sound System, dan tipe ruangan (Kelas Teori, Laboratorium, Smart Classroom, Auditorium, Studio).

6. **Ketersediaan Ruangan Dinamis Berdasarkan Waktu**:
   - Filter pencarian berbasis tanggal dan slot waktu perkuliahan (08:00 - 10:30, 10:30 - 13:00, 13:00 - 15:30, 15:30 - 18:00, 18:00 - 20:30).
   - Pendeteksian bentrokan otomatis terhadap Jadwal Perkuliahan Tetap maupun Booking Mahasiswa yang telah disetujui.

7. **Alur Validasi & Approval Bertingkat (Role-Based Workflow)**:
   - **Mahasiswa (PJ Mata Kuliah / Public)**: Melihat denah, cek ketersediaan, mengajukan permohonan ruangan pengganti.
   - **Sekretaris Dekan (Sekdek)**: Menginputkan master jadwal tetap mingguan dan melakukan validasi pengajuan mahasiswa terhadap jadwal tetap fakultas.
   - **Administrator Master**: Memberikan approval akhir (penguncian resmi ruangan), mengelola master kampus/gedung/ruangan, serta mengatur layout denah visual dengan Ribbon Editor.

---

## 🧩 Penerapan Konsep Object-Oriented Programming (OOP)
Aplikasi ini dibangun dengan struktur berorientasi objek yang ketat dan modular:

| Konsep OOP | Implementasi pada Proyek CampusRoom |
|---|---|
| **Class & Object** | Pemodelan entitas nyata: `Campus`, `Building`, `Room`, `User`, `RegularSchedule`, `Booking`. |
| **Encapsulation** | Semua atribut entitas berakses `private`/`protected` dengan getter dan setter yang dilengkapi validasi bisnis (misal kapasitas harus > 0, waktu mulai < waktu selesai). |
| **Inheritance** | Superclass `BaseEntity` diturunkan ke semua model entitas; Superclass abstrak `User` diturunkan ke `StudentUser`, `AdminUser`, dan `SecretaryUser`. |
| **Abstraction** | Penggunaan kelas abstrak `User` dan `BaseEntity` yang mendefinisikan template perilaku mendasar pengguna tanpa mengekspos detail implementasi internal. |
| **Polymorphism** | Method overriding pada subclass pengguna: `getRoleDisplayName()`, `canApproveBooking()`, `canManageMasterData()`, serta Dynamic User Factory di `UserDao.mapUserFromResultSet()`. |
| **Constructor Overloading** | Overloading konstruktor pada `Campus`, `Building`, `Room`, dan `RegularSchedule` untuk fleksibilitas instansiasi baru maupun pemetaan dari database. |

---

## 🗄️ Database SQLite
Database disimpan secara lokal pada file `campusroom.db` menggunakan driver JDBC `org.xerial.sqlite-jdbc`. Skema tabel dan data awal (seeding) otomatis dibuat saat aplikasi dijalankan pertama kali:
- Tabel: `campuses`, `buildings`, `rooms`, `users`, `regular_schedules`, `bookings`.

---

## 🚀 Cara Menjalankan Aplikasi

### Opsi 1: Menjalankan via Skrip Terminal (Sangat Cepat & Tanpa Setup Tambahan)
Pastikan berada di direktori `campusroom_app`:
```bash
cd campusroom_app
./run.sh
```
Skrip ini akan otomatis mengompilasi seluruh source code dengan `javac` dan menyertakan dependensi JAR bawaan di folder `lib/`.

### Opsi 2: Menjalankan di Apache NetBeans
1. Buka **Apache NetBeans**.
2. Pilih menu **File** -> **Open Project**.
3. Arahkan dan pilih folder `campusroom_app` (NetBeans akan otomatis mengenali icon kubus proyek Maven dari file `pom.xml`).
4. Klik kanan pada proyek -> pilih **Run** (atau tekan **F6**).

### Opsi 3: Menjalankan via Apache Maven CLI
```bash
cd campusroom_app
mvn compile exec:java
```

---

## 👥 Akun Demo untuk Presentasi
Untuk memudahkan demonstrasi alur kerja 3 peran, disediakan fitur **Quick Role Switcher** di pojok kanan atas jendela aplikasi, atau login dengan akun berikut:

| Peran (Role) | Username | Password | Nama Pengguna | Tugas & Hak Akses |
|---|---|---|---|---|
| **Mahasiswa (PJ)** | `mahasiswa` | `123` | Muhamad Nur Akbar (PJ Kuliah) | Melihat denah visual, cek ruangan kosong/terpakai, mengajukan permohonan booking. |
| **Sekretaris Dekan** | `sekdek` | `123` | Ibu Nurul Hidayah, S.Kom. | Menginputkan jadwal tetap perkuliahan, memvalidasi permohonan mahasiswa. |
| **Admin Master** | `admin` | `123` | Bpk. Rahmat Santoso | Approval final ruangan, kelola data kampus & gedung, editor layout koordinat denah. |

---

## 📂 Struktur Direktori Proyek
```text
campusroom_app/
├── pom.xml                               # Konfigurasi Maven & Apache NetBeans
├── run.sh                                # Skrip eksekusi satu-klik
├── build.sh                              # Skrip kompilasi otomatis
├── campusroom.db                         # Database lokal SQLite
├── lib/                                  # Dependensi pustaka mandiri
│   ├── sqlite-jdbc.jar
│   └── flatlaf.jar
├── images/                               # Aset gambar denah gedung
│   ├── floorplan_gedung_a.png
│   └── floorplan_gedung_b.png
└── src/
    └── main/
        ├── java/com/campusroom/
        │   ├── CampusRoomApp.java        # Main Entry Point
        │   ├── model/                    # Entitas OOP (BaseEntity, User, Room, dll)
        │   ├── dao/                      # Akses SQLite (DatabaseHelper, RoomDao, dll)
        │   ├── service/                  # Logika Bisnis (AvailabilityService, dll)
        │   └── ui/                       # Antarmuka Java Swing (FloorPlan, MainFrame, dll)
        └── resources/
            └── images/                   # Resource gambar internal
```
