package com.campusroom.ui;

import com.campusroom.dao.BookingDao;
import com.campusroom.dao.RoomDao;
import com.campusroom.dao.ScheduleDao;
import com.campusroom.model.Booking;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * SecretaryPanel - Modul Portal Sekretaris Dekan.
 * Bertanggung jawab memvalidasi pengajuan booking mahasiswa dan mengelola jadwal tetap semester.
 * Mendukung embedding panel terpisah di CardLayout (tanpa nested tab) atau via tabbed pane.
 */
public class SecretaryPanel extends JPanel {
    private final BookingDao bookingDao = new BookingDao();
    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final RoomDao roomDao = new RoomDao();
    private final Runnable onDataChangedCallback;

    private JPanel pnlValidation;
    private JPanel pnlSchedule;

    private JTable tblPending;
    private DefaultTableModel modelPending;
    private JLabel lblPendingCount;

    private JTable tblSchedules;
    private DefaultTableModel modelSchedules;
    private JLabel lblScheduleCount;

    private JTabbedPane tabbedPane;

    public SecretaryPanel(Runnable onDataChanged) {
        this.onDataChangedCallback = onDataChanged;
        setLayout(new BorderLayout());

        pnlValidation = createValidationContent();
        pnlSchedule = createScheduleContent();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.addTab("Antrean Validasi Pengajuan", pnlValidation);
        tabbedPane.addTab("Master Jadwal Tetap Perkuliahan", pnlSchedule);

        add(tabbedPane, BorderLayout.CENTER);
        refreshData();
    }

    public JPanel getValidationPanel() {
        return pnlValidation;
    }

    public JPanel getSchedulePanel() {
        return pnlSchedule;
    }

    public void selectTab(int index) {
        if (tabbedPane != null && index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    private JPanel createValidationContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // Modern Page Header
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblPendingCount = new JLabel("0 Pengajuan Menunggu");
        lblPendingCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblPendingCount.setOpaque(true);
        lblPendingCount.setBackground(new Color(254, 243, 199)); // Amber 100
        lblPendingCount.setForeground(new Color(146, 64, 14));   // Amber 800
        lblPendingCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblPendingCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Validasi Pengajuan Jadwal Mahasiswa",
                "Verifikasi permohonan ruangan terhadap master jadwal dekanat sebelum diteruskan ke Admin Master.",
                rightHeader
        );
        panel.add(header, BorderLayout.NORTH);

        // Table Card Container
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] columns = {"ID", "Kode Booking", "Ruangan", "Tanggal", "Waktu", "Mata Kuliah / Acara", "Dosen", "PJ Mahasiswa", "NIM", "Keperluan", "Status"};
        modelPending = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblPending = new JTable(modelPending);
        ModernUIHelper.styleModernTable(tblPending);
        tblPending.getColumnModel().getColumn(10).setCellRenderer(ModernUIHelper.createStatusBadgeRenderer());
        tblPending.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblPending.getColumnModel().getColumn(1).setPreferredWidth(110);
        tblPending.getColumnModel().getColumn(10).setPreferredWidth(190);

        JScrollPane scrollPane = new JScrollPane(tblPending);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton btnRefresh = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefresh.addActionListener(e -> refreshData());

        JButton btnReject = ModernUIHelper.createStyledButton("Tolak Pengajuan", ModernUIHelper.COLOR_DANGER, Color.WHITE, 12, true);
        btnReject.addActionListener(e -> handleValidateBooking(false));

        JButton btnValidate = ModernUIHelper.createStyledButton("Validasi & Teruskan ke Admin", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnValidate.addActionListener(e -> handleValidateBooking(true));

        actionPanel.add(btnRefresh);
        actionPanel.add(btnReject);
        actionPanel.add(btnValidate);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createScheduleContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // Modern Page Header
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblScheduleCount = new JLabel("0 Jadwal Tetap Terdaftar");
        lblScheduleCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblScheduleCount.setOpaque(true);
        lblScheduleCount.setBackground(new Color(220, 252, 231)); // Emerald 100
        lblScheduleCount.setForeground(new Color(22, 101, 52));   // Emerald 800
        lblScheduleCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblScheduleCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Master Jadwal Tetap Perkuliahan Semester",
                "Jadwal rutin yang terdaftar di sini secara otomatis mengunci ruangan dan mencegah bentrokan peminjaman.",
                rightHeader
        );
        panel.add(header, BorderLayout.NORTH);

