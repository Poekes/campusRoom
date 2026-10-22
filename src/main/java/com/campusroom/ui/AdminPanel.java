package com.campusroom.ui;

import com.campusroom.dao.BookingDao;
import com.campusroom.dao.BuildingDao;
import com.campusroom.dao.CampusDao;
import com.campusroom.dao.RoomDao;
import com.campusroom.model.Booking;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.Room;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * AdminPanel - Portal Administrator Master.
 * Mengelola approval akhir booking, konfigurasi multi-kampus & gedung, serta inventaris ruangan.
 * Mendukung embedding panel terpisah di CardLayout (tanpa nested tab) atau via tabbed pane.
 */
public class AdminPanel extends JPanel {
    private final BookingDao bookingDao = new BookingDao();
    private final CampusDao campusDao = new CampusDao();
    private final BuildingDao buildingDao = new BuildingDao();
    private final RoomDao roomDao = new RoomDao();
    private final Runnable onDataChangedCallback;

    private JPanel pnlApproval;
    private JPanel pnlCampusBuilding;
    private JPanel pnlRoomManagement;

    private JTable tblAdminApproval;
    private DefaultTableModel modelAdminApproval;
    private JLabel lblApprovalCount;

    private JTable tblCampuses;
    private DefaultTableModel modelCampuses;

    private JTable tblBuildings;
    private DefaultTableModel modelBuildings;

    private JTable tblRooms;
    private DefaultTableModel modelRooms;
    private JLabel lblRoomCount;

    private JTabbedPane tabbedPane;

    public AdminPanel(Runnable onDataChanged) {
        this.onDataChangedCallback = onDataChanged;
        setLayout(new BorderLayout());

        pnlApproval = createApprovalContent();
        pnlCampusBuilding = createCampusBuildingContent();
        pnlRoomManagement = createRoomManagementContent();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.addTab("Approval Akhir Reservasi", pnlApproval);
        tabbedPane.addTab("Kelola Multi-Kampus & Gedung", pnlCampusBuilding);
        tabbedPane.addTab("Kelola Master Ruangan", pnlRoomManagement);

        add(tabbedPane, BorderLayout.CENTER);
        refreshData();
    }

    public JPanel getApprovalPanel() {
        return pnlApproval;
    }

    public JPanel getCampusBuildingPanel() {
        return pnlCampusBuilding;
    }

    public JPanel getRoomManagementPanel() {
        return pnlRoomManagement;
    }

    public void selectTab(int index) {
        if (tabbedPane != null && index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    private JPanel createApprovalContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // Modern Page Header
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblApprovalCount = new JLabel("0 Pengajuan Siap Disetujui");
        lblApprovalCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblApprovalCount.setOpaque(true);
        lblApprovalCount.setBackground(new Color(254, 243, 199)); // Amber 100
        lblApprovalCount.setForeground(new Color(146, 64, 14));   // Amber 800
        lblApprovalCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblApprovalCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Approval Akhir & Penetapan Reservasi",
                "Pengajuan yang telah divalidasi Sekretaris Dekan menunggu persetujuan final Admin Master untuk penguncian resmi.",
                rightHeader
        );
        panel.add(header, BorderLayout.NORTH);

        // Table Card Container
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] cols = {"ID", "Kode Booking", "Ruangan", "Tanggal", "Jam", "Mata Kuliah / Acara", "Dosen", "PJ Mahasiswa", "Catatan Sekdek", "Status"};
        modelAdminApproval = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblAdminApproval = new JTable(modelAdminApproval);
        ModernUIHelper.styleModernTable(tblAdminApproval);
        tblAdminApproval.getColumnModel().getColumn(9).setCellRenderer(ModernUIHelper.createStatusBadgeRenderer());
        tblAdminApproval.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblAdminApproval.getColumnModel().getColumn(1).setPreferredWidth(110);
        tblAdminApproval.getColumnModel().getColumn(9).setPreferredWidth(190);

        JScrollPane scrollPane = new JScrollPane(tblAdminApproval);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actions.setOpaque(false);

        JButton btnRefresh = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefresh.addActionListener(e -> refreshData());

        JButton btnReject = ModernUIHelper.createStyledButton("Tolak / Perintahkan Pindah", ModernUIHelper.COLOR_DANGER, Color.WHITE, 12, true);
        btnReject.addActionListener(e -> handleFinalApproval(false));

