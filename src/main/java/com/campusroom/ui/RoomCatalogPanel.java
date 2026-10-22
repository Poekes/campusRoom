package com.campusroom.ui;

import com.campusroom.model.Room;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * RoomCatalogPanel - Katalog tabular semua ruangan dengan pencarian, kapasitas,
 * fasilitas, dan status ketersediaan dinamis.
 */
public class RoomCatalogPanel extends JPanel {
    private List<Room> currentRooms = new ArrayList<>();
    private final TableCallback callback;

    public interface TableCallback {
        void onBookRoom(Room room);
        void onViewDetail(Room room);
    }

    private JTextField txtSearch;
    private JComboBox<String> cbTypeFilter;
    private JComboBox<String> cbFloorFilter;
    private JLabel lblFilteredCount;
    private JTable tblCatalog;
    private DefaultTableModel modelCatalog;

    public RoomCatalogPanel(TableCallback callback) {
        this.callback = callback;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(ModernUIHelper.BG_PAGE);

        initHeaderAndToolbar();
        initTable();
    }

    private void initHeaderAndToolbar() {
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // 1. Page Header
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);
        lblFilteredCount = new JLabel("0 Ruangan Ditemukan");
        lblFilteredCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblFilteredCount.setOpaque(true);
        lblFilteredCount.setBackground(new Color(241, 245, 249));
        lblFilteredCount.setForeground(ModernUIHelper.TEXT_SECONDARY);
        lblFilteredCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblFilteredCount);

        JPanel header = ModernUIHelper.createPageHeader(
                "Katalog & Pencarian Ruangan Kampus",
                "Cari ruangan kuliah, laboratorium, dan auditorium berdasarkan kapasitas, tipe, dan fasilitas penunjang.",
                rightHeader
        );
        topContainer.add(header);
        topContainer.add(Box.createVerticalStrut(6));

        // 2. Search & Filter Bar Card
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        JLabel lblSearchIcon = new JLabel("Cari:");
        lblSearchIcon.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblSearchIcon.setForeground(new Color(71, 85, 105));
        toolbar.add(lblSearchIcon);

        txtSearch = new JTextField(18);
        txtSearch.setPreferredSize(new Dimension(210, 30));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nama, kode, fasilitas...");
        txtSearch.addActionListener(e -> filterRooms());
        toolbar.add(txtSearch);

        JLabel lblType = new JLabel("Tipe:");
        lblType.setFont(new Font("SansSerif", Font.PLAIN, 11));
        toolbar.add(lblType);

        String[] types = {"Semua Tipe", "Kelas Teori", "Smart Classroom", "Laboratorium", "Studio", "Auditorium", "Ruang Seminar", "Administrasi", "Ruang Rapat"};
        cbTypeFilter = new JComboBox<>(types);
        cbTypeFilter.setPreferredSize(new Dimension(145, 30));
        cbTypeFilter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbTypeFilter.addActionListener(e -> filterRooms());
        toolbar.add(cbTypeFilter);

        JLabel lblFloor = new JLabel("Lantai:");
        lblFloor.setFont(new Font("SansSerif", Font.PLAIN, 11));
        toolbar.add(lblFloor);

        String[] floors = {"Semua Lantai", "Lantai 1", "Lantai 2", "Lantai 3", "Lantai 4"};
        cbFloorFilter = new JComboBox<>(floors);
        cbFloorFilter.setPreferredSize(new Dimension(120, 30));
        cbFloorFilter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbFloorFilter.addActionListener(e -> filterRooms());
        toolbar.add(cbFloorFilter);

        JButton btnFilter = ModernUIHelper.createStyledButton("Filter", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 11, true);
        btnFilter.addActionListener(e -> filterRooms());
        toolbar.add(btnFilter);

        JButton btnReset = ModernUIHelper.createStyledButton("Reset", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cbTypeFilter.setSelectedIndex(0);
            if (cbFloorFilter.getItemCount() > 0) cbFloorFilter.setSelectedIndex(0);
            filterRooms();
        });
        toolbar.add(btnReset);

        topContainer.add(toolbar);
        add(topContainer, BorderLayout.NORTH);
    }

    private void initTable() {
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] cols = {"Kode", "Nama Ruangan", "Lantai", "Kapasitas", "Tipe", "Fasilitas Tersedia", "Status Realtime", "Keterangan Penggunaan"};
        modelCatalog = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCatalog = new JTable(modelCatalog);
        ModernUIHelper.styleModernTable(tblCatalog);
        tblCatalog.getColumnModel().getColumn(6).setCellRenderer(ModernUIHelper.createStatusBadgeRenderer());
        tblCatalog.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblCatalog.getColumnModel().getColumn(2).setPreferredWidth(60);
        tblCatalog.getColumnModel().getColumn(3).setPreferredWidth(85);
        tblCatalog.getColumnModel().getColumn(6).setPreferredWidth(150);
        tblCatalog.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(tblCatalog);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottom.setOpaque(false);
        JButton btnDetail = ModernUIHelper.createStyledButton("Lihat Detail Ruangan", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnDetail.addActionListener(e -> {
            Room r = getSelectedRoom();
            if (r != null && callback != null) {
                callback.onViewDetail(r);
            } else {
                JOptionPane.showMessageDialog(this, "Pilih salah satu baris ruangan!", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton btnBook = ModernUIHelper.createStyledButton("Ajukan Booking Ruangan Terpilih ➔", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnBook.addActionListener(e -> {
            Room r = getSelectedRoom();
            if (r != null) {
                if (!r.isAvailable()) {
                    JOptionPane.showMessageDialog(this, "Ruangan ini berstatus TERPAKAI pada slot waktu yang dipilih!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (callback != null) {
                    callback.onBookRoom(r);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih salah satu baris ruangan!", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        bottom.add(btnDetail);
        bottom.add(btnBook);
        add(bottom, BorderLayout.SOUTH);
    }

    public void setRooms(List<Room> rooms) {
        this.currentRooms = (rooms != null) ? rooms : new ArrayList<>();
        updateFloorCombo();
        filterRooms();
    }

    private void updateFloorCombo() {
        if (cbFloorFilter == null) return;
        String currentSelection = (String) cbFloorFilter.getSelectedItem();
        java.util.Set<Integer> floors = new java.util.TreeSet<>();
        for (Room r : currentRooms) {
            floors.add(r.getFloor());
        }

        java.awt.event.ActionListener[] listeners = cbFloorFilter.getActionListeners();
        for (java.awt.event.ActionListener al : listeners) cbFloorFilter.removeActionListener(al);

        cbFloorFilter.removeAllItems();
        cbFloorFilter.addItem("Semua Lantai");
        for (Integer f : floors) {
            cbFloorFilter.addItem("Lantai " + f);
        }

        if (currentSelection != null) {
            for (int i = 0; i < cbFloorFilter.getItemCount(); i++) {
                if (currentSelection.equals(cbFloorFilter.getItemAt(i))) {
                    cbFloorFilter.setSelectedIndex(i);
                    break;
                }
            }
        }

        for (java.awt.event.ActionListener al : listeners) cbFloorFilter.addActionListener(al);
    }

    private void filterRooms() {
        modelCatalog.setRowCount(0);
        String search = txtSearch.getText().trim().toLowerCase();
        String typeFilter = (String) cbTypeFilter.getSelectedItem();

        String floorFilter = (cbFloorFilter != null) ? (String) cbFloorFilter.getSelectedItem() : "Semua Lantai";

        int matchesCount = 0;
        for (Room r : currentRooms) {
            boolean matchesSearch = search.isEmpty() ||
                    r.getCode().toLowerCase().contains(search) ||
                    r.getName().toLowerCase().contains(search) ||
                    r.getFacilities().toLowerCase().contains(search);

            boolean matchesType = "Semua Tipe".equals(typeFilter) || r.getRoomType().equalsIgnoreCase(typeFilter);
            boolean matchesFloor = floorFilter == null || "Semua Lantai".equals(floorFilter) || ("Lantai " + r.getFloor()).equals(floorFilter);

            if (matchesSearch && matchesType && matchesFloor) {
                String statusStr = r.isAvailable() ? "🟢 KOSONG / TERSEDIA" : "🔴 TERPAKAI";
                modelCatalog.addRow(new Object[]{
                        r.getCode(),
                        r.getName(),
                        "Lt. " + r.getFloor(),
                        r.getCapacity() + " Kursi",
                        r.getRoomType(),
                        r.getFacilities(),
                        statusStr,
                        r.getCurrentOccupantInfo()
                });
                matchesCount++;
            }
        }

        if (lblFilteredCount != null) {
            lblFilteredCount.setText(matchesCount + " Ruangan Ditemukan");
        }
    }

    public Room getSelectedRoom() {
        int selectedRow = tblCatalog.getSelectedRow();
        if (selectedRow == -1) return null;
        String code = (String) modelCatalog.getValueAt(selectedRow, 0);
        for (Room r : currentRooms) {
            if (r.getCode().equals(code)) {
                return r;
            }
        }
        return null;
    }
}