        // Table Card Container
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] columns = {"ID", "Hari", "Jam Mulai", "Jam Selesai", "Ruangan", "Kode MK", "Nama Mata Kuliah", "Dosen Pengampu", "Kelas"};
        modelSchedules = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblSchedules = new JTable(modelSchedules);
        ModernUIHelper.styleModernTable(tblSchedules);
        tblSchedules.getColumnModel().getColumn(0).setPreferredWidth(45);

        JScrollPane scrollPane = new JScrollPane(tblSchedules);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton btnDeleteSchedule = ModernUIHelper.createStyledButton("Hapus Jadwal Terpilih", new Color(254, 226, 226), ModernUIHelper.COLOR_DANGER, 11, false);
        btnDeleteSchedule.addActionListener(e -> handleDeleteSchedule());

        JButton btnAddSchedule = ModernUIHelper.createStyledButton("Tambah Jadwal Tetap", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 12, true);
        btnAddSchedule.addActionListener(e -> showAddScheduleDialog());

        actionPanel.add(btnDeleteSchedule);
        actionPanel.add(btnAddSchedule);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshData() {
        // 1. Refresh Pending Bookings
        modelPending.setRowCount(0);
        List<Booking> bookings = bookingDao.getBookingsByStatus("MENUNGGU_SEKDEK");
        for (Booking b : bookings) {
            modelPending.addRow(new Object[]{
                    b.getId(),
                    b.getBookingCode(),
                    b.getRoomCode() + " (" + b.getRoomName() + ")",
                    b.getBookingDate(),
                    b.getStartTime() + " - " + b.getEndTime(),
                    b.getCourseName(),
                    b.getLecturerName(),
                    b.getStudentName(),
                    b.getStudentNim(),
                    b.getPurpose(),
                    b.getStatusBadgeText()
            });
        }
        if (lblPendingCount != null) {
            lblPendingCount.setText(bookings.size() + " Pengajuan Menunggu");
        }

        // 2. Refresh Schedules
        modelSchedules.setRowCount(0);
        List<RegularSchedule> schedules = scheduleDao.getAllSchedules();
        for (RegularSchedule s : schedules) {
            modelSchedules.addRow(new Object[]{
                    s.getId(),
                    s.getDayOfWeek(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.getRoomCode(),
                    s.getCourseCode(),
                    s.getCourseName(),
                    s.getLecturerName(),
                    s.getClassName()
            });
        }
        if (lblScheduleCount != null) {
            lblScheduleCount.setText(schedules.size() + " Jadwal Tetap Terdaftar");
        }
    }

    private void handleValidateBooking(boolean approve) {
        int selectedRow = tblPending.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu baris pengajuan terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) modelPending.getValueAt(selectedRow, 0);
        String bookingCode = (String) modelPending.getValueAt(selectedRow, 1);

        String note = JOptionPane.showInputDialog(this,
                (approve ? "Masukkan catatan verifikasi (opsional):" : "Masukkan alasan penolakan:"),
                approve ? "Validasi Sekretaris Dekan" : "Tolak Pengajuan",
                JOptionPane.QUESTION_MESSAGE);

        if (note == null && !approve) {
            return;
        }

        String newStatus = approve ? "DIVALIDASI_SEKDEK" : "DITOLAK";
        if (note == null) note = approve ? "Diverifikasi tidak ada bentrokan dengan jadwal tetap dekanat." : "Ditolak.";

        boolean success = bookingDao.updateSecretaryValidation(bookingId, newStatus, note);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Pengajuan " + bookingCode + " berhasil diubah menjadi: " + (approve ? "DIVALIDASI_SEKDEK (Diteruskan ke Admin)" : "DITOLAK"),
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
        }
    }

    private void showAddScheduleDialog() {
        List<Room> allRooms = roomDao.getAllRooms();
        if (allRooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Data ruangan belum tersedia!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<Room> cbRoom = new JComboBox<>(allRooms.toArray(new Room[0]));
        String[] days = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        JComboBox<String> cbDay = new JComboBox<>(days);

        String[] timeSlots = {"07:00", "08:00", "09:00", "10:00", "10:30", "11:30", "13:00", "14:00", "15:30", "16:00", "17:30"};
        JComboBox<String> cbStart = new JComboBox<>(timeSlots);
        JComboBox<String> cbEnd = new JComboBox<>(timeSlots);
        cbEnd.setSelectedIndex(3);

        JTextField txtCode = new JTextField("TIF-215", 15);
        JTextField txtCourse = new JTextField("Kecerdasan Buatan & Sistem Pakar", 20);
        JTextField txtLecturer = new JTextField("Prof. Dr. Suwandi, M.Sc.", 20);
        JTextField txtClass = new JTextField("TI-3A", 10);

        JPanel panel = new JPanel(new GridLayout(7, 2, 8, 8));
        panel.add(new JLabel("Pilih Ruangan:"));
        panel.add(cbRoom);
        panel.add(new JLabel("Hari Perkuliahan:"));
        panel.add(cbDay);
        panel.add(new JLabel("Jam Mulai:"));
        panel.add(cbStart);
        panel.add(new JLabel("Jam Selesai:"));
        panel.add(cbEnd);
        panel.add(new JLabel("Kode Mata Kuliah:"));
        panel.add(txtCode);
        panel.add(new JLabel("Nama Mata Kuliah:"));
        panel.add(txtCourse);
        panel.add(new JLabel("Dosen & Kelas:"));

        JPanel pSub = new JPanel(new BorderLayout(5, 0));
        pSub.add(txtLecturer, BorderLayout.CENTER);
        pSub.add(txtClass, BorderLayout.EAST);
        panel.add(pSub);

        int result = JOptionPane.showConfirmDialog(this, panel, "Tambah Jadwal Perkuliahan Tetap (Sekdek)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Room r = (Room) cbRoom.getSelectedItem();
            if (r == null) return;

            RegularSchedule schedule = new RegularSchedule(
                    r.getId(),
                    (String) cbDay.getSelectedItem(),
                    (String) cbStart.getSelectedItem(),
                    (String) cbEnd.getSelectedItem(),
                    txtCode.getText().trim(),
                    txtCourse.getText().trim(),
                    txtLecturer.getText().trim(),
                    txtClass.getText().trim()
            );

            if (scheduleDao.addSchedule(schedule)) {
                JOptionPane.showMessageDialog(this, "Jadwal tetap berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }

    private void handleDeleteSchedule() {
        int selectedRow = tblSchedules.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu jadwal yang ingin dihapus!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int scheduleId = (int) modelSchedules.getValueAt(selectedRow, 0);
        String course = (String) modelSchedules.getValueAt(selectedRow, 6);

        int confirm = JOptionPane.showConfirmDialog(this, "Hapus jadwal tetap: " + course + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (scheduleDao.deleteSchedule(scheduleId)) {
                JOptionPane.showMessageDialog(this, "Jadwal berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }
}
