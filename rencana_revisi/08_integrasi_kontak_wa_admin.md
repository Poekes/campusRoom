# Spesifikasi Revisi 8: Tombol & Dialog Kontak WhatsApp Resmi Admin

> **ID Butir**: REV-08  
> **Komponen**: `MainFrame`, `StudentBookingPanel`, `BookingDialog`, `ModernUIHelper`  
> **Tujuan**: Menambahkan akses langsung untuk menghubungi Administrator Sarpras kampus via WhatsApp resmi (`081399887766` / `Bpk. Rahmat Santoso`) guna memfasilitasi kebutuhan izin darurat, konfirmasi cepat permohonan kelas pengganti, atau pelaporan kendala teknis.

---

## 1. Analisis Kebutuhan & Masalah

### Kondisi Saat Ini (As-Is)
- Data kontak admin (`Bpk. Rahmat Santoso`, nomor HP `081399887766`, email `admin.campusroom@univ.ac.id`) sudah tersimpan di tabel `users` database SQLite, tetapi tidak ditampilkan di antarmuka mahasiswa maupun pengguna umum.
- Mahasiswa PJ mata kuliah yang membutuhkan persetujuan mendesak atau mengalami kendala sistem tidak memiliki saluran komunikasi langsung ke administrator.

### Kondisi yang Diharapkan (To-Be)
- Menyediakan tombol / tautan visual *"💬 Hubungi Admin via WhatsApp"* di beberapa lokasi strategis:
  1. **Top App Bar / Status Bar Bawah pada `MainFrame`**: Tombol bantuan cepat (*Help Desk*).
  2. **Footer pada `StudentBookingPanel`**: Banner bantuan jika status pengajuan belum diproses.
  3. **Bagian Bawah `BookingDialog`**: Teks bantuan bagi mahasiswa saat mengisi formulir peminjaman.
- **Interaksi Pengguna saat Diklik**:
  - Membuka modal dialog informasi kontak resmi:
    - Nama Petugas: **Bpk. Rahmat Santoso (Admin Sarpras & Ruangan)**
    - Nomor WhatsApp: **+62 813-9988-7766** (atau `081399887766`)
    - Jam Layanan Operasional: **Senin – Jumat, 08.00 – 16.00 WIB**
    - Email Resmi: `admin.campusroom@univ.ac.id`
  - Tombol Aksi 1: **"💬 Buka Chat WhatsApp"** — Membuka aplikasi WhatsApp / WhatsApp Web secara otomatis menggunakan URL `https://wa.me/6281399887766?text=Halo%20Admin%20Sarpras%2C%20saya%20ingin%20konfirmasi%20ruangan%20CampusRoom...` melalui `Desktop.getDesktop().browse(URI)`.
  - Tombol Aksi 2: **"📋 Salin Nomor WA"** — Menyalin nomor ke clipboard pengguna dengan notifikasi konfirmasi.

---

## 2. Rincian Teknis Implementasi

### A. Modifikasi `ModernUIHelper.java`

