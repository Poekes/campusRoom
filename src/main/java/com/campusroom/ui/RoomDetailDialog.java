package com.campusroom.ui;

import com.campusroom.model.Booking;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import com.campusroom.service.BookingService;
import com.campusroom.service.ScheduleService;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * RoomDetailDialog - Menampilkan informasi lengkap ruangan, fasilitas,
 * serta seluruh daftar jadwal perkuliahan tetap dan peminjaman yang masuk di ruangan tersebut.
 */
public class RoomDetailDialog extends JDialog {
    private final ScheduleService scheduleService = new ScheduleService();
    private final BookingService bookingService = new BookingService();

    private JTabbedPane tabbedPane;
    private int regularScheduleCount = 0;
    private int incomingBookingCount = 0;

    public RoomDetailDialog(JFrame parent, Room room, LocalDate date, String startTime, String endTime, Runnable onBookingClicked) {
        super(parent, "Detail & Jadwal Ruangan - " + (room != null ? room.getCode() : ""), true);
        if (room == null) return;

        List<RegularSchedule> regularSchedules = scheduleService.getSchedulesByRoomId(room.getId());
        List<Booking> incomingBookings = bookingService.getAllIncomingBookingsForRoom(room.getId());
        this.regularScheduleCount = regularSchedules.size();
        this.incomingBookingCount = incomingBookings.size();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(820, 560));
        setMinimumSize(new Dimension(740, 480));
        setResizable(true);

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        boolean isAvail = room.isAvailable();
        headerPanel.setBackground(isAvail ? new Color(6, 95, 70) : new Color(153, 27, 27));
        headerPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel title = new JLabel(room.getCode() + " — " + room.getName());
        title.setFont(ModernUIHelper.fontBold(16));
        title.setForeground(Color.WHITE);

        String sessionInfo = (date != null ? date.toString() : "-") + " (" + (startTime != null ? startTime : "-") + " - " + (endTime != null ? endTime : "-") + ")";
        JLabel status = new JLabel((isAvail ? "● Ruangan Tersedia" : "■ Ruangan Terpakai") + "  •  Sesi Dipilih: " + sessionInfo);
        status.setFont(ModernUIHelper.fontBold(11));
        status.setForeground(isAvail ? new Color(167, 243, 208) : new Color(254, 202, 202));

        titleCol.add(title);
        titleCol.add(Box.createVerticalStrut(4));
        titleCol.add(status);
        headerPanel.add(titleCol, BorderLayout.WEST);

        // Summary Pills on Right
        JPanel pillGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        pillGroup.setOpaque(false);
        pillGroup.add(ModernUIHelper.createPillBadge(regularScheduleCount + " Jadwal Tetap", null, new Color(255, 255, 255, 40), Color.WHITE));
        pillGroup.add(ModernUIHelper.createPillBadge(incomingBookingCount + " Peminjaman Masuk", null, new Color(255, 255, 255, 40), Color.WHITE));
        headerPanel.add(pillGroup, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 2. Tabbed Content
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ModernUIHelper.fontBold(12));
        tabbedPane.setBorder(new EmptyBorder(10, 14, 6, 14));

        tabbedPane.addTab("Spesifikasi & Fasilitas", createSpecTab(room));
        tabbedPane.addTab("Jadwal Tetap Mingguan (" + regularScheduleCount + ")", createRegularScheduleTab(regularSchedules));
        tabbedPane.addTab("Peminjaman Masuk (" + incomingBookingCount + ")", createBookingsTab(incomingBookings));

        // Default fokus ke tab jadwal jika ruangan sedang terpakai atau pengguna ingin melihat agenda
        add(tabbedPane, BorderLayout.CENTER);

