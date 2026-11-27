# Spesifikasi Revisi 9: Penyempurnaan UX & Alur Interaktif Edit Ruangan

> **ID Butir**: REV-09  
> **Komponen**: `AdminPanel`, `LayoutEditorPanel`, `RoomInspectorDialog`, `MasterDataService`  
> **Tujuan**: Memperbaiki dan menyederhanakan antarmuka serta pengalaman pengguna (*User Experience* / UX) pada fitur pengeditan ruangan. Menghilangkan kebingungan pengguna melalui penambahan tombol aksi eksplisit pada tabel master data, panduan visual interaktif di canvas editor denah, dan pelabelan koordinat yang intuitif.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
1. **Di `AdminPanel.java` (Master Ruangan)**:
   - Pada tabel master ruangan, hanya ada tombol `Hapus Ruangan` dan `+ Tambah Ruangan`. **Sama sekali tidak ada tombol "Edit Ruangan"**!
   - Pengguna bingung bagaimana cara mengubah data nama, tipe, fasilitas, atau kapasitas ruangan yang sudah terdaftar.
2. **Di `LayoutEditorPanel.java` (Visual Layout Editor)**:
   - Tidak ada panduan cara mengedit ruangan (*Zero Onboarding*). Pengguna tidak mengetahui bahwa:
     - Menggeser posisi dilakukan dengan klik kiri lalu drag kotak ruangan.
     - Mengubah ukuran dilakukan dengan menarik sudut oranye di kanan bawah kotak.
     - Mengedit data properti dilakukan dengan klik ganda (*double click*) atau menekan tombol Inspector di toolbar.
   - Pada dialog `RoomInspectorDialog`: Bagian koordinat geometris menampilkan label teknis seperti `Posisi X: 360`, `Posisi Y: 200`, `Lebar (W): 180`, `Tinggi (H): 140` yang terasa kaku dan membingungkan bagi pengguna non-programmer.

### Kondisi yang Diharapkan (To-Be)
1. **Penyempurnaan pada Tabel Master Ruangan (`AdminPanel.java`)**:
   - Tambahkan tombol eksplisit: `✏️ Edit Data Ruangan` di toolbar tabel.
   - Tambahkan event klik ganda (*double-click*) pada baris tabel ruangan untuk langsung membuka formulir edit ruangan.
   - Tampilkan form edit dialog dengan data yang sudah terisi (*pre-populated*) dan validasi perubahan kode ruangan yang aman.
2. **Penyempurnaan pada Visual Layout Editor (`LayoutEditorPanel.java`)**:
   - **Interactive Quick-Guide Banner**: Tampilkan bilah panduan visual interaktif di bawah toolbar:
     *💡 Panduan Editor Denah: (1) Klik ruangan untuk memilih → (2) Geser langsung untuk memindahkan posisi → (3) Tarik sudut oranye kanan bawah untuk ubah ukuran → (4) Klik ganda untuk buka dialog properti.*
   - **Floating Action Overlay pada Ruangan Terpilih**:
     Saat sebuah ruangan dipilih di denah, tampilkan floating pill mini langsung di atas kotak ruangan:
     `[✏️ Edit Properti]` `[📋 Duplikat]` `[🗑️ Hapus]`
   - **Penyederhanaan Label `RoomInspectorDialog`**:
     - Ubah `Posisi X` -> `Jarak dari Kiri (X) px`
     - Ubah `Posisi Y` -> `Jarak dari Atas (Y) px`
     - Ubah `Lebar (W)` -> `Lebar Ruangan (px)`
     - Ubah `Tinggi (H)` -> `Panjang / Kedalaman Ruangan (px)`
     - Sertakan petunjuk visual rasio standar (misal: "Rasio Standar: 180 × 140 px").

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `AdminPanel.java`

1. **Tambahkan Tombol Edit Ruangan**:
   ```java
   JButton btnEditRoom = ModernUIHelper.createStyledButton("✏️ Edit Ruangan", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
   btnEditRoom.addActionListener(e -> handleEditRoom());
   actions.add(btnDeleteRoom);
   actions.add(btnEditRoom);
   actions.add(btnAddRoom);
   ```

