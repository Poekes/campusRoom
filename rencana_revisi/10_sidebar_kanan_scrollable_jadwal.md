# Spesifikasi Revisi 10: Sidebar Kanan Scrollable Jadwal Ruangan

> **ID Butir**: REV-10  
> **Komponen**: `MainFrame`, `InteractiveFloorPlanPanel`, `ScheduleService`, `BookingService`  
> **Tujuan**: Memperkaya dock inspector di sebelah kanan denah (`roomDetailSidebar`) dengan menambahkan daftar jadwal perkuliahan terintegrasi yang dapat digulir (*scrollable container*). Ketika pengguna mengklik salah satu ruangan di denah, sidebar langsung menampilkan informasi agenda dan jadwal sesi kuliah yang sudah ada di ruangan tersebut secara terstruktur dan rapi.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Ketika sebuah ruangan diklik di denah (`InteractiveFloorPlanPanel`), sidebar inspector kanan (`roomDetailSidebar`) melebar secara otomatis dari lebar 0 menjadi 315 px.
- Namun, isi di dalam sidebar saat ini sangat terbatas:
  1. Header judul dan status banner (Tersedia / Terpakai).
  2. Empat label teks statis: Kapasitas, Lantai, Fasilitas, dan Penggunaan Saat Ini (hanya satu baris keterangan singkat).
  3. Dua tombol di bagian bawah: "Ajukan Booking Ruangan" dan "Detail & Jadwal Lengkap".
- Tidak ada daftar agenda kegiatan atau sesi perkuliahan di ruangan tersebut. Pengguna yang ingin mengetahui jam berapa saja ruangan tersebut terpakai pada hari yang dipilih terpaksa harus membuka dialog terpisah, mengganggu alur visual denah (*broken visual flow*).

### Kondisi yang Diharapkan (To-Be)
- Rombak tata letak `roomDetailSidebar` pada `MainFrame.java` menjadi dock modern kaya informasi yang dilengkapi kontainer **Jadwal Ruangan Scrollable**:
  1. **Bagian Atas (Header & Atribut Ringkas)**:
     - Kode & Nama Ruangan (misal: `R. 201 • Ruang Teori Grafika`).
     - Badge Ketersediaan Sesi yang dipilih (Hijau: Kosong, Merah: Terpakai).
     - Atribut kunci dalam chip ringkas: `👥 40 Kursi` • `📍 Lantai 2` • `🛠️ AC, LCD, Wi-Fi`.
  2. **Bagian Tengah (Scrollable Schedule Feed / Kartu Agenda)**:
     - Judul sub-seksi: `📅 JADWAL RUANGAN HARI INI` (menyesuaikan tanggal filter yang dipilih).
     - Komponen `JScrollPane` (lebar ~295px, tinggi fleksibel mengikuti jendela) dengan unit increment scrolling yang halus.
     - Di dalam scroll pane, render kartu-kartu jadwal perkuliahan yang berlangsung di ruangan tersebut:
       - **Kartu Jadwal Kuliah Reguler**:
         - Jam: `08:00 - 10:30`
         - Mata Kuliah: `Pemrograman Berorientasi Objek`
         - Dosen: `Dr. Ir. Hendra Saputra, M.T.`
         - Kelas: `Kelas TI-2A`
         - Badge tipe: `[KULIAH REGULER]` (warna biru lembut)
       - **Kartu Booking Kelas Pengganti / Khusus**:
         - Jam: `13:00 - 15:30`
         - Kegiatan: `Kelas Pengganti Sistem Operasi`
         - PJ: `Ahmad Fauzi (250111019)`
         - Badge tipe: `[PEMINJAMAN PJ]` (warna amber)
     - **Empty State**: Jika ruangan tidak memiliki jadwal sama sekali pada hari itu, tampilkan kartu ilustrasi:
       *"✨ Tidak ada jadwal perkuliahan pada tanggal ini. Ruangan bebas digunakan sepanjang hari."*
  3. **Bagian Bawah (Action Buttons)**:
     - Tombol "Ajukan Booking Ruangan Ini" (aktif jika sesi kosong).
     - Tombol "Lihat Semua Hari (Seminggu Penuh)" yang membuka `RoomDetailDialog`.

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `createRoomDetailSidebar()` di `MainFrame.java`

1. **Struktur Tata Letak Vertikal**:
   ```java
   private JPanel createRoomDetailSidebar() {
       roomDetailSidebar = new JPanel(new BorderLayout(0, 10));
       roomDetailSidebar.setPreferredSize(new Dimension(isInspectorVisible ? INSPECTOR_WIDTH : 0, 500));
       // Background putih, border light, rounded corner

       // 1. NORTH: Top Header & Badges
       // (Pertahankan tombol close ✕, judul ruangan, badge status, chip kapasitas/lantai)

       // 2. CENTER: CardLayout ("empty" vs "spec_with_schedules")
       inspectorBodyContainer = new JPanel(inspectorBodyCardLayout);
       inspectorBodyContainer.add(createInspectorEmptyStatePanel(), "empty");
       inspectorBodyContainer.add(createInspectorScheduleView(), "spec");

       // 3. SOUTH: Action Buttons ("Ajukan Booking" & "Detail Semua Hari")
       // ...
       return roomDetailSidebar;
   }
   ```

