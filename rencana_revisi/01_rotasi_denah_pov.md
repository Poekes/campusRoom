# Spesifikasi Revisi 1: Fitur Rotasi Denah Interaktif (Menyesuaikan POV Pengguna)

> **ID Butir**: REV-01  
> **Komponen**: `InteractiveFloorPlanPanel`, `MainFrame`, `LayoutEditorPanel`  
> **Tujuan**: Memungkinkan pengguna memutar denah lantai (0°, 90°, 180°, 270°) agar sudut pandang (*Point of View* / POV) sesuai dengan orientasi fisik pengguna saat berada di gedung.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Denah visual digambar dalam orientasi sudut tetap (0 derajat / arah utara statis).
- Komponen `InteractiveFloorPlanPanel` hanya mendukung operasi *Pan* (drag klik kanan) dan *Zoom* (0.4x s/d 2.5x).
- Pengguna yang masuk dari arah gedung tertentu (misalnya pintu barat atau selatan) mengalami disorientasi karena peta denah tidak selaras dengan arah pandang mereka.

### Kondisi yang Diharapkan (To-Be)
- Terdapat tombol kontrol rotasi di toolbar ribbon denah:
  - `↺ Putar Kiri (-90°)`
  - `↻ Putar Kanan (+90°)`
  - `0° Reset Orientasi (Default POV)`
  - Label indikator sudut aktif (misal: `POV: 90°`).
- Canvas denah berputar mulus 90°, 180°, atau 270° terhadap titik pusat canvas.
- Seluruh interaksi mouse (hover ruangan, seleksi klik, drag-and-drop, resize handle) **tetap berfungsi 100% presisi** dengan menerapkan transformasi balik (*inverse affine transform*) dari posisi mouse ke koordinat logis ruangan.
- Teks nama dan kode ruangan di dalam kotak denah dapat dibaca dengan nyaman tanpa orientasi terbalik yang membingungkan.

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `InteractiveFloorPlanPanel.java`

1. **State Rotasi**:
   ```java
   private int rotationDegrees = 0; // Nilai yang valid: 0, 90, 180, 270
   ```
2. **Method Pengontrol Rotasi**:
   ```java
   public int getRotationDegrees() {
       return rotationDegrees;
   }

   public void setRotationDegrees(int degrees) {
       this.rotationDegrees = ((degrees % 360) + 360) % 360; // Normalisasi ke [0, 90, 180, 270]
       updateCanvasDimension();
       repaint();
       fireRotationChanged(this.rotationDegrees);
   }

   public void rotateClockwise() {
       setRotationDegrees(rotationDegrees + 90);
   }

   public void rotateCounterClockwise() {
       setRotationDegrees(rotationDegrees - 90);
   }

   public void resetRotation() {
       setRotationDegrees(0);
   }
   ```
3. **Transformasi Rendering di `paintComponent(Graphics g)`**:
   - Hitung titik tengah logis canvas `(cx, cy)`.
   - Gunakan matriks transformasi `Graphics2D`:
     ```java
     Graphics2D g2 = (Graphics2D) g.create();
     // Setup antialiasing
     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
     
     // 1. Zoom scaling
     g2.scale(zoomLevel, zoomLevel);

     // 2. Rotasi seputar pusat gambar/denah
     int unscaledW = (int) Math.ceil(getWidth() / zoomLevel);
     int unscaledH = (int) Math.ceil(getHeight() / zoomLevel);
     double cx = unscaledW / 2.0;
     double cy = unscaledH / 2.0;

     if (rotationDegrees != 0) {
         g2.rotate(Math.toRadians(rotationDegrees), cx, cy);
     }

     // Lakukan penggambaran blueprint & drawRoomBox
     // ...
     g2.dispose();
     ```

4. **Konversi Koordinat Mouse (Inverse Coordinate Transformation)**:
   - Koordinat layar dari `MouseEvent e.getPoint()` harus dibalik dengan transformasi rotasi dan skala:
   ```java
   private Point screenToLogical(Point screenPoint) {
       double unscaledX = screenPoint.x / zoomLevel;
       double unscaledY = screenPoint.y / zoomLevel;

       int unscaledW = (int) Math.ceil(getWidth() / zoomLevel);
       int unscaledH = (int) Math.ceil(getHeight() / zoomLevel);
       double cx = unscaledW / 2.0;
       double cy = unscaledH / 2.0;

       if (rotationDegrees == 0) {
           return new Point((int) Math.round(unscaledX), (int) Math.round(unscaledY));
       }

       // Rotasi terbalik (-rotationDegrees) terhadap (cx, cy)
       double rad = Math.toRadians(-rotationDegrees);
       double dx = unscaledX - cx;
       double dy = unscaledY - cy;

       double rotX = dx * Math.cos(rad) - dy * Math.sin(rad) + cx;
       double rotY = dx * Math.sin(rad) + dy * Math.cos(rad) + cy;

       return new Point((int) Math.round(rotX), (int) Math.round(rotY));
   }
   ```
   - Gunakan `screenToLogical(e.getPoint())` pada:
     - `handleMouseMoved(MouseEvent e)`
     - `handleMousePressed(MouseEvent e)`
     - `handleMouseDragged(MouseEvent e)`
     - `findRoomAt(int x, int y)`

### B. Modifikasi Toolbar Navigasi di `MainFrame.java` & `LayoutEditorPanel.java`

- Pada `createFloorPlanFilterCard()` di ROW 2 (berdampingan dengan kontrol Zoom), tambahkan capsule kontrol POV:
  - Tombol `↺` (Tooltip: "Putar Denah 90° Berlawanan Jarum Jam")
  - Label chip `POV: 0°` (dapat diklik untuk reset)
  - Tombol `↻` (Tooltip: "Putar Denah 90° Searah Jarum Jam")
- Shortcut keyboard:
  - `Ctrl + R`: Putar denah searah jarum jam (+90°).
  - `Ctrl + Shift + R`: Reset orientasi denah ke 0°.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-01-1**: Mengklik tombol `↻` mengubah sudut denah dari 0° -> 90° -> 180° -> 270° -> 0°.
2. **AC-01-2**: Pada sudut rotasi 90°, 180°, dan 270°, mengarahkan kursor mouse ke sebuah ruangan tetap memicu highlight hover dan tooltip yang sesuai dengan ruangan tersebut tanpa meleset.
3. **AC-01-3**: Mengklik ruangan pada denah yang sedang diputar tetap memilih ruangan dengan benar dan membuka detail di sidebar kanan.
4. **AC-01-4**: Pada Layout Editor, menggeser ruangan (*dragging*) pada orientasi berputar tetap menggerakkan ruangan sesuai arah pergerakan kursor logis.
5. **AC-01-5**: Tombol Reset atau chip sudut mengembalikan orientasi denah ke 0° secara instan.