        JButton btnApprove = ModernUIHelper.createStyledButton("Setujui Final (Kunci Ruangan)", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnApprove.addActionListener(e -> handleFinalApproval(true));

        actions.add(btnRefresh);
        actions.add(btnReject);
        actions.add(btnApprove);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCampusBuildingContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        JPanel header = ModernUIHelper.createPageHeader(
                "Manajemen Multi-Kampus & Gedung Perkuliahan",
                "Konfigurasi seluruh lokasi kampus dan gedung bertingkat di lingkungan universitas.",
                null
        );
        panel.add(header, BorderLayout.NORTH);

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 12, 12));
        centerGrid.setOpaque(false);

        // Left Card: Kampus
        JPanel pnlCampus = new JPanel(new BorderLayout(8, 8));
        pnlCampus.setBackground(Color.WHITE);
        pnlCampus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblCampusTitle = new JLabel("Daftar Kampus");
        lblCampusTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCampusTitle.setForeground(ModernUIHelper.TEXT_DARK);
        pnlCampus.add(lblCampusTitle, BorderLayout.NORTH);

        modelCampuses = new DefaultTableModel(new String[]{"ID", "Kode", "Nama Kampus", "Alamat", "Kota"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCampuses = new JTable(modelCampuses);
        ModernUIHelper.styleModernTable(tblCampuses);
        JScrollPane spCampus = new JScrollPane(tblCampuses);
        spCampus.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
        pnlCampus.add(spCampus, BorderLayout.CENTER);

        JPanel btnCampusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnCampusPanel.setOpaque(false);
        JButton btnAddCampus = ModernUIHelper.createStyledButton("➕ Tambah Kampus Baru", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 11, true);
        btnAddCampus.addActionListener(e -> showAddCampusDialog());
        btnCampusPanel.add(btnAddCampus);
        pnlCampus.add(btnCampusPanel, BorderLayout.SOUTH);

        // Right Card: Gedung
        JPanel pnlBuilding = new JPanel(new BorderLayout(8, 8));
        pnlBuilding.setBackground(Color.WHITE);
        pnlBuilding.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblBldTitle = new JLabel("Daftar Gedung Perkuliahan");
        lblBldTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblBldTitle.setForeground(ModernUIHelper.TEXT_DARK);
        pnlBuilding.add(lblBldTitle, BorderLayout.NORTH);

        modelBuildings = new DefaultTableModel(new String[]{"ID", "Kampus ID", "Kode Gedung", "Nama Gedung", "Lantai"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBuildings = new JTable(modelBuildings);
        ModernUIHelper.styleModernTable(tblBuildings);
        JScrollPane spBuilding = new JScrollPane(tblBuildings);
        spBuilding.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
        pnlBuilding.add(spBuilding, BorderLayout.CENTER);

        JPanel btnBuildingPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnBuildingPanel.setOpaque(false);
        JButton btnAddBuilding = ModernUIHelper.createStyledButton("➕ Tambah Gedung Baru", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 11, true);
        btnAddBuilding.addActionListener(e -> showAddBuildingDialog());
        btnBuildingPanel.add(btnAddBuilding);
        pnlBuilding.add(btnBuildingPanel, BorderLayout.SOUTH);

        centerGrid.add(pnlCampus);
        centerGrid.add(pnlBuilding);
        panel.add(centerGrid, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRoomManagementContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblRoomCount = new JLabel("0 Ruangan Terdaftar");
        lblRoomCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblRoomCount.setOpaque(true);
        lblRoomCount.setBackground(new Color(224, 231, 255)); // Indigo 100
        lblRoomCount.setForeground(ModernUIHelper.COLOR_PRIMARY);
        lblRoomCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblRoomCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Kelola Master Inventaris Ruangan",
                "Tambah, hapus, dan perbarui spesifikasi ruangan kuliah, laboratorium, serta fasilitas penunjang.",
                rightHeader
        );
        panel.add(header, BorderLayout.NORTH);

        // Table Card Container
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        modelRooms = new DefaultTableModel(new String[]{"ID", "Gedung ID", "Kode", "Nama Ruangan", "Lantai", "Kapasitas", "Tipe", "Fasilitas", "Pos (X,Y)"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = new JTable(modelRooms);
        ModernUIHelper.styleModernTable(tblRooms);
        tblRooms.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblRooms.getColumnModel().getColumn(2).setPreferredWidth(70);

        JScrollPane scrollPane = new JScrollPane(tblRooms);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actions.setOpaque(false);

        JButton btnDeleteRoom = ModernUIHelper.createStyledButton("🗑️ Hapus Ruangan", new Color(254, 226, 226), ModernUIHelper.COLOR_DANGER, 11, false);
        btnDeleteRoom.addActionListener(e -> handleDeleteRoom());

        JButton btnAddRoom = ModernUIHelper.createStyledButton("➕ Tambah Ruangan Baru", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 12, true);
        btnAddRoom.addActionListener(e -> showAddRoomDialog());

        actions.add(btnDeleteRoom);
        actions.add(btnAddRoom);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshData() {
        // 1. Refresh Pending Admin Approval
        modelAdminApproval.setRowCount(0);
        List<Booking> pending = bookingDao.getBookingsByStatus("DIVALIDASI_SEKDEK");
        for (Booking b : pending) {
            modelAdminApproval.addRow(new Object[]{
                    b.getId(),
                    b.getBookingCode(),
                    b.getRoomCode() + " (" + b.getRoomName() + ")",
                    b.getBookingDate(),
                    b.getStartTime() + " - " + b.getEndTime(),
                    b.getCourseName(),
                    b.getLecturerName(),
                    b.getStudentName(),
                    b.getSecretaryNote(),
                    b.getStatusBadgeText()
            });
        }
        if (lblApprovalCount != null) {
            lblApprovalCount.setText(pending.size() + " Pengajuan Siap Disetujui");
        }

        // 2. Refresh Kampus
        modelCampuses.setRowCount(0);
        List<Campus> campuses = campusDao.getAllCampuses();
        for (Campus c : campuses) {
            modelCampuses.addRow(new Object[]{c.getId(), c.getCode(), c.getName(), c.getAddress(), c.getCity()});
        }

        // 3. Refresh Gedung
        modelBuildings.setRowCount(0);
        List<Building> buildings = buildingDao.getAllBuildings();
        for (Building b : buildings) {
            modelBuildings.addRow(new Object[]{b.getId(), b.getCampusId(), b.getCode(), b.getName(), b.getTotalFloors()});
        }

        // 4. Refresh Ruangan
        modelRooms.setRowCount(0);
        List<Room> rooms = roomDao.getAllRooms();
        for (Room r : rooms) {
            modelRooms.addRow(new Object[]{
                    r.getId(),
                    r.getBuildingId(),
                    r.getCode(),
                    r.getName(),
                    r.getFloor(),
                    r.getCapacity() + " orang",
                    r.getRoomType(),
                    r.getFacilities(),
                    "(" + r.getPosX() + ", " + r.getPosY() + ")"
            });
        }
        if (lblRoomCount != null) {
            lblRoomCount.setText(rooms.size() + " Ruangan Terdaftar");
        }
    }

    private void handleFinalApproval(boolean approve) {
        int row = tblAdminApproval.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih permohonan booking yang ingin diproses!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) modelAdminApproval.getValueAt(row, 0);
        String code = (String) modelAdminApproval.getValueAt(row, 1);

        String note = JOptionPane.showInputDialog(this,
                (approve ? "Catatan penetapan admin (misal info pengambilan kunci):" : "Alasan penolakan / instruksi alternatif:"),
                approve ? "Approval Final Booking" : "Tolak Booking",
                JOptionPane.QUESTION_MESSAGE);

        if (note == null && !approve) return;

        String status = approve ? "DISETUJUI" : "DITOLAK";
        if (note == null) note = approve ? "Disetujui Admin Master. Ruangan telah dialokasikan resmi." : "Ditolak oleh admin.";

        if (bookingDao.updateAdminApproval(bookingId, status, note)) {
            JOptionPane.showMessageDialog(this,
                    "Permohonan " + code + " berhasil di-" + (approve ? "SETUJUI dan DIKUNCI" : "TOLAK") + "!",
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshData();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
        }
    }

    private void showAddCampusDialog() {
        JTextField txtCode = new JTextField("KMP-03", 10);
        JTextField txtName = new JTextField("Kampus 3 (Teknologi & Vokasi)", 20);
        JTextField txtAddress = new JTextField("Jl. Palagan Tentara Pelajar KM 9", 20);
        JTextField txtCity = new JTextField("Sleman, Yogyakarta", 15);

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.add(new JLabel("Kode Kampus:")); p.add(txtCode);
        p.add(new JLabel("Nama Kampus:")); p.add(txtName);
        p.add(new JLabel("Alamat:")); p.add(txtAddress);
        p.add(new JLabel("Kota:")); p.add(txtCity);

        int res = JOptionPane.showConfirmDialog(this, p, "Tambah Kampus Baru", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            Campus c = new Campus(txtCode.getText().trim(), txtName.getText().trim(), txtAddress.getText().trim(), txtCity.getText().trim());
            if (campusDao.addCampus(c)) {
                JOptionPane.showMessageDialog(this, "Kampus baru berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }

    private void showAddBuildingDialog() {
        List<Campus> campuses = campusDao.getAllCampuses();
        if (campuses.isEmpty()) return;

        JComboBox<Campus> cbCampus = new JComboBox<>(campuses.toArray(new Campus[0]));
        JTextField txtCode = new JTextField("GDG-D", 10);
        JTextField txtName = new JTextField("Gedung D (Pusat Pembelajaran Digital)", 20);
        JSpinner spFloors = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.add(new JLabel("Pilih Kampus:")); p.add(cbCampus);
        p.add(new JLabel("Kode Gedung:")); p.add(txtCode);
        p.add(new JLabel("Nama Gedung:")); p.add(txtName);
        p.add(new JLabel("Jumlah Lantai:")); p.add(spFloors);

        int res = JOptionPane.showConfirmDialog(this, p, "Tambah Gedung Baru", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            Campus sc = (Campus) cbCampus.getSelectedItem();
            if (sc == null) return;
            Building b = new Building(sc.getId(), txtCode.getText().trim(), txtName.getText().trim(), (Integer) spFloors.getValue(), "images/floorplan_gedung_a.png");
            if (buildingDao.addBuilding(b)) {
                JOptionPane.showMessageDialog(this, "Gedung baru berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }

    private void showAddRoomDialog() {
        List<Building> buildings = buildingDao.getAllBuildings();
        if (buildings.isEmpty()) return;

        JComboBox<Building> cbBuilding = new JComboBox<>(buildings.toArray(new Building[0]));
        JTextField txtCode = new JTextField("R. 209", 10);
        JTextField txtName = new JTextField("Ruang Teori Komputasi Awan", 20);
        JSpinner spFloor = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        JSpinner spCap = new JSpinner(new SpinnerNumberModel(40, 5, 500, 5));
        String[] types = {"Kelas Teori", "Laboratorium", "Smart Classroom", "Studio", "Auditorium", "Ruang Seminar"};
        JComboBox<String> cbType = new JComboBox<>(types);
        JTextField txtFacilities = new JTextField("LCD Proyektor, AC, Whiteboard, Wi-Fi 6, Audio Sound", 20);

        JPanel p = new JPanel(new GridLayout(6, 2, 8, 8));
        p.add(new JLabel("Gedung:")); p.add(cbBuilding);
        p.add(new JLabel("Kode Ruangan:")); p.add(txtCode);
        p.add(new JLabel("Nama Ruangan:")); p.add(txtName);
        p.add(new JLabel("Lantai:")); p.add(spFloor);
        p.add(new JLabel("Kapasitas (Orang):")); p.add(spCap);
        p.add(new JLabel("Tipe & Fasilitas:"));
        JPanel sub = new JPanel(new BorderLayout(5, 0));
        sub.add(cbType, BorderLayout.WEST);
        sub.add(txtFacilities, BorderLayout.CENTER);
        p.add(sub);

        int res = JOptionPane.showConfirmDialog(this, p, "Tambah Ruangan Baru", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            Building b = (Building) cbBuilding.getSelectedItem();
            if (b == null) return;

            Room r = new Room(
                    b.getId(),
                    txtCode.getText().trim(),
                    txtName.getText().trim(),
                    (Integer) spFloor.getValue(),
                    (Integer) spCap.getValue(),
                    (String) cbType.getSelectedItem(),
                    txtFacilities.getText().trim(),
                    360, 200, 180, 140
            );

            if (roomDao.addRoom(r)) {
                JOptionPane.showMessageDialog(this, "Ruangan berhasil ditambahkan! Anda dapat mengatur posisinya di Tab Desainer Denah.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }

    private void handleDeleteRoom() {
        int row = tblRooms.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih ruangan yang ingin dihapus!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = (int) modelRooms.getValueAt(row, 0);
        String code = (String) modelRooms.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this, "Hapus ruangan " + code + "? Tindakan ini juga akan menghapus jadwal terkait.", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (roomDao.deleteRoom(roomId)) {
                JOptionPane.showMessageDialog(this, "Ruangan berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                if (onDataChangedCallback != null) onDataChangedCallback.run();
            }
        }
    }
}
