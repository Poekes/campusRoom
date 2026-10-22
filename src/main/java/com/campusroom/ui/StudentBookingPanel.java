package com.campusroom.ui;

import com.campusroom.dao.BookingDao;
import com.campusroom.model.Booking;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * StudentBookingPanel - Panel riwayat pengajuan dan status permohonan ruangan bagi mahasiswa.
 * Didesain modern, minimalis, dengan page header elegan dan tabel berstatus pill badge.
 */
public class StudentBookingPanel extends JPanel {
    private final BookingDao bookingDao = new BookingDao();
    private final Runnable onNewBookingRequested;

    private JTable tblBookings;
    private DefaultTableModel modelBookings;
    private JLabel lblTotalCount;

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

        JButton btnNewBooking = ModernUIHelper.createStyledButton("Buat Pengajuan Ruangan", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
        btnNewBooking.addActionListener(e -> {
            if (this.onNewBookingRequested != null) {
                this.onNewBookingRequested.run();
            }
        });
        rightHeader.add(btnNewBooking);

        JPanel header = ModernUIHelper.createPageHeader(
                "Riwayat & Pengajuan Ruangan Kuliah",
                "Lacak status verifikasi permohonan peminjaman ruangan kuliah pengganti secara transparan dan realtime.",
                rightHeader
        );
        add(header, BorderLayout.NORTH);

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
        tblBookings.getColumnModel().getColumn(10).setPreferredWidth(190);

        JScrollPane scrollPane = new JScrollPane(tblBookings);
        scrollPane.setBorder(null);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // Footer Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actions.setOpaque(false);
        JButton btnRefresh = ModernUIHelper.createStyledButton("Muat Ulang", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnRefresh.addActionListener(e -> refreshData());

        actions.add(btnRefresh);
        add(actions, BorderLayout.SOUTH);

        refreshData();
    }

    public void refreshData() {
        modelBookings.setRowCount(0);
        User user = AuthService.getInstance().getCurrentUser();
        List<Booking> list = (user != null && "MAHASISWA".equalsIgnoreCase(user.getRole()))
                ? bookingDao.getBookingsByUserId(user.getId())
                : bookingDao.getAllBookings();

        for (Booking b : list) {
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
            lblTotalCount.setText(list.size() + " Pengajuan Terdaftar");
        }
    }
}
