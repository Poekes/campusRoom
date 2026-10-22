package com.campusroom.ui;

import com.campusroom.model.Room;
import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

/**
 * RoomDetailDialog - Menampilkan informasi lengkap ruangan dan fasilitasnya.
 */
public class RoomDetailDialog extends JDialog {

    public RoomDetailDialog(JFrame parent, Room room, LocalDate date, String startTime, String endTime, Runnable onBookingClicked) {
        super(parent, "Detail Ruangan - " + (room != null ? room.getCode() : ""), true);
        if (room == null) return;

        setLayout(new BorderLayout());
        setResizable(false);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        boolean isAvail = room.isAvailable();
        headerPanel.setBackground(isAvail ? new Color(6, 95, 70) : new Color(153, 27, 27));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel(room.getCode() + " : " + room.getName());
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel status = new JLabel(isAvail ? "🟢 STATUS: KOSONG / TERSEDIA" : "🔴 STATUS: TERPAKAI / SUDAH DIPAKAI");
        status.setFont(new Font("SansSerif", Font.BOLD, 12));
        status.setForeground(isAvail ? new Color(167, 243, 208) : new Color(254, 202, 202));

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(status, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        contentPanel.add(createDetailRow("Kapasitas Mahasiswa:", room.getCapacity() + " Kursi / Orang"));
        contentPanel.add(createDetailRow("Tipe Ruangan:", room.getRoomType()));
        contentPanel.add(createDetailRow("Lantai:", "Lantai " + room.getFloor()));
        contentPanel.add(createDetailRow("Fasilitas Tersedia:", room.getFacilities()));
        contentPanel.add(createDetailRow("Koordinat Layout Denah:", "X: " + room.getPosX() + " px, Y: " + room.getPosY() + " px (" + room.getWidth() + "x" + room.getHeight() + " px)"));
        contentPanel.add(createDetailRow("Keterangan Penggunaan:", room.getCurrentOccupantInfo()));

        add(contentPanel, BorderLayout.CENTER);

        // Footer Actions
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnClose = new JButton("Tutup");
        btnClose.addActionListener(e -> dispose());
        footerPanel.add(btnClose);

        if (isAvail) {
            JButton btnBook = new JButton("Ajukan Booking Ruangan Ini");
            btnBook.setBackground(new Color(16, 185, 129));
            btnBook.setForeground(Color.WHITE);
            btnBook.setFont(new Font("SansSerif", Font.BOLD, 12));
            btnBook.addActionListener(e -> {
                dispose();
                if (onBookingClicked != null) {
                    onBookingClicked.run();
                }
            });
            footerPanel.add(btnBook);
        }

        add(footerPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setPreferredSize(new Dimension(180, 20));

        JLabel v = new JLabel("<html>" + value + "</html>");
        v.setFont(new Font("SansSerif", Font.PLAIN, 12));
        v.setForeground(new Color(30, 41, 59));

        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