2. **Implementasi `createInspectorScheduleView()`**:
   ```java
   private JPanel pnlScheduleListContainer;
   private JScrollPane scrollScheduleList;

   private JPanel createInspectorScheduleView() {
       JPanel panel = new JPanel(new BorderLayout(0, 8));
       panel.setOpaque(false);

       // Ringkasan Atribut Ruangan (Kapasitas, Lantai, Fasilitas)
       JPanel pnlAttrs = new JPanel(new GridLayout(2, 2, 4, 4));
       // lblCapacity, lblFloor, lblFacilities
       panel.add(pnlAttrs, BorderLayout.NORTH);

       // Header Agenda
       JPanel agendaHeader = new JPanel(new BorderLayout());
       agendaHeader.setOpaque(false);
       JLabel lblAgendaTitle = new JLabel("📅 AGENDA & JADWAL RUANGAN");
       lblAgendaTitle.setFont(ModernUIHelper.fontBold(10));
       lblAgendaTitle.setForeground(new Color(71, 85, 105));
       agendaHeader.add(lblAgendaTitle, BorderLayout.WEST);

       // Container Daftar Jadwal (BoxLayout Y_AXIS)
       pnlScheduleListContainer = new JPanel();
       pnlScheduleListContainer.setLayout(new BoxLayout(pnlScheduleListContainer, BoxLayout.Y_AXIS));
       pnlScheduleListContainer.setBackground(Color.WHITE);

       scrollScheduleList = new JScrollPane(pnlScheduleListContainer);
       scrollScheduleList.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
       scrollScheduleList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
       scrollScheduleList.getVerticalScrollBar().setUnitIncrement(14);

       JPanel centerWrapper = new JPanel(new BorderLayout(0, 4));
       centerWrapper.setOpaque(false);
       centerWrapper.add(agendaHeader, BorderLayout.NORTH);
       centerWrapper.add(scrollScheduleList, BorderLayout.CENTER);

       panel.add(centerWrapper, BorderLayout.CENTER);
       return panel;
   }
   ```

3. **Memuat Jadwal Ruangan secara Dinamis saat `onRoomSelected(Room room)`**:
   ```java
   private void updateSidebarScheduleFeed(Room room) {
       pnlScheduleListContainer.removeAll();

       if (room == null) {
           pnlScheduleListContainer.revalidate();
           pnlScheduleListContainer.repaint();
           return;
       }

       LocalDate date = getSelectedDate();
       String dayIndo = AvailabilityService.convertDayOfWeekToIndonesian(date.getDayOfWeek());

       // 1. Ambil Jadwal Reguler hari ini
       List<RegularSchedule> regularToday = scheduleService.getSchedulesByRoomAndDay(room.getId(), dayIndo);

       // 2. Ambil Booking Disetujui hari ini
       List<Booking> bookingsToday = bookingService.getBookingsByRoomAndDate(room.getId(), date.toString());
       List<Booking> approvedBookings = new ArrayList<>();
       for (Booking b : bookingsToday) {
           if ("DISETUJUI".equalsIgnoreCase(b.getStatus())) {
               approvedBookings.add(b);
           }
       }

       if (regularToday.isEmpty() && approvedBookings.isEmpty()) {
           // Tampilkan Empty State Tile
           pnlScheduleListContainer.add(createEmptyScheduleTile(dayIndo, date));
       } else {
           // Tambahkan kartu jadwal reguler
           for (RegularSchedule s : regularToday) {
               pnlScheduleListContainer.add(createScheduleCardItem(
                   s.getStartTime() + " - " + s.getEndTime(),
                   s.getCourseName(),
                   s.getLecturerName() + " (" + s.getClassName() + ")",
                   "KULIAH REGULER",
                   new Color(224, 242, 254), new Color(3, 105, 161)
               ));
               pnlScheduleListContainer.add(Box.createVerticalStrut(6));
           }

           // Tambahkan kartu booking disetujui
           for (Booking b : approvedBookings) {
               pnlScheduleListContainer.add(createScheduleCardItem(
                   b.getStartTime() + " - " + b.getEndTime(),
                   b.getCourseName(),
                   b.getLecturerName() + " • PJ: " + b.getStudentName(),
                   "BOOKING RESMI",
                   new Color(254, 243, 199), new Color(180, 83, 9)
               ));
               pnlScheduleListContainer.add(Box.createVerticalStrut(6));
           }
       }

       pnlScheduleListContainer.revalidate();
       pnlScheduleListContainer.repaint();
   }
   ```

4. **Desain Komponen Item Kartu Jadwal (`createScheduleCardItem`)**:
   - Kartu kecil dengan background lembut, garis batas halus, teks jam kuliah tebal, nama mata kuliah, pengampu, serta badge kategori sesi.
   - Efek hover lembut saat mouse melintas di atas kartu jadwal.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-10-1**: Saat sebuah ruangan diklik di denah, sidebar kanan terbuka dan otomatis memuat daftar seluruh perkuliahan yang terjadwal di ruangan tersebut untuk tanggal/hari yang sedang aktif.
2. **AC-10-2**: Jika ruangan memiliki lebih dari 3 jadwal sehingga melebihi tinggi layar, scrollbar vertikal muncul dan dapat digulir dengan mulus menggunakan mouse wheel.
3. **AC-10-3**: Kartu jadwal menampilkan jam sesi, mata kuliah, dosen pengampu, kelas, dan penanda tipe sesi (Reguler vs Booking Resmi) dengan kontras warna yang jelas.
4. **AC-10-4**: Jika ruangan tidak memiliki jadwal pada hari tersebut, ditampilkan kartu pesan ramah: *"Tidak ada jadwal perkuliahan pada hari ini. Ruangan bebas digunakan."*
5. **AC-10-5**: Mengubah tanggal atau sesi waktu pada ribbon atas denah otomatis memperbarui daftar jadwal di sidebar secara real-time.
