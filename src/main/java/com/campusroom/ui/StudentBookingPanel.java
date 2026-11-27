package com.campusroom.ui;

import com.campusroom.model.Booking;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.BookingService;
import com.campusroom.service.ServiceResult;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * StudentBookingPanel - Panel riwayat pengajuan dan status permohonan ruangan bagi mahasiswa.
 * Didesain modern, minimalis, dengan page header elegan, tabel berstatus pill badge,
 * serta proteksi kunci data FIKS bagi pengajuan yang telah disetujui.
 */
public class StudentBookingPanel extends JPanel {
    private final BookingService bookingService = new BookingService();
    private final Runnable onNewBookingRequested;

    private JTable tblBookings;
    private DefaultTableModel modelBookings;
    private JTextField txtSearch;
    private JLabel lblTotalCount;
    private JLabel lblSelectionInfo;
    private JButton btnCancelBooking;
    private List<Booking> currentBookings = new ArrayList<>();

    public StudentBookingPanel(Runnable onNewBookingRequested) {
        this.onNewBookingRequested = onNewBookingRequested;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(ModernUIHelper.BG_PAGE);

        // Modern Page Header with Primary Action Button
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);

        lblTotalCount = new JLabel("0 Pengajuan Terdaftar");
        lblTotalCount.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblTotalCount.setOpaque(true);
        lblTotalCount.setBackground(new Color(241, 245, 249)); // Slate 100
        lblTotalCount.setForeground(ModernUIHelper.TEXT_SECONDARY);
        lblTotalCount.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        rightHeader.add(lblTotalCount);

