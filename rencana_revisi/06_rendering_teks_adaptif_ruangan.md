# Spesifikasi Revisi 6: Rendering Teks Adaptif & Level-of-Detail Ruangan

> **ID Butir**: REV-06  
> **Komponen**: `InteractiveFloorPlanPanel`  
> **Tujuan**: Menerapkan rendering teks adaptif (*Semantic Zoom & Level of Detail / LOD*) pada kotak ruangan di denah. Ketika ukuran ruangan diperkecil, teks detail (tipe, kapasitas, fasilitas, badge panjang) otomatis disederhanakan/dihilangkan dan hanya menyisakan kode ruangan atau nama ruangan agar tidak terjadi tabrakan/pemotongan teks (*text clipping/overlap*).

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Pada `InteractiveFloorPlanPanel.drawRoomBox()`, penggambaran isi ruangan mengasumsikan dimensi kotak yang cukup besar (lebar $\ge 180$, tinggi $\ge 140$ px).
- Elemen yang digambar meliputi:
  1. Header strip hitam (tinggi 28px) dengan kode ruangan font 13pt & badge kapasitas kursi.
  2. Nama ruangan pada `ry + 48` (font 11pt).
  3. Tipe ruangan pada `ry + 64` (font 10pt).
  4. Badge status "TERSEDIA" / "TERPAKAI" (tinggi 22px) di bagian bawah `ry + rh - 30`.
- Jika admin memperkecil ukuran ruangan (misal lebar $90$ px atau tinggi $60$ px) atau pada denah dengan banyak partisi ruangan kecil:
  - Teks saling bertumpuk (*overlapping*).
  - Badge status menabrak teks nama ruangan.
  - Tulisan terpotong jelek di luar batas rounded corner.

### Kondisi yang Diharapkan (To-Be)
- Terapkan **Level-of-Detail (LOD)** dinamis berdasarkan dimensi aktual kotak ruangan (`rw` x `rh`):
  1. **Mode Ringkas / Compact (`rh < 65` atau `rw < 95`)**:
     - Hilangkan header strip hitam, hilangkan tipe ruangan, hilangkan badge status bawah.
     - Hanya gambar **Kode Ruangan** (misal: `R.201`) dengan font tebal yang terpusat (*centered*).
     - Gambar status ketersediaan sebagai titik lingkaran kecil (*status indicator dot* 6px) di pojok kanan atas: Hijau (Tersedia) / Merah (Terpakai).
  2. **Mode Menengah / Medium (`65 <= rh < 105` atau `95 <= rw < 155`)**:
     - Header strip atas ringkas (tinggi 20px) dengan Kode Ruangan.
     - Nama ruangan (maksimal 1 baris, dipotong elipsis `...` jika terlalu panjang).
     - Indikator status berupa strip warna atau pill badge tipis di bawah.
     - Sembunyikan tipe ruangan panjang dan kapasitas detail.
  3. **Mode Penuh / Full (`rh >= 105` dan `rw >= 155`)**:
     - Gambar seluruh elemen lengkap: Header strip 28px, Badge Kapasitas, Nama Ruangan, Tipe Ruangan, serta Badge Status Solid "TERSEDIA" / "TERPAKAI".
- Perhitungan ukuran font adaptif yang mempertimbangkan `FontMetrics` agar teks tidak pernah overflow.

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `InteractiveFloorPlanPanel.java`

Ubah logika di dalam method `drawRoomBox(Graphics2D g2, Room r)`:

```java
private void drawRoomBox(Graphics2D g2, Room r) {
    int rx = r.getPosX();
    int ry = r.getPosY();
    int rw = r.getWidth();
    int rh = r.getHeight();

    boolean isHovered = (r == hoveredRoom);
    boolean isSelected = (r == selectedRoom);
    boolean isAvail = r.isAvailable();

    // 1. Gambar Soft Shadow & Background Kotak (Elevasi Modern)
    // ... (pertahankan kode shadow eksisting)

    // 2. Gambar Background Utama Ruangan (Hijau vs Merah)
    Color fillColor = isAvail ? COLOR_AVAILABLE_BG : COLOR_OCCUPIED_BG;
    g2.setColor(fillColor);
    g2.fillRoundRect(rx, ry, rw, rh, 12, 12);

    // 3. Gambar Border Focus / Hover
    // ... (pertahankan kode border eksisting)

    // ==========================================
    // 4. LEVEL OF DETAIL (LOD) RENDERING TEKS
    // ==========================================

    if (rh < 65 || rw < 95) {
        // --- TIER 1: COMPACT / TINY ROOM ---
        renderCompactRoomContent(g2, r, rx, ry, rw, rh, isAvail);
    } else if (rh < 105 || rw < 155) {
        // --- TIER 2: MEDIUM ROOM ---
        renderMediumRoomContent(g2, r, rx, ry, rw, rh, isAvail);
    } else {
        // --- TIER 3: FULL / LARGE ROOM ---
        renderFullRoomContent(g2, r, rx, ry, rw, rh, isAvail);
    }

    // 5. Gambar Drag & Resize Handles jika layoutEditMode aktif
    // ...
}
```