Tambahkan helper utility untuk membuka URL dan menyalin ke clipboard:
```java
public static final String ADMIN_NAME = "Bpk. Rahmat Santoso";
public static final String ADMIN_PHONE = "081399887766";
public static final String ADMIN_PHONE_INTL = "6281399887766";
public static final String ADMIN_EMAIL = "admin.campusroom@univ.ac.id";
public static final String ADMIN_OFFICE_HOURS = "Senin – Jumat, 08:00 – 16:00 WIB";

/**
 * Menampilkan dialog kontak admin resmi dan opsi buka WhatsApp
 */
public static void showContactAdminDialog(Component parent) {
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Pusat Bantuan & Kontak Admin Sarpras", Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setLayout(new BorderLayout());
    dialog.setResizable(false);

    // Header Card
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(new Color(15, 23, 42)); // Slate 900
    header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

    JLabel lblTitle = new JLabel("💬 Layanan Pengaduan & Bantuan Cepat");
    lblTitle.setFont(fontBold(14));
    lblTitle.setForeground(Color.WHITE);

    JLabel lblSubtitle = new JLabel("Hubungi langsung Admin Sarpras untuk izin darurat atau kendala ruangan.");
    lblSubtitle.setFont(fontPlain(11));
    lblSubtitle.setForeground(new Color(148, 163, 184));

    header.add(lblTitle, BorderLayout.NORTH);
    header.add(lblSubtitle, BorderLayout.SOUTH);
    dialog.add(header, BorderLayout.NORTH);

    // Body Info Card
    JPanel body = new JPanel(new GridLayout(4, 1, 6, 6));
    body.setBackground(Color.WHITE);
    body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

    body.add(new JLabel("<html><b>Nama Petugas:</b> " + ADMIN_NAME + " (Admin Sarpras)</html>"));
    body.add(new JLabel("<html><b>Nomor WhatsApp:</b> <span style='color:#059669; font-weight:bold;'>" + ADMIN_PHONE + "</span></html>"));
    body.add(new JLabel("<html><b>Jam Layanan:</b> " + ADMIN_OFFICE_HOURS + "</html>"));
    body.add(new JLabel("<html><b>Email:</b> " + ADMIN_EMAIL + "</html>"));
    dialog.add(body, BorderLayout.CENTER);

    // Footer Buttons
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    footer.setBackground(new Color(248, 250, 252));
    footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT));

    JButton btnCopy = createStyledButton("📋 Salin Nomor", new Color(241, 245, 249), TEXT_DARK, 11, false);
    btnCopy.addActionListener(e -> {
        copyToClipboard(ADMIN_PHONE);
        JOptionPane.showMessageDialog(dialog, "Nomor WhatsApp " + ADMIN_PHONE + " berhasil disalin ke clipboard!", "Tersalin", JOptionPane.INFORMATION_MESSAGE);
    });

    JButton btnOpenWa = createStyledButton("💬 Buka WhatsApp", new Color(16, 185, 129), Color.WHITE, 11, true);
    btnOpenWa.addActionListener(e -> {
        openWhatsAppDirect(ADMIN_PHONE_INTL, "Halo Admin Sarpras CampusRoom, saya ingin konfirmasi perihal peminjaman ruangan.");
        dialog.dispose();
    });

    footer.add(btnCopy);
    footer.add(btnOpenWa);
    dialog.add(footer, BorderLayout.SOUTH);

    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
}

public static void copyToClipboard(String text) {
    StringSelection stringSelection = new StringSelection(text);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
}

public static void openWhatsAppDirect(String phoneIntl, String message) {
    try {
        String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String url = "https://wa.me/" + phoneIntl + "?text=" + encodedMsg;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            // Fallback: jalankan perintah browser Linux/Windows
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Gagal membuka browser secara otomatis.\nSilakan hubungi WhatsApp ke: " + ADMIN_PHONE, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
```

### B. Penempatan Komponen pada UI

1. **Pada `MainFrame.java` (Top App Bar)**:
   - Tambahkan tombol kecil di samping role context: `btnContactAdmin = createStyledButton("💬 Bantuan Admin", ...)`.
2. **Pada `StudentBookingPanel.java`**:
   - Di bawah tabel riwayat pengajuan, tambahkan banner:
     *"Butuh konfirmasi cepat atau ada kendala permohonan ruangan? [Hubungi WhatsApp Admin Sarpras]"*.
3. **Pada `BookingDialog.java`**:
   - Tambahkan footer link: *"Kendala teknis? Klik untuk hubungi admin"*.

---

## 3. Acceptance Criteria (Kriteria Keberhasilan)

1. **AC-08-1**: Terdapat tombol yang jelas untuk menghubungi admin di antarmuka pengguna mahasiswa.
2. **AC-08-2**: Mengklik tombol tersebut menampilkan dialog modal dengan identitas resmi Admin Sarpras (`Bpk. Rahmat Santoso`, nomor `081399887766`, dan jam operasional).
3. **AC-08-3**: Mengklik tombol "Salin Nomor" menyalin nomor telepon admin ke clipboard komputer dan memunculkan toast konfirmasi.
4. **AC-08-4**: Mengklik tombol "Buka WhatsApp" memicu peluncuran browser sistem membuka tautan `https://wa.me/6281399887766` dengan pesan sapaan awal yang sudah terformat.
