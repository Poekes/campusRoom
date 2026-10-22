package com.campusroom.ui;

import com.campusroom.dao.BookingDao;
import com.campusroom.model.Booking;
import com.campusroom.model.Room;
import com.campusroom.model.StudentUser;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.AvailabilityService;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Random;
import javax.swing.*;

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
    private JComboBox<String> cbStart;
    private JComboBox<String> cbEnd;
    private JTextField txtCourse;
    private JTextField txtLecturer;
    private JTextField txtNim;
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextArea txtPurpose;

    private final AvailabilityService availabilityService = new AvailabilityService();
    private final BookingDao bookingDao = new BookingDao();

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

        JLabel titleLabel = new JLabel("Formulir Pengajuan Ruangan Kuliah");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Alur Validasi: Diajukan PJ -> Validasi Jadwal (Sekdek) -> Approval (Admin)");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        User currentUser = AuthService.getInstance().getCurrentUser();

        txtRoom = new JTextField(targetRoom != null ? targetRoom.getCode() + " - " + targetRoom.getName() + " (Kapasitas: " + targetRoom.getCapacity() + " Kursi)" : "");
        txtRoom.setEditable(false);
        txtRoom.setBackground(new Color(241, 245, 249));

        txtDate = new JTextField(initialDate.toString(), 12);
        
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
        addFormRow(formPanel, gbc, row++, "Tanggal Booking (YYYY-MM-DD):", txtDate);
        
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

        add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSubmit = new JButton("Ajukan Pemesanan Ruangan");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSubmit.setBackground(new Color(16, 185, 129));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.addActionListener(e -> handleSubmitBooking());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        add(btnPanel, BorderLayout.SOUTH);
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
            JOptionPane.showMessageDialog(this, "Silakan pilih ruangan terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah! Gunakan format YYYY-MM-DD (contoh: 2026-10-25)", "Peringatan", JOptionPane.WARNING_MESSAGE);
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

        if (course.isEmpty() || nim.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mohon lengkapi data mata kuliah, NIM, dan nama!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cek ketersediaan
        String dayIndo = AvailabilityService.convertDayOfWeekToIndonesian(date.getDayOfWeek());
        AvailabilityService.AvailabilityResult avail = availabilityService.checkAvailability(targetRoom.getId(), date, dayIndo, startTime, endTime);

        if (!avail.isAvailable()) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ Ruangan TIDAK DAPAT DIAJUKAN karena terjadi bentrokan waktu!\n\n" +
                            avail.getOccupantInfo() + "\n\n" +
                            "Silakan pilih slot waktu atau ruangan lain.",
                    "Konflik Jadwal Terdeteksi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Buat kode booking acak unik
        String bookingCode = "BK-" + date.toString().replace("-", "") + "-" + (100 + new Random().nextInt(900));

        User user = AuthService.getInstance().getCurrentUser();
        int userId = (user != null) ? user.getId() : 1;

        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setRoomId(targetRoom.getId());
        booking.setUserId(userId);
        booking.setStudentNim(nim);
        booking.setStudentName(name);
        booking.setStudentPhone(phone);
        booking.setCourseName(course);
        booking.setLecturerName(lecturer);
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);
        booking.setStatus("MENUNGGU_SEKDEK");

        boolean success = bookingDao.addBooking(booking);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "✅ Pengajuan Booking Berhasil Dikirim!\n\n" +
                            "Kode Booking: " + bookingCode + "\n" +
                            "Ruangan: " + targetRoom.getCode() + " - " + targetRoom.getName() + "\n" +
                            "Tanggal & Waktu: " + date + " (" + startTime + " - " + endTime + ")\n" +
                            "Status Saat Ini: Menunggu Validasi Jadwal oleh Sekretaris Dekan.\n\n" +
                            "Silakan pantau status pengajuan Anda.",
                    "Pengajuan Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan pengajuan ke sistem.", "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }
    }
}
