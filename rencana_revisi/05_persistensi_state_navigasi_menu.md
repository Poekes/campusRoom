# Spesifikasi Revisi 5: Persistensi State UI Saat Berpindah Menu Navigasi

> **ID Butir**: REV-05  
> **Komponen**: `MainFrame`, `SidebarNavigationPanel`, `InteractiveFloorPlanPanel`, `SecretaryPanel`, `AdminPanel`  
> **Tujuan**: Mencegah hilangnya status kerja pengguna (*prevent UI state loss*) ketika berpindah antar menu di sidebar navigasi. Mempertahankan pilihan filter gedung/lantai, ruangan terpilih, teks pencarian, seleksi baris tabel, dan posisi scroll.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Pada `MainFrame.java`, method `switchToView(int navIndex)` secara agresif memanggil `refreshAvailability()`, `secretaryPanel.refreshData()`, dan `adminPanel.refreshData()` setiap kali user berpindah tab pada `CardLayout`.
- Pemanggilan refresh data yang tidak perlu ini menimbulkan efek samping negatif:
  1. Di **Denah Interaktif**: Memanggil `floorCanvas.setRooms(...)` yang secara internal mengeksekusi `this.selectedRoom = null`, menyebabkan ruangan yang sebelumnya sedang dilihat pengguna tertutup dan sidebar detail ter-reset ke *empty state*.
  2. Pilihan Gedung dan Lantai pada Denah dapat tertimpa atau tersinkronisasi ulang secara sepihak dari editor denah via `syncLocationFromEditor()`.
  3. Di **Tabel Data**: Seluruh seleksi baris (`tblPending.getSelectedRow()`, `tblRooms.getSelectedRow()`) hilang, scrollbar kembali ke posisi paling atas, dan kata kunci yang sedang diketik di filter pencarian bisa hilang.

### Kondisi yang Diharapkan (To-Be)
- Saat pengguna berpindah menu (misal: Denah -> Riwayat Booking -> kembali lagi ke Denah):
  1. **Denah**: Posisi canvas, level zoom, sudut rotasi, gedung/lantai terpilih, serta ruangan yang sedang diseleksi dan informasi di sidebar kanan **tetap bertahan persis seperti sebelum berpindah**.
  2. **Tabel Data**: Baris tabel yang sedang dipilih, query teks pencarian yang sudah diketik, dan posisi scrollbar tidak hilang.
  3. Data hanya diperbarui (*refreshed*) ketika terjadi mutasi data nyata (seperti setelah berhasil booking, tambah jadwal, ubah status approval, atau ketika tombol "Muat Ulang" ditekan manual).

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `MainFrame.java`

1. **Selektif Refresh pada `switchToView`**:
   - Jangan panggil `refreshAvailability()` atau `refreshData()` jika data tidak mengalami mutasi:
   ```java
   public void switchToView(int navIndex) {
       // Validasi otorisasi peran
       // ...

       if (sidebarNav != null) {
           sidebarNav.selectItem(navIndex, false);
       }

       switch (navIndex) {
           case SidebarNavigationPanel.NAV_FLOOR_PLAN:
               cardLayout.show(cardPanel, "floorPlan");
               lblBreadcrumbView.setText("Denah Visual Interaktif");
               lblViewSubtitle.setText("Peta visual ruangan realtime dan status ketersediaan per slot sesi kuliah");
               // JANGAN panggil syncLocationFromEditor() atau refreshAvailability() tanpa alasan!
               // Hanya refresh jika flag dataDirty == true
               if (isFloorPlanDirty) {
                   refreshAvailability();
                   isFloorPlanDirty = false;
               }
               break;

           case SidebarNavigationPanel.NAV_CATALOG:
               cardLayout.show(cardPanel, "catalog");
               lblBreadcrumbView.setText("Katalog Ruangan");
               lblViewSubtitle.setText("Daftar lengkap seluruh inventaris ruangan kuliah...");
               break;

           case SidebarNavigationPanel.NAV_BOOKING:
               cardLayout.show(cardPanel, "booking");
               lblBreadcrumbView.setText("Pengajuan Saya");
               lblViewSubtitle.setText("Portal permohonan peminjaman ruangan kuliah...");
               if (isStudentBookingDirty) {
                   studentBookingPanel.refreshData();
                   isStudentBookingDirty = false;
               }
               break;

           // Begitu pula untuk case NAV_SEC_VALIDATION, NAV_SEC_SCHEDULE, NAV_ADMIN_*, dll.
       }
   }
   ```

2. **Flag Pelacak Mutasi Data (*Dirty State Tracker*)**:
   - Deklarasikan flag boolean:
     ```java
     private boolean isFloorPlanDirty = false;
     private boolean isStudentBookingDirty = false;
     private boolean isSecretaryDirty = false;
     private boolean isAdminDirty = false;
     ```
   - Tandai `isDirty = true` hanya saat ada operasi penyimpanan, persetujuan, atau perubahan master data.

### B. Modifikasi `InteractiveFloorPlanPanel.java`

Ubah method `setRooms(List<Room> rooms)` agar mempertahankan ruangan yang sedang dipilih jika masih ada dalam daftar baru:
```java
public void setRooms(List<Room> newRooms) {
    int previouslySelectedId = (this.selectedRoom != null) ? this.selectedRoom.getId() : -1;
    this.rooms = (newRooms != null) ? newRooms : new ArrayList<>();
    
    // Pulihkan referensi selectedRoom jika ID yang sama ditemukan di list baru
    Room matched = null;
    if (previouslySelectedId != -1) {
        for (Room r : this.rooms) {
            if (r.getId() == previouslySelectedId) {
                matched = r;
                break;
            }
        }
    }
    this.selectedRoom = matched;
    this.hoveredRoom = null;
    
    updateCanvasDimension();
    repaint();
}
```

### C. Retensi State pada Panel Tabel

Pada `SecretaryPanel`, `StudentBookingPanel`, dan `AdminPanel`:
- Simpan query pencarian di variabel instance `String currentFilterText = ""`.
- Saat `refreshData()` dieksekusi, pulihkan teks pada field pencarian dan baris yang dipilih:
```java
int selectedId = getSelectedRowId();
// reload data model
restoreSelectedRowById(selectedId);
```

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-05-1**: Pengguna memilih ruangan "R. 201" pada denah lantai 2, kemudian berpindah ke menu "Katalog Ruangan", lalu kembali lagi ke menu "Denah". Ruangan "R. 201" tetap berstatus terpilih (*selected*) dan sidebar detail kanan tetap menampilkan data R. 201.
2. **AC-05-2**: Pengguna mengetik kata kunci pencarian pada tabel permohonan mahasiswa, berpindah ke menu lain, dan kembali. Teks pencarian dan hasil filter tabel tidak terhapus.
3. **AC-05-3**: Level zoom dan orientasi rotasi denah tidak kembali ke default saat berpindah-pindah menu.
4. **AC-05-4**: Sinkronisasi lokasi antara editor dan denah hanya terjadi saat pengguna secara eksplisit menekan simpan layout atau sengaja memicu sinkronisasi.