        // 3. Footer Actions
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(248, 250, 252));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUIHelper.BORDER_LIGHT),
                new EmptyBorder(10, 18, 10, 18)
        ));

        JLabel footerHint = new JLabel("Menampilkan seluruh jadwal kuliah tetap mingguan dan reservasi yang masuk di " + room.getCode() + ".");
        footerHint.setFont(ModernUIHelper.fontPlain(11));
        footerHint.setForeground(ModernUIHelper.TEXT_SECONDARY);
        footerPanel.add(footerHint, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnClose = ModernUIHelper.createStyledButton("Tutup", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnClose.addActionListener(e -> dispose());
        btnGroup.add(btnClose);

        if (isAvail) {
            JButton btnBook = ModernUIHelper.createStyledButton("Ajukan Booking Ruangan", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 12, true);
            btnBook.addActionListener(e -> {
                dispose();
                if (onBookingClicked != null) {
                    onBookingClicked.run();
                }
            });
            btnGroup.add(btnBook);
        }

        footerPanel.add(btnGroup, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createSpecTab(Room room) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel gridCard = new JPanel(new GridLayout(6, 1, 8, 10));
        gridCard.setBackground(new Color(248, 250, 252));
        gridCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT),
                new EmptyBorder(14, 16, 14, 16)
        ));

        gridCard.add(createDetailRow("Kode & Nama Ruangan", room.getCode() + " — " + room.getName()));
        gridCard.add(createDetailRow("Kapasitas Kursi", room.getCapacity() + " orang"));
        gridCard.add(createDetailRow("Tipe Ruangan", room.getRoomType()));
        gridCard.add(createDetailRow("Lokasi Lantai", "Lantai " + room.getFloor()));
        gridCard.add(createDetailRow("Fasilitas Tersedia", room.getFacilities()));
        gridCard.add(createDetailRow("Dimensi Denah", "Posisi (" + room.getPosX() + ", " + room.getPosY() + ") • Ukuran " + room.getWidth() + " × " + room.getHeight() + " px"));

        panel.add(gridCard, BorderLayout.NORTH);

        JPanel statusCallout = ModernUIHelper.createCalloutCard(
                "Status Penggunaan Sesi Dipilih",
                room.getCurrentOccupantInfo(),
                room.isAvailable() ? ModernUIHelper.COLOR_SUCCESS : ModernUIHelper.COLOR_DANGER,
                room.isAvailable() ? ModernUIHelper.COLOR_SUCCESS_LIGHT : ModernUIHelper.COLOR_DANGER_LIGHT
        );
        panel.add(statusCallout, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRegularScheduleTab(List<RegularSchedule> schedules) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 12, 12, 12));

        String[] cols = {"Hari", "Jam Mulai", "Jam Selesai", "Kode MK", "Nama Mata Kuliah", "Dosen Pengampu", "Kelas"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (RegularSchedule s : schedules) {
            model.addRow(new Object[]{
                    s.getDayOfWeek(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.getCourseCode(),
                    s.getCourseName(),
                    s.getLecturerName(),
                    s.getClassName()
            });
        }

        JTable table = new JTable(model);
        ModernUIHelper.styleModernTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(75);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(210);
        table.getColumnModel().getColumn(5).setPreferredWidth(180);

        JTextField txtSearch = new JTextField(20);
        ModernUIHelper.attachInstantTableSearch(txtSearch, table, 0, 3, 4, 5, 6);
        panel.add(ModernUIHelper.createTableSearchBar("Filter Jadwal:", "Cari hari, mata kuliah, dosen, atau kelas...", txtSearch), BorderLayout.NORTH);

        if (schedules.isEmpty()) {
            JPanel emptyBanner = ModernUIHelper.createCalloutCard(
                    "Belum Ada Jadwal Tetap",
                    "Ruangan ini belum memiliki jadwal perkuliahan rutin semester yang terdaftar.",
                    ModernUIHelper.TEXT_SECONDARY,
                    new Color(248, 250, 252)
            );
            panel.add(emptyBanner, BorderLayout.CENTER);
        } else {
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
            panel.add(scroll, BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createBookingsTab(List<Booking> bookings) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 12, 12, 12));

        String[] cols = {"Kode Booking", "Tanggal", "Waktu", "Mata Kuliah / Kegiatan", "Dosen", "PJ Mahasiswa", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Booking b : bookings) {
            model.addRow(new Object[]{
                    b.getBookingCode(),
                    b.getBookingDate(),
                    b.getStartTime() + " - " + b.getEndTime(),
                    b.getCourseName(),
                    b.getLecturerName(),
                    b.getStudentName() + " (" + b.getStudentNim() + ")",
                    b.getStatusBadgeText()
            });
        }

        JTable table = new JTable(model);
        ModernUIHelper.styleModernTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(ModernUIHelper.createStatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(115);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(6).setPreferredWidth(175);

        JTextField txtSearch = new JTextField(20);
        ModernUIHelper.attachInstantTableSearch(txtSearch, table, 0, 1, 3, 4, 5, 6);
        panel.add(ModernUIHelper.createTableSearchBar("Filter Peminjaman:", "Cari kode booking, tanggal, matkul, PJ, atau status...", txtSearch), BorderLayout.NORTH);

        if (bookings.isEmpty()) {
            JPanel emptyBanner = ModernUIHelper.createCalloutCard(
                    "Belum Ada Peminjaman Masuk",
                    "Saat ini belum ada pengajuan peminjaman aktif atau jadwal kelas pengganti yang masuk di ruangan ini.",
                    ModernUIHelper.TEXT_SECONDARY,
                    new Color(248, 250, 252)
            );
            panel.add(emptyBanner, BorderLayout.CENTER);
        } else {
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
            panel.add(scroll, BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label + ":");
        l.setFont(ModernUIHelper.fontBold(12));
        l.setForeground(ModernUIHelper.TEXT_SECONDARY);
        l.setPreferredSize(new Dimension(170, 22));

        JLabel v = new JLabel("<html>" + (value != null ? value : "-") + "</html>");
        v.setFont(ModernUIHelper.fontPlain(12));
        v.setForeground(ModernUIHelper.TEXT_DARK);

        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public int getRegularScheduleCount() {
        return regularScheduleCount;
    }

    public int getIncomingBookingCount() {
        return incomingBookingCount;
    }
}