2. **Dukungan Double-Click pada Baris Tabel**:
   ```java
   tblRooms.addMouseListener(new MouseAdapter() {
       @Override
       public void mouseClicked(MouseEvent e) {
           if (e.getClickCount() == 2 && tblRooms.getSelectedRow() != -1) {
               handleEditRoom();
           }
       }
   });
   ```

3. **Implementasi `handleEditRoom()`**:
   ```java
   private void handleEditRoom() {
       int row = tblRooms.getSelectedRow();
       if (row == -1) {
           JOptionPane.showMessageDialog(this, "Pilih salah satu baris ruangan yang ingin diedit.", "Peringatan", JOptionPane.WARNING_MESSAGE);
           return;
       }
       int roomId = (int) modelRooms.getValueAt(row, 0);
       Room room = masterDataService.getRoomById(roomId);
       if (room == null) return;

       showEditRoomDialog(room);
   }

   private void showEditRoomDialog(Room room) {
       // Buat form dialog dengan textfield terisi: Kode, Nama, Lantai, Kapasitas, Tipe, Fasilitas
       // Saat disubmit, panggil masterDataService.updateRoom(room)
       // Refresh tabel dan trigger onDataChangedCallback.run()
   }
   ```

### B. Modifikasi `LayoutEditorPanel.java`

1. **Tambahkan Hint Banner di Bawah Header Island**:
   ```java
   private JPanel createEditorHintBar() {
       JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
       hintBar.setBackground(new Color(238, 242, 255)); // Indigo 50
       hintBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(199, 210, 254)));

       JLabel lblHint = new JLabel("<html><b>💡 Panduan Interaksi:</b> " +
               "<b>1. Pilih:</b> Klik kotak ruangan &nbsp;•&nbsp; " +
               "<b>2. Geser:</b> Drag mouse untuk reposisi &nbsp;•&nbsp; " +
               "<b>3. Resize:</b> Tarik handle oranye di pojok kanan bawah &nbsp;•&nbsp; " +
               "<b>4. Properti:</b> Klik ganda kotak untuk edit data</html>");
       lblHint.setFont(ModernUIHelper.fontPlain(11));
       lblHint.setForeground(new Color(67, 56, 202));
       hintBar.add(lblHint);
       return hintBar;
   }
   ```

2. **Perjelas Visual Handle Resize di `InteractiveFloorPlanPanel.java`**:
   - Gambar handle sudut kanan bawah dengan kontras tinggi (warna oranye terang bergaris putih tebal) dan kursor mouse berubah menjadi `Cursor.SE_RESIZE_CURSOR` saat mouse mendekati area sudut 14x14 px.
   - Tampilkan tooltip melayang saat kursor berada di atas handle: *"Tarik sudut ini untuk mengubah panjang & lebar ruangan"*.

### C. Modifikasi `RoomInspectorDialog.java`

- Ubah judul dialog menjadi: `⚙️ Pengaturan Data & Dimensi Ruangan`.
- Berikan instruksi jelas pada panel geometri: *"Sesuaikan dimensi kotak denah di bawah ini, atau gunakan tombol preset ukuran standar."*
- Tambahkan tombol preset ukuran cepat:
  - `[📐 Kelas Standar (180x140)]`
  - `[🔬 Laboratorium Besar (260x160)]`
  - `[🏢 Ruang Seminar (200x180)]`

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-09-1**: Di menu Master Ruangan (`AdminPanel`), terdapat tombol "✏️ Edit Ruangan" yang berfungsi membuka dialog pengeditan ruangan.
2. **AC-09-2**: Mengklik ganda (*double-click*) baris ruangan pada tabel Master Ruangan langsung membuka form edit ruangan.
3. **AC-09-3**: Pada Layout Editor, terdapat bilah panduan visual interaktif di bawah toolbar yang menjelaskan 4 langkah interaksi (Pilih, Geser, Resize, Properti).
4. **AC-09-4**: Mengarahkan kursor ke sudut kanan bawah ruangan memunculkan kursor resize dan tooltip panduan yang jelas.
5. **AC-09-5**: Mengubah nama atau fasilitas ruangan pada dialog edit berhasil memperbarui data di database dan langsung tercermin pada denah interaktif.
