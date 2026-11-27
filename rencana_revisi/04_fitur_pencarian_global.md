# Spesifikasi Revisi 4: Sistem Pencarian Global & Filter Responsif

> **ID Butir**: REV-04  
> **Komponen**: `MainFrame`, `InteractiveFloorPlanPanel`, `SecretaryPanel`, `StudentBookingPanel`, `AdminPanel`, `ModernUIHelper`  
> **Tujuan**: Menyediakan fitur pencarian cepat (*instant responsive search*) di seluruh modul aplikasi: pencarian & penyorotan ruangan langsung di denah interaktif, serta pencarian berbasis tabel dengan `TableRowSorter`.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Fitur pencarian hanya tersedia secara parsial di `RoomCatalogPanel` (Katalog Ruangan).
- Pada **Denah Interaktif (`InteractiveFloorPlanPanel`)**: Tidak ada fasilitas pencarian. Pengguna harus menyisir denah satu per satu untuk menemukan ruangan tertentu (misal: "R. 204" atau "Lab Jaringan").
- Pada **Portal Sekretaris**: Tabel pengajuan dan tabel jadwal tidak memiliki kolom pencarian, menyulitkan sekdek saat jumlah data permohonan mencapai puluhan.
- Pada **Riwayat Mahasiswa & Admin**: Tidak ada filter pencarian cepat.

### Kondisi yang Diharapkan (To-Be)
1. **Pencarian Denah Cepat (*Quick Floor Plan Finder*)**:
   - Terdapat kolom input pencarian di ribbon atas denah: `🔍 Cari Ruangan (Kode / Nama)...`.
   - Mengetik kode (misal `201`) atau nama (misal `Teori`):
     - Kotak ruangan yang cocok otomatis diberi efek highlight menyala (*pulsing gold border*).
     - Denah otomatis melakukan *pan / scroll* memusatkan pandangan ke ruangan tersebut.
     - Ruangan terpilih otomatis dan detail ruangan di sidebar kanan langsung terbuka.
2. **Pencarian Instan pada Seluruh Tabel dengan `TableRowSorter`**:
   - Menyediakan komponen filter instan menggunakan `DocumentListener` pada `JTextField` pencarian di:
     - `SecretaryPanel`: Filter berdasarkan NIM, Nama Mahasiswa, Kode Booking, Mata Kuliah, Dosen.
     - `StudentBookingPanel`: Filter berdasarkan Kode Booking, Ruangan, Tanggal, Status.
     - `AdminPanel`: Filter pada Master Ruangan (Kode, Nama, Fasilitas, Lantai) dan Approval.
   - Hasil pencarian muncul secara *realtime* (setiap ketukan keyboard) tanpa perlu menekan tombol Enter atau reload database.

---

## 2. Rincian Teknis Implementasi

### A. Fitur Pencarian pada Denah (`MainFrame.java` & `InteractiveFloorPlanPanel.java`)

1. **Tambahkan Input Pencarian di Ribbon Denah (`createFloorPlanFilterCard`)**:
   ```java
   JTextField txtSearchRoom = new JTextField(12);
   txtSearchRoom.putClientProperty("JTextField.placeholderText", "🔍 Cari ruangan...");
   ModernUIHelper.styleTextField(txtSearchRoom);
   
   txtSearchRoom.getDocument().addDocumentListener(new DocumentListener() {
       @Override
       public void insertUpdate(DocumentEvent e) { performRoomSearch(txtSearchRoom.getText().trim()); }
       @Override
       public void removeUpdate(DocumentEvent e) { performRoomSearch(txtSearchRoom.getText().trim()); }
       @Override
       public void changedUpdate(DocumentEvent e) { performRoomSearch(txtSearchRoom.getText().trim()); }
   });
   ```

2. **Logika Pencarian Ruangan (`performRoomSearch`)**:
   ```java
   private void performRoomSearch(String query) {
       if (query.isEmpty() || currentRooms == null) {
           floorCanvas.setSearchHighlightRoom(null);
           return;
       }
       String lower = query.toLowerCase();
       Room matched = null;
       for (Room r : currentRooms) {
           if (r.getCode().toLowerCase().contains(lower) || r.getName().toLowerCase().contains(lower)) {
               matched = r;
               break;
           }
       }
       if (matched != null) {
           floorCanvas.setSearchHighlightRoom(matched);
           floorCanvas.setSelectedRoom(matched);
           onRoomSelected(matched);
           // Pusatkan viewport scroll ke ruangan tersebut
           centerCanvasOnRoom(matched);
       }
   }
   ```

3. **Rendering Highlight pada `InteractiveFloorPlanPanel.java`**:
   - Jika `r == searchHighlightRoom`, gambar border berkilau (*Glowing Amber Halo*) dan animasi penanda lokasi (misal ikon pin lokasi `📍` di atas box).

### B. Pola Standardisasi Pencarian Tabel Menggunakan `TableRowSorter`

Tambahkan helper method terstandarisasi di `ModernUIHelper.java`:
```java
public static void attachInstantTableSearch(JTextField searchField, JTable table, int... columnIndices) {
    DefaultTableModel model = (DefaultTableModel) table.getModel();
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    table.setRowSorter(sorter);

    searchField.getDocument().addDocumentListener(new DocumentListener() {
        private void updateFilter() {
            String text = searchField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), columnIndices));
            }
        }
        @Override
        public void insertUpdate(DocumentEvent e) { updateFilter(); }
        @Override
        public void removeUpdate(DocumentEvent e) { updateFilter(); }
        @Override
        public void changedUpdate(DocumentEvent e) { updateFilter(); }
    });
}
```

Terapkan helper ini di:
- `SecretaryPanel.java` untuk `tblPending` dan `tblSchedules`.
- `StudentBookingPanel.java` untuk `tblBookings`.
- `AdminPanel.java` untuk `tblAdminApproval` dan `tblRooms`.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-04-1**: Mengetik kode ruangan (misal `201`) pada search bar denah langsung menyorot ruangan R. 201 di canvas dan membuka datanya di sidebar kanan.
2. **AC-04-2**: Mengetik kata kunci di kolom pencarian tabel Sekretaris langsung menyaring baris tabel secara instan (kurang dari 50ms) tanpa jeda reload database.
3. **AC-04-3**: Menghapus teks pada kolom pencarian mengembalikan seluruh data tabel ke kondisi semula.
4. **AC-04-4**: Pencarian bersifat *case-insensitive* (mengetik `r. 201`, `R. 201`, atau `r.201` memberikan hasil yang konsisten).
5. **AC-04-5**: Pencarian tidak menyebabkan eror regex jika pengguna memasukkan karakter khusus seperti tanda kurung `()`, titik `.`, atau minus `-`.
