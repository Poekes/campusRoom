# Panduan Penggunaan & Menjalankan Proyek CampusRoom

Panduan ini ditujukan agar pengguna, rekan tim, maupun dosen penguji dapat langsung membuka dan menjalankan proyek **CampusRoom** dengan mudah menggunakan berbagai metode: IDE (Apache NetBeans, IntelliJ IDEA, Eclipse), script eksekusi mandiri, maupun Maven CLI.

---

## 🚀 1. Cara Menjalankan Aplikasi

Pilih salah satu metode berikut sesuai preferensi dan sistem operasi yang digunakan:

### Opsi A: Menggunakan IDE (Apache NetBeans / IntelliJ IDEA / Eclipse) — Rekomendasi
1. Buka aplikasi IDE pilihan Anda (misalnya **Apache NetBeans**).
2. Klik menu **File** → **Open Project...** (atau shortcut `Ctrl + Shift + O`).
3. Cari dan pilih folder:
   ```text
   CampusRoom
   ```
   *(IDE akan otomatis mendeteksi konfigurasi Maven dengan lencana proyek).*
4. Klik tombol **Open Project**.
5. Jalankan aplikasi dengan salah satu cara:
   - Klik tombol **Run Project** (ikon segitiga hijau ▶️ di toolbar atas) atau tekan shortcut **`F6`**.
   - Atau klik kanan pada proyek **CampusRoom** di panel Projects → pilih **Run**.

---

### Opsi B: Windows Langsung (Tanpa Membuka IDE)
Cukup klik dua kali (*double-click*) file:
```text
run.bat
```
*Script ini akan otomatis mendeteksi Maven atau JDK di komputer Anda dan langsung membuka aplikasi.*

---

### Opsi C: Linux / macOS Terminal
Buka terminal pada direktori proyek, lalu jalankan perintah:
```bash
chmod +x run.sh
./run.sh
```

---

### Opsi D: Maven CLI
Jika Maven terpasang di sistem, jalankan perintah berikut:
```bash
mvn compile exec:java
```

---

## 🔑 2. Akun Demo untuk Pengujian

Aplikasi telah dilengkapi dengan database SQLite bawaan (`campusroom.db`) yang telah terisi data awal:

| No | Role | Username | Password | Deskripsi Hak Akses & Fitur |
|:--:|---|---|---|---|
| 1 | **Mahasiswa (PJ)** | `mahasiswa` | `123` | Muhamad Nur Akbar — Cek denah visual interaktif, cek ketersediaan ruangan real-time, ajukan peminjaman ruangan. |
| 2 | **Sekretaris Dekan** | `sekdek` | `123` | Ibu Nurul Hidayah, S.Kom. — Dashboard persetujuan booking (Setujui / Tolak), input catatan resmi, cetak pratinjau surat izin. |
| 3 | **Admin Sarpras** | `admin` | `123` | Bpk. Rahmat Santoso — Approval akhir, manajemen master Kampus, Gedung, Ruangan, Jadwal Tetap, serta Editor Denah. |

---

## 🏢 3. Fitur Utama yang Dapat Dicoba

1. **Denah Visual Interaktif Multi-Lantai:**
   - Navigasi Kampus (Kampus 1 / Kampus 2).
   - Pemilihan Gedung (Gedung A, Gedung B, Gedung Kampus 2).
   - Pemilihan Lantai via Ribbon Bar (Lantai 1, 2, 3, 4).
   - **Inspector Ruangan (Left Bar Interaktif)**: Panel detail ruangan didesain sebagai Left Bar collapsible di sebelah kiri denah (hemat ruang canvas). Dilengkapi tombol ciutkan/perluas (`◀`/`▶`), shortcut keyboard `Ctrl + I`, dan tombol toggle di toolbar. Ketika kotak ruangan pada denah diklik, inspector otomatis terbuka menampilkan kapasitas, fasilitas, status ketersediaan realtime (Hijau: Kosong, Merah: Terpakai), dan tombol aksi booking.
2. **Katalog & Filter Ruangan Real-Time:**
   - Menampilkan seluruh inventaris ruangan dengan filter gedung dan status operasional.
3. **Editor Denah (Layout Editor):**
   - Drag-and-drop ruangan, ubah ukuran kotak koordinat secara visual, auto grid alignment, serta konfigurasi blueprint lantai.
4. **Alur Validasi Bertingkat & Surat Izin:**
   - Mahasiswa mengajukan booking -> Sekretaris Dekan memvalidasi -> Admin menyetujui -> Pratinjau surat izin peminjaman resmi siap dicetak.

---

## 🛠️ 4. Catatan Teknis & Troubleshooting

* **Jika ikon Run di IDE belum aktif:**
  Klik kanan pada nama project **CampusRoom** di panel Projects → pilih **Build with Dependencies** atau **Resolve Project Problems**.
* **Persyaratan Sistem:**
  - Java Development Kit (JDK) versi 17 atau yang lebih baru.
  - Resolusi layar minimal 1280x720 untuk kenyamanan antarmuka grafis.