        JButton btnNewBooking = ModernUIHelper.createStyledButton("Ajukan Ruangan", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnNewBooking.addActionListener(e -> {
            if (this.onNewBookingRequested != null) {
                this.onNewBookingRequested.run();
            }
        });
        rightHeader.add(btnNewBooking);

        JPanel header = ModernUIHelper.createPageHeader(
                "Riwayat Pengajuan",
                "Daftar dan status permohonan peminjaman ruangan (maks. H-1; status FIKS tidak dapat diubah).",
                rightHeader
        );

        JPanel policyBanner = ModernUIHelper.createCalloutCard(
                "Informasi Aturan Peminjaman & Penguncian Status",
                "Pemesanan ruangan wajib diajukan maksimal <b>H-1</b> sebelum tanggal penggunaan. Pengajuan yang telah disetujui resmi (<b>FIKS</b>) bersifat permanen dan tidak dapat diubah/dibatalkan.",
                ModernUIHelper.COLOR_PRIMARY,
                ModernUIHelper.COLOR_PRIMARY_LIGHT
        );

        JPanel topWrapper = new JPanel(new BorderLayout(0, 8));
        topWrapper.setOpaque(false);
        topWrapper.add(header, BorderLayout.NORTH);
        topWrapper.add(policyBanner, BorderLayout.SOUTH);
        add(topWrapper, BorderLayout.NORTH);

        // Table Card Container
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));

        String[] cols = {"ID", "Kode Booking", "Ruangan", "Tanggal", "Waktu", "Mata Kuliah", "Dosen", "Alasan", "Catatan Sekdek", "Catatan Admin", "Status Akhir"};
        modelBookings = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBookings = new JTable(modelBookings);
        ModernUIHelper.styleModernTable(tblBookings);
        tblBookings.getColumnModel().getColumn(10).setCellRenderer(ModernUIHelper.createStatusBadgeRenderer());
        tblBookings.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblBookings.getColumnModel().getColumn(1).setPreferredWidth(110);
        tblBookings.getColumnModel().getColumn(10).setPreferredWidth(205);
        tblBookings.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblBookings.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionActionState();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblBookings);
        scrollPane.setBorder(null);

        txtSearch = new JTextField(22);
        ModernUIHelper.attachInstantTableSearch(txtSearch, tblBookings, 1, 2, 3, 4, 5, 6, 7, 10);
        tableCard.add(ModernUIHelper.createTableSearchBar("Cari Pengajuan:", "Kode booking, ruangan, matkul, dosen, status...", txtSearch), BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // Footer Actions & Status Info
        JPanel footerBar = new JPanel(new BorderLayout(10, 0));
        footerBar.setOpaque(false);

        lblSelectionInfo = new JLabel("Pilih baris pengajuan untuk melihat aksi yang tersedia.");
        lblSelectionInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSelectionInfo.setForeground(ModernUIHelper.TEXT_SECONDARY);
        footerBar.add(lblSelectionInfo, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actions.setOpaque(false);

        btnCancelBooking = ModernUIHelper.createStyledButton("Batalkan Pengajuan", new Color(254, 226, 226), ModernUIHelper.COLOR_DANGER, 11, false);
        btnCancelBooking.setEnabled(false);
        btnCancelBooking.addActionListener(e -> handleCancelSelectedBooking());

        JButton btnContactAdmin = ModernUIHelper.createStyledButton("Hubungi Admin (WA: " + ModernUIHelper.ADMIN_PHONE + ")", new Color(220, 252, 231), new Color(22, 101, 52), 11, true);
        btnContactAdmin.setToolTipText("Klik untuk melihat kontak WhatsApp resmi Admin Sarpras");
        btnContactAdmin.addActionListener(e -> ModernUIHelper.showContactAdminDialog(this));

        JButton btnRefresh = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefresh.addActionListener(e -> refreshData());

        actions.add(btnContactAdmin);
        actions.add(btnCancelBooking);
        actions.add(btnRefresh);
        footerBar.add(actions, BorderLayout.EAST);
        add(footerBar, BorderLayout.SOUTH);

        refreshData();
    }

    private void updateSelectionActionState() {
        int viewRow = tblBookings.getSelectedRow();
        if (viewRow == -1) {
            btnCancelBooking.setEnabled(false);
            lblSelectionInfo.setText("Pilih baris pengajuan untuk melihat aksi yang tersedia.");
            lblSelectionInfo.setForeground(ModernUIHelper.TEXT_SECONDARY);
            return;
        }
        int modelRow = tblBookings.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentBookings.size()) return;
        Booking selected = currentBookings.get(modelRow);

        if (bookingService.isBookingFixed(selected)) {
            btnCancelBooking.setEnabled(false);
            btnCancelBooking.setToolTipText("Booking berstatus FIKS (Disetujui Resmi) dan tidak dapat diubah atau dibatalkan.");
            lblSelectionInfo.setText("Status FIKS (Resmi): Pengajuan " + selected.getBookingCode() + " terkunci permanen dan tidak dapat diubah/dibatalkan.");
            lblSelectionInfo.setForeground(new Color(22, 101, 52));
        } else if ("DITOLAK".equalsIgnoreCase(selected.getStatus())) {
            btnCancelBooking.setEnabled(false);
            btnCancelBooking.setToolTipText("Pengajuan yang sudah ditolak tidak dapat dibatalkan.");
            lblSelectionInfo.setText("Pengajuan " + selected.getBookingCode() + " berstatus Ditolak.");
            lblSelectionInfo.setForeground(ModernUIHelper.COLOR_DANGER);
        } else {
            btnCancelBooking.setEnabled(true);
            btnCancelBooking.setToolTipText("Batalkan pengajuan yang masih menunggu validasi.");
            lblSelectionInfo.setText("Pengajuan " + selected.getBookingCode() + " masih menunggu verifikasi dan dapat dibatalkan.");
            lblSelectionInfo.setForeground(ModernUIHelper.TEXT_SECONDARY);
        }
    }

    private void handleCancelSelectedBooking() {
        int viewRow = tblBookings.getSelectedRow();
        if (viewRow == -1) return;
        int modelRow = tblBookings.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentBookings.size()) return;
        Booking selected = currentBookings.get(modelRow);

        if (bookingService.isBookingFixed(selected)) {
            JOptionPane.showMessageDialog(this,
                    "Pengajuan " + selected.getBookingCode() + " sudah berstatus FIKS (DISETUJUI Resmi) dan tidak dapat diubah atau dibatalkan!",
                    "Terkunci (Status FIKS)", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Batalkan pengajuan " + selected.getBookingCode() + " untuk ruangan " + selected.getRoomCode() + "?",
                "Konfirmasi Pembatalan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            User user = AuthService.getInstance().getCurrentUser();
            ServiceResult<Void> res = bookingService.cancelBooking(selected.getId(), user);
            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public void refreshData() {
        Integer prevSelectedId = ModernUIHelper.getSelectedRowId(tblBookings, modelBookings);
        modelBookings.setRowCount(0);
        User user = AuthService.getInstance().getCurrentUser();
        currentBookings = bookingService.getBookingsForUser(user);

        for (Booking b : currentBookings) {
            modelBookings.addRow(new Object[]{
                    b.getId(),
                    b.getBookingCode(),
                    b.getRoomCode() + " (" + b.getRoomName() + ")",
                    b.getBookingDate(),
                    b.getStartTime() + " - " + b.getEndTime(),
                    b.getCourseName(),
                    b.getLecturerName(),
                    b.getPurpose(),
                    (b.getSecretaryNote() != null ? b.getSecretaryNote() : "-"),
                    (b.getAdminNote() != null ? b.getAdminNote() : "-"),
                    b.getStatusBadgeText()
            });
        }

        if (lblTotalCount != null) {
            lblTotalCount.setText(currentBookings.size() + " Pengajuan Terdaftar");
        }
        ModernUIHelper.restoreSelectedRowById(tblBookings, modelBookings, prevSelectedId);
        updateSelectionActionState();
    }
}
