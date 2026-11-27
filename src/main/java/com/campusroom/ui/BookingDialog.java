package com.campusroom.ui;

import com.campusroom.model.Booking;
import com.campusroom.model.Room;
import com.campusroom.model.StudentUser;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.BookingService;
import com.campusroom.service.ServiceResult;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * BookingDialog - Form pengajuan peminjaman ruangan kampus untuk perkuliahan pengganti/kegiatan.
 */
public class BookingDialog extends JDialog {
    private final Room targetRoom;
    private final LocalDate initialDate;
    private final String initialStart;
    private final String initialEnd;
    private final Runnable onSuccessCallback;

    private JTextField txtRoom;
    private JTextField txtDate;
    private JLabel lblDateHint;
    private JComboBox<String> cbStart;
    private JComboBox<String> cbEnd;
    private JTextField txtCourse;
    private JTextField txtLecturer;
    private JTextField txtNim;
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextArea txtPurpose;

    private final BookingService bookingService = new BookingService();

    public BookingDialog(JFrame parent, Room room, LocalDate date, String startTime, String endTime, Runnable onSuccess) {
        super(parent, "Form Pengajuan Booking Ruangan - " + (room != null ? room.getCode() : ""), true);
        this.targetRoom = room;
        this.initialDate = (date != null) ? date : LocalDate.now();
        this.initialStart = (startTime != null) ? startTime : "08:00";
        this.initialEnd = (endTime != null) ? endTime : "10:30";
        this.onSuccessCallback = onSuccess;

        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setResizable(false);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 41, 59));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Pengajuan Peminjaman Ruangan");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Isi data untuk peminjaman ruangan.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Panel Wrapper (Banner Ketentuan H-1 + GridBag Form)
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 10));
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(12, 22, 10, 22));

        JPanel noticeBanner = ModernUIHelper.createCalloutCard(
                "Ketentuan Reservasi (Maks. H-1 & Status Fiks)",
                "Pengajuan booking ruangan wajib dilakukan paling lambat <b>H-1</b> sebelum tanggal penggunaan. " +
                "Ruangan yang sudah disetujui resmi (<b>FIKS</b>) tidak dapat diubah atau dibatalkan.",
                ModernUIHelper.COLOR_WARNING,
                ModernUIHelper.COLOR_WARNING_LIGHT
        );
        centerWrapper.add(noticeBanner, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        User currentUser = AuthService.getInstance().getCurrentUser();

        txtRoom = new JTextField(targetRoom != null ? targetRoom.getCode() + " - " + targetRoom.getName() + " (Kapasitas: " + targetRoom.getCapacity() + " Kursi)" : "");
        txtRoom.setEditable(false);
        txtRoom.setBackground(new Color(241, 245, 249));

        LocalDate minAllowed = LocalDate.now().plusDays(1);
        boolean autoAdjusted = initialDate.isBefore(minAllowed);
        LocalDate defaultFormDate = autoAdjusted ? minAllowed : initialDate;
        txtDate = new JTextField(defaultFormDate.toString(), 12);
        lblDateHint = new JLabel();
        lblDateHint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        txtDate.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateDateValidationHint(false); }
            @Override public void removeUpdate(DocumentEvent e) { updateDateValidationHint(false); }
            @Override public void changedUpdate(DocumentEvent e) { updateDateValidationHint(false); }
        });
        updateDateValidationHint(autoAdjusted);

        JPanel datePanel = new JPanel(new BorderLayout(0, 3));
        datePanel.setOpaque(false);
        datePanel.add(txtDate, BorderLayout.NORTH);
        datePanel.add(lblDateHint, BorderLayout.SOUTH);
        
        String[] timeSlots = {"07:00", "08:00", "09:00", "10:00", "10:30", "11:30", "13:00", "14:00", "15:30", "16:00", "17:30", "19:00"};
        cbStart = new JComboBox<>(timeSlots);
        cbStart.setSelectedItem(initialStart);

        cbEnd = new JComboBox<>(timeSlots);
        cbEnd.setSelectedItem(initialEnd);

        txtCourse = new JTextField("Pemrograman Berorientasi Objek (Kelas Pengganti)", 20);
        txtLecturer = new JTextField("Dr. Ir. Hendra Saputra, M.T.", 20);

        String userNim = (currentUser instanceof StudentUser) ? ((StudentUser) currentUser).getNim() : "250111019";
        String userName = currentUser != null ? currentUser.getFullName() : "Muhamad Nur Akbar";
        String userPhone = currentUser != null ? currentUser.getPhone() : "081234567890";

        txtNim = new JTextField(userNim, 15);
        txtName = new JTextField(userName, 20);
        txtPhone = new JTextField(userPhone != null ? userPhone : "081234567890", 15);

        txtPurpose = new JTextArea("Dosen berhalangan hadir pada jadwal reguler sebelumnya. Ruangan diajukan untuk kelas pengganti tatap muka.", 3, 20);
        txtPurpose.setLineWrap(true);
        txtPurpose.setWrapStyleWord(true);
        JScrollPane spPurpose = new JScrollPane(txtPurpose);

        // Tambah field ke GridBag
        int row = 0;
        addFormRow(formPanel, gbc, row++, "Ruangan:", txtRoom);
        addFormRow(formPanel, gbc, row++, "Tanggal Booking (YYYY-MM-DD):", datePanel);
        
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timePanel.add(new JLabel("Mulai:"));
        timePanel.add(cbStart);
        timePanel.add(Box.createHorizontalStrut(10));
        timePanel.add(new JLabel("Selesai:"));
        timePanel.add(cbEnd);
        addFormRow(formPanel, gbc, row++, "Waktu Perkuliahan:", timePanel);

        addFormRow(formPanel, gbc, row++, "Mata Kuliah / Kegiatan:", txtCourse);
        addFormRow(formPanel, gbc, row++, "Dosen Pengampu:", txtLecturer);
        addFormRow(formPanel, gbc, row++, "NIM PJ Mahasiswa:", txtNim);
        addFormRow(formPanel, gbc, row++, "Nama Lengkap PJ:", txtName);
        addFormRow(formPanel, gbc, row++, "No. HP / WhatsApp:", txtPhone);
        addFormRow(formPanel, gbc, row++, "Alasan / Keperluan:", spPurpose);

        centerWrapper.add(formPanel, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Buttons Panel + Admin Contact Help
        JPanel footerBar = new JPanel(new BorderLayout(10, 0));
        footerBar.setBackground(new Color(248, 250, 252));
        footerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        JButton btnWaHelp = ModernUIHelper.createStyledButton("WA Admin: " + ModernUIHelper.ADMIN_PHONE, new Color(220, 252, 231), new Color(22, 101, 52), 11, true);
        btnWaHelp.setToolTipText("Hubungi Admin Sarpras langsung via WhatsApp jika ada kendala pengajuan");
        btnWaHelp.addActionListener(e -> ModernUIHelper.showContactAdminDialog(this));
        footerBar.add(btnWaHelp, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSubmit = new JButton("Kirim Pengajuan");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSubmit.setBackground(new Color(16, 185, 129));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.addActionListener(e -> handleSubmitBooking());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        footerBar.add(btnPanel, BorderLayout.EAST);
        add(footerBar, BorderLayout.SOUTH);
    }

    private void updateDateValidationHint(boolean autoAdjusted) {
        if (lblDateHint == null || txtDate == null) return;
        String text = txtDate.getText().trim();
        LocalDate minAllowed = LocalDate.now().plusDays(1);
        try {
            LocalDate parsed = LocalDate.parse(text);
            if (parsed.isBefore(minAllowed)) {
                lblDateHint.setText("Peringatan: Booking maksimal H-1 (minimal tanggal " + minAllowed + ").");
                lblDateHint.setForeground(ModernUIHelper.COLOR_DANGER);
            } else if (autoAdjusted) {
                lblDateHint.setText("Tanggal disesuaikan ke H+1 (" + minAllowed + ") sesuai ketentuan maks. H-1.");
                lblDateHint.setForeground(new Color(180, 83, 9));
            } else {
                lblDateHint.setText("Tanggal valid (memenuhi ketentuan minimal H-1).");
                lblDateHint.setForeground(new Color(5, 150, 105));
            }
        } catch (DateTimeParseException ex) {
            lblDateHint.setText("Format tanggal harus YYYY-MM-DD.");
            lblDateHint.setForeground(ModernUIHelper.COLOR_DANGER);
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }

    private void handleSubmitBooking() {
        if (targetRoom == null) {
            JOptionPane.showMessageDialog(this, "Pilih ruangan terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah (YYYY-MM-DD).", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String startTime = (String) cbStart.getSelectedItem();
        String endTime = (String) cbEnd.getSelectedItem();
        String course = txtCourse.getText().trim();
        String lecturer = txtLecturer.getText().trim();
        String nim = txtNim.getText().trim();
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String purpose = txtPurpose.getText().trim();
        User user = AuthService.getInstance().getCurrentUser();

        // Proses validasi dan booking di Service Layer
        ServiceResult<Booking> result = bookingService.createBooking(
                targetRoom, date, startTime, endTime, course, lecturer, nim, name, phone, purpose, user
        );

        if (result.isSuccess()) {
            Booking booking = result.getData();
            JOptionPane.showMessageDialog(this,
                    "Pengajuan berhasil dikirim!\nKode Booking: " + booking.getBookingCode() + "\nStatus: Menunggu Validasi Sekdek",
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Peringatan", JOptionPane.WARNING_MESSAGE);
        }
    }
}
