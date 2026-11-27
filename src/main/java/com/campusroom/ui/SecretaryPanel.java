package com.campusroom.ui;

import com.campusroom.model.Booking;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import com.campusroom.service.BookingService;
import com.campusroom.service.MasterDataService;
import com.campusroom.service.ScheduleService;
import com.campusroom.service.ServiceResult;
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
    private final BookingService bookingService = new BookingService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final MasterDataService masterDataService = new MasterDataService();
    private final Runnable onDataChangedCallback;

    private JPanel pnlValidation;
    private JPanel pnlSchedule;
    private JPanel pnlLecturerSchedule;

    private JTable tblPending;
    private DefaultTableModel modelPending;
    private JLabel lblPendingCount;

    private JTable tblSchedules;
    private DefaultTableModel modelSchedules;
    private JLabel lblScheduleCount;

    // Komponen Jadwal per Dosen
    private JTabbedPane scheduleTabs;
    private JTable tblLecturerSchedules;
    private DefaultTableModel modelLecturerSchedules;
    private JComboBox<String> cbLecturers;
    private JComboBox<String> cbDayFilter;
    private JLabel lblLecturerName;
    private JLabel lblLecturerSessions;
    private JLabel lblLecturerHours;
    private JLabel lblLecturerDays;
    private JLabel lblLecturerConflict;
    private JLabel lblLecturerScheduleCount;

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
                "Validasi Pengajuan",
                "Periksa dan verifikasi permohonan ruangan mahasiswa.",
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
        JTextField txtSearchPending = new JTextField(22);
        ModernUIHelper.attachInstantTableSearch(txtSearchPending, tblPending, 1, 2, 3, 5, 6, 7, 8, 9, 10);
        tableCard.add(ModernUIHelper.createTableSearchBar("Cari Pengajuan:", "Kode booking, ruangan, matkul, dosen, NIM, nama PJ...", txtSearchPending), BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton btnRefresh = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefresh.addActionListener(e -> refreshData());

        JButton btnReject = ModernUIHelper.createStyledButton("Tolak", ModernUIHelper.COLOR_DANGER, Color.WHITE, 12, true);
        btnReject.addActionListener(e -> handleValidateBooking(false));

        JButton btnValidate = ModernUIHelper.createStyledButton("Setujui", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnValidate.addActionListener(e -> handleValidateBooking(true));

        actionPanel.add(btnRefresh);
        actionPanel.add(btnReject);
        actionPanel.add(btnValidate);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createScheduleContent() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ModernUIHelper.BG_PAGE);

        scheduleTabs = new JTabbedPane();
        scheduleTabs.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel pnlAllSchedules = createAllSchedulesSubContent();
        pnlLecturerSchedule = createLecturerScheduleContent();

        scheduleTabs.addTab("Seluruh Jadwal Perkuliahan", pnlAllSchedules);
        scheduleTabs.addTab("Detail Jadwal per Dosen", pnlLecturerSchedule);

        container.add(scheduleTabs, BorderLayout.CENTER);
        return container;
    }

    private JPanel createAllSchedulesSubContent() {
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
                "Jadwal Perkuliahan Tetap",
                "Daftar seluruh jadwal perkuliahan rutin semester berdasarkan ruangan.",
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
        JTextField txtSearchSchedule = new JTextField(22);
        ModernUIHelper.attachInstantTableSearch(txtSearchSchedule, tblSchedules, 1, 4, 5, 6, 7, 8);
        tableCard.add(ModernUIHelper.createTableSearchBar("Cari Jadwal:", "Hari, ruangan, kode/nama matkul, dosen, kelas...", txtSearchSchedule), BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton btnDeleteSchedule = ModernUIHelper.createStyledButton("Hapus Jadwal", new Color(254, 226, 226), ModernUIHelper.COLOR_DANGER, 11, false);
        btnDeleteSchedule.addActionListener(e -> handleDeleteSchedule());

        JButton btnAddSchedule = ModernUIHelper.createStyledButton("Tambah Jadwal", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 12, true);
        btnAddSchedule.addActionListener(e -> showAddScheduleDialog());

        actionPanel.add(btnDeleteSchedule);
        actionPanel.add(btnAddSchedule);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLecturerScheduleContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // Modern Page Header
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblLecturerScheduleCount = new JLabel("0 Sesi Mengajar");
        lblLecturerScheduleCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblLecturerScheduleCount.setOpaque(true);
        lblLecturerScheduleCount.setBackground(new Color(224, 231, 255)); // Indigo 100
        lblLecturerScheduleCount.setForeground(new Color(67, 56, 202));   // Indigo 700
        lblLecturerScheduleCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblLecturerScheduleCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Detail Jadwal per Dosen",
                "Periksa rincian jadwal mengajar mingguan dan total jam mengajar per dosen.",
                rightHeader
        );

        // Filter Bar Card
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterCard.setBackground(Color.WHITE);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JLabel lblLec = new JLabel("Dosen Pengampu:");
        lblLec.setFont(ModernUIHelper.fontBold(11));
        lblLec.setForeground(ModernUIHelper.TEXT_DARK);
        filterCard.add(lblLec);

        cbLecturers = new JComboBox<>();
        cbLecturers.setPreferredSize(new Dimension(240, 28));
        ModernUIHelper.styleComboBox(cbLecturers);
        cbLecturers.addActionListener(e -> updateLecturerScheduleView());
        filterCard.add(cbLecturers);

        JLabel lblDay = new JLabel("Hari:");
        lblDay.setFont(ModernUIHelper.fontBold(11));
        lblDay.setForeground(ModernUIHelper.TEXT_DARK);
        filterCard.add(lblDay);

        String[] days = {"Semua Hari", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        cbDayFilter = new JComboBox<>(days);
        cbDayFilter.setPreferredSize(new Dimension(110, 28));
        ModernUIHelper.styleComboBox(cbDayFilter);
        cbDayFilter.addActionListener(e -> updateLecturerScheduleView());
        filterCard.add(cbDayFilter);

        JButton btnRefreshLec = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefreshLec.addActionListener(e -> refreshLecturers());
        filterCard.add(btnRefreshLec);

        JPanel topGroup = new JPanel(new BorderLayout(0, 8));
        topGroup.setOpaque(false);
        topGroup.add(header, BorderLayout.NORTH);
        topGroup.add(filterCard, BorderLayout.SOUTH);
        panel.add(topGroup, BorderLayout.NORTH);

        // Center Container: Profile Summary Card + Schedule Table Card
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);

        // Summary Card
        JPanel summaryCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        summaryCard.setBackground(new Color(248, 250, 252));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        lblLecturerName = new JLabel("Pilih Dosen Pengampu");
        lblLecturerName.setFont(ModernUIHelper.fontBold(13));
        lblLecturerName.setForeground(ModernUIHelper.TEXT_DARK);

        lblLecturerSessions = ModernUIHelper.createPillBadge("0 Sesi", null, new Color(241, 245, 249), ModernUIHelper.COLOR_PRIMARY);
        lblLecturerHours = ModernUIHelper.createPillBadge("0 Jam", null, new Color(241, 245, 249), ModernUIHelper.TEXT_DARK);
        lblLecturerDays = ModernUIHelper.createPillBadge("Hari: -", null, new Color(241, 245, 249), ModernUIHelper.TEXT_SECONDARY);
        lblLecturerConflict = ModernUIHelper.createPillBadge("🟢 Jadwal Teratur", new Color(16, 185, 129), new Color(220, 252, 231), new Color(22, 101, 52));

        summaryCard.add(lblLecturerName);
        summaryCard.add(lblLecturerSessions);
        summaryCard.add(lblLecturerHours);
        summaryCard.add(lblLecturerDays);
        summaryCard.add(lblLecturerConflict);
        centerContainer.add(summaryCard, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] cols = {"ID", "Hari", "Jam Mulai", "Jam Selesai", "Durasi", "Ruangan", "Kode MK", "Nama Mata Kuliah", "Kelas"};
        modelLecturerSchedules = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLecturerSchedules = new JTable(modelLecturerSchedules);
        ModernUIHelper.styleModernTable(tblLecturerSchedules);
        tblLecturerSchedules.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblLecturerSchedules.getColumnModel().getColumn(1).setPreferredWidth(75);
        tblLecturerSchedules.getColumnModel().getColumn(2).setPreferredWidth(75);
        tblLecturerSchedules.getColumnModel().getColumn(3).setPreferredWidth(75);
        tblLecturerSchedules.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblLecturerSchedules.getColumnModel().getColumn(5).setPreferredWidth(85);
        tblLecturerSchedules.getColumnModel().getColumn(6).setPreferredWidth(80);
        tblLecturerSchedules.getColumnModel().getColumn(7).setPreferredWidth(220);

        JScrollPane scroll = new JScrollPane(tblLecturerSchedules);
        scroll.setBorder(null);
        tableCard.add(scroll, BorderLayout.CENTER);
        centerContainer.add(tableCard, BorderLayout.CENTER);

        panel.add(centerContainer, BorderLayout.CENTER);
        return panel;
    }

    private void updateLecturerScheduleView() {
        if (modelLecturerSchedules == null || cbLecturers == null) return;
        modelLecturerSchedules.setRowCount(0);

        String selectedLecturer = (String) cbLecturers.getSelectedItem();
        if (selectedLecturer == null || selectedLecturer.trim().isEmpty()) {
            if (lblLecturerName != null) lblLecturerName.setText("Pilih Dosen Pengampu");
            if (lblLecturerSessions != null) lblLecturerSessions.setText("0 Sesi");
            if (lblLecturerHours != null) lblLecturerHours.setText("0 Jam");
            if (lblLecturerDays != null) lblLecturerDays.setText("Hari: -");
            if (lblLecturerScheduleCount != null) lblLecturerScheduleCount.setText("0 Sesi Mengajar");
            return;
        }

        ScheduleService.LecturerSummary summary = scheduleService.getLecturerSummary(selectedLecturer);
        if (lblLecturerName != null) lblLecturerName.setText("👨‍🏫 " + selectedLecturer);
        if (lblLecturerSessions != null) lblLecturerSessions.setText("Total: " + summary.getTotalSessions() + " Sesi");
        if (lblLecturerHours != null) lblLecturerHours.setText("Beban: " + summary.getFormattedDuration());
        if (lblLecturerDays != null) {
            String daysStr = summary.getActiveDays().isEmpty() ? "-" : String.join(", ", summary.getActiveDays());
            lblLecturerDays.setText("Hari: " + daysStr);
        }
        if (lblLecturerConflict != null) {
            if (summary.hasConflict()) {
                lblLecturerConflict.setText("⚠️ Terdeteksi Bentrok Jadwal!");
                lblLecturerConflict.setBackground(new Color(254, 226, 226));
                lblLecturerConflict.setForeground(new Color(185, 28, 28));
            } else {
                lblLecturerConflict.setText("🟢 Jadwal Teratur (Tanpa Bentrok)");
                lblLecturerConflict.setBackground(new Color(220, 252, 231));
                lblLecturerConflict.setForeground(new Color(22, 101, 52));
            }
        }

        String selectedDay = (String) cbDayFilter.getSelectedItem();
        boolean filterDay = (selectedDay != null && !selectedDay.equalsIgnoreCase("Semua Hari"));

        List<RegularSchedule> list = scheduleService.getSchedulesByLecturer(selectedLecturer);
        int countShown = 0;
        for (RegularSchedule s : list) {
            if (filterDay && !s.getDayOfWeek().equalsIgnoreCase(selectedDay)) {
                continue;
            }
            countShown++;

            String durStr = "-";
            try {
                java.time.LocalTime t1 = java.time.LocalTime.parse(s.getStartTime());
                java.time.LocalTime t2 = java.time.LocalTime.parse(s.getEndTime());
                long mins = java.time.Duration.between(t1, t2).toMinutes();
                durStr = mins + " mnt";
            } catch (Exception ignored) {}

            modelLecturerSchedules.addRow(new Object[]{
                    s.getId(),
                    s.getDayOfWeek(),
                    s.getStartTime(),
                    s.getEndTime(),
                    durStr,
                    (s.getRoomCode() != null ? s.getRoomCode() : "R." + s.getRoomId()),
                    s.getCourseCode(),
                    s.getCourseName(),
                    s.getClassName()
            });
        }

        if (lblLecturerScheduleCount != null) {
            lblLecturerScheduleCount.setText(countShown + " Sesi Ditampilkan");
        }
    }

    private void refreshLecturers() {
        if (cbLecturers == null) return;
        String prev = (String) cbLecturers.getSelectedItem();
        cbLecturers.removeAllItems();

        List<String> lecturers = scheduleService.getAllLecturers();
        for (String lec : lecturers) {
            cbLecturers.addItem(lec);
        }

        if (prev != null && lecturers.contains(prev)) {
            cbLecturers.setSelectedItem(prev);
        } else if (!lecturers.isEmpty()) {
            cbLecturers.setSelectedIndex(0);
        }
        updateLecturerScheduleView();
    }

    public JPanel getLecturerSchedulePanel() {
        return pnlLecturerSchedule;
    }

    public void selectLecturerTab() {
        if (scheduleTabs != null && scheduleTabs.getTabCount() > 1) {
            scheduleTabs.setSelectedIndex(1);
        }
    }

    public void refreshData() {
        Integer prevPendingId = ModernUIHelper.getSelectedRowId(tblPending, modelPending);
        Integer prevScheduleId = ModernUIHelper.getSelectedRowId(tblSchedules, modelSchedules);

        // 1. Refresh Pending Bookings via BookingService
        modelPending.setRowCount(0);
        List<Booking> bookings = bookingService.getBookingsByStatus("MENUNGGU_SEKDEK");
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
        ModernUIHelper.restoreSelectedRowById(tblPending, modelPending, prevPendingId);

        // 2. Refresh Schedules via ScheduleService
        modelSchedules.setRowCount(0);
        List<RegularSchedule> schedules = scheduleService.getAllSchedules();
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
        ModernUIHelper.restoreSelectedRowById(tblSchedules, modelSchedules, prevScheduleId);

        // 3. Refresh Jadwal per Dosen
        refreshLecturers();
    }

    private void handleValidateBooking(boolean approve) {
        int viewRow = tblPending.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih pengajuan terlebih dahulu.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = tblPending.convertRowIndexToModel(viewRow);

        int bookingId = (int) modelPending.getValueAt(selectedRow, 0);
        String bookingCode = (String) modelPending.getValueAt(selectedRow, 1);

        String note = JOptionPane.showInputDialog(this,
                (approve ? "Catatan (opsional):" : "Alasan penolakan:"),
                approve ? "Setujui Pengajuan" : "Tolak Pengajuan",
                JOptionPane.QUESTION_MESSAGE);

        if (note == null && !approve) {
            return;
        }

        ServiceResult<Void> result = bookingService.validateBySecretary(bookingId, approve, note);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Pengajuan " + bookingCode + (approve ? " disetujui." : " ditolak."),
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddScheduleDialog() {
        List<Room> allRooms = masterDataService.getAllRooms();
        if (allRooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Data ruangan belum tersedia.", "Peringatan", JOptionPane.WARNING_MESSAGE);
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
        panel.add(new JLabel("Ruangan:"));
        panel.add(cbRoom);
        panel.add(new JLabel("Hari:"));
        panel.add(cbDay);
        panel.add(new JLabel("Jam Mulai:"));
        panel.add(cbStart);
        panel.add(new JLabel("Jam Selesai:"));
        panel.add(cbEnd);
        panel.add(new JLabel("Kode MK:"));
        panel.add(txtCode);
        panel.add(new JLabel("Nama MK:"));
        panel.add(txtCourse);
        panel.add(new JLabel("Dosen & Kelas:"));

        JPanel pSub = new JPanel(new BorderLayout(5, 0));
        pSub.add(txtLecturer, BorderLayout.CENTER);
        pSub.add(txtClass, BorderLayout.EAST);
        panel.add(pSub);

        int result = JOptionPane.showConfirmDialog(this, panel, "Tambah Jadwal Kuliah", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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

            ServiceResult<RegularSchedule> res = scheduleService.addSchedule(schedule);
            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleDeleteSchedule() {
        int viewRow = tblSchedules.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih jadwal yang ingin dihapus.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRow = tblSchedules.convertRowIndexToModel(viewRow);

        int scheduleId = (int) modelSchedules.getValueAt(selectedRow, 0);
        String course = (String) modelSchedules.getValueAt(selectedRow, 6);

        int confirm = JOptionPane.showConfirmDialog(this, "Hapus jadwal: " + course + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ServiceResult<Void> res = scheduleService.deleteSchedule(scheduleId);
            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