### B. Implementasi Tiga Tingkatan Renderer

```java
private void renderCompactRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
    // 1. Status Dot di pojok kanan atas
    int dotSize = 8;
    g2.setColor(isAvail ? new Color(52, 211, 153) : new Color(248, 113, 113));
    g2.fillOval(rx + rw - dotSize - 6, ry + 6, dotSize, dotSize);
    g2.setColor(Color.WHITE);
    g2.setStroke(new BasicStroke(1.0f));
    g2.drawOval(rx + rw - dotSize - 6, ry + 6, dotSize, dotSize);

    // 2. Kode Ruangan di Tengah Kotak
    g2.setColor(Color.WHITE);
    g2.setFont(ModernUIHelper.fontBold(Math.max(10, Math.min(13, rh / 4))));
    FontMetrics fm = g2.getFontMetrics();
    String code = r.getCode();
    int textW = fm.stringWidth(code);
    int textX = rx + Math.max(4, (rw - textW) / 2);
    int textY = ry + ((rh - fm.getHeight()) / 2) + fm.getAscent();
    g2.drawString(code, textX, textY);
}

private void renderMediumRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
    // 1. Header Strip Minimalis
    int headerH = 22;
    g2.setColor(new Color(15, 23, 42, 160));
    g2.fillRoundRect(rx, ry, rw, headerH, 12, 12);
    g2.fillRect(rx, ry + 12, rw, 10);

    g2.setColor(Color.WHITE);
    g2.setFont(ModernUIHelper.fontBold(11));
    g2.drawString(r.getCode(), rx + 8, ry + 16);

    // Mini status dot
    int dotSize = 6;
    g2.setColor(isAvail ? new Color(52, 211, 153) : new Color(248, 113, 113));
    g2.fillOval(rx + rw - dotSize - 8, ry + (headerH - dotSize) / 2, dotSize, dotSize);

    // 2. Nama Ruangan (1 Baris terpotong aman)
    g2.setColor(Color.WHITE);
    g2.setFont(ModernUIHelper.fontBold(10));
    String name = fitTextToWidth(g2, r.getName(), rw - 16);
    g2.drawString(name, rx + 8, ry + headerH + 16);

    // 3. Mini Kapasitas di bagian bawah
    g2.setFont(ModernUIHelper.fontPlain(9));
    g2.setColor(new Color(241, 245, 249, 210));
    g2.drawString(r.getCapacity() + " Kursi", rx + 8, ry + rh - 8);
}

private void renderFullRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
    // Render elemen lengkap standar: Strip header, nama, tipe, status badge solid
    // ...
}

private String fitTextToWidth(Graphics2D g2, String text, int maxWidth) {
    if (text == null) return "";
    FontMetrics fm = g2.getFontMetrics();
    if (fm.stringWidth(text) <= maxWidth) return text;
    String ellipsis = "...";
    for (int i = text.length() - 1; i > 0; i--) {
        String sub = text.substring(0, i) + ellipsis;
        if (fm.stringWidth(sub) <= maxWidth) {
            return sub;
        }
    }
    return text.substring(0, Math.min(3, text.length()));
}
```

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-06-1**: Ketika ruangan diperkecil hingga ukuran 80x55 px pada Layout Editor, seluruh teks detail (tipe, fasilitas, badge panjang) disembunyikan otomatis, hanya menampilkan kode ruangan dan dot ketersediaan tanpa overflow keluar kotak.
2. **AC-06-2**: Ketika ruangan diperbesar kembali ke ukuran normal (180x140 px), seluruh detail informasi (strip header, kapasitas, nama, tipe, badge status lengkap) muncul kembali secara utuh.
3. **AC-06-3**: Nama ruangan yang panjang selalu dipotong rapi dengan elipsis `...` dan tidak pernah menabrak tepi kanan kotak pada semua ukuran.
4. **AC-06-4**: Tooltip saat kursor diarahkan ke ruangan (*hover*) tetap menampilkan seluruh informasi lengkap terlepas dari ukuran fisik kotak ruangan pada canvas.
