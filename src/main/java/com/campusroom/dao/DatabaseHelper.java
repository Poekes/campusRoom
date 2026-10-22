package com.campusroom.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseHelper - Mengelola koneksi SQLite dan inisialisasi skema tabel otomatis serta data awal (seed).
 */
public class DatabaseHelper {
    private static final String DB_FILE_NAME = "campusroom.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite JDBC tidak ditemukan: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        File dbFile = new File(DB_FILE_NAME);
        boolean isNew = !dbFile.exists() || dbFile.length() == 0;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Aktifkan foreign keys di SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Tabel Kampus (Multi-Campus)
            stmt.execute("CREATE TABLE IF NOT EXISTS campuses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "code TEXT NOT NULL UNIQUE," +
                    "name TEXT NOT NULL," +
                    "address TEXT," +
                    "city TEXT," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 2. Tabel Gedung (Multi-Building)
            stmt.execute("CREATE TABLE IF NOT EXISTS buildings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "campus_id INTEGER NOT NULL," +
                    "code TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "total_floors INTEGER DEFAULT 1," +
                    "floor_plan_image_path TEXT," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(campus_id) REFERENCES campuses(id) ON DELETE CASCADE" +
                    ");");

            // 3. Tabel Ruangan dengan Koordinat Layout Denah
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "building_id INTEGER NOT NULL," +
                    "code TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "floor INTEGER DEFAULT 1," +
                    "capacity INTEGER NOT NULL," +
                    "room_type TEXT," +
                    "facilities TEXT," +
                    "pos_x INTEGER DEFAULT 50," +
                    "pos_y INTEGER DEFAULT 50," +
                    "width INTEGER DEFAULT 160," +
                    "height INTEGER DEFAULT 120," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(building_id) REFERENCES buildings(id) ON DELETE CASCADE" +
                    ");");

            // 4. Tabel Pengguna (Users: Mahasiswa, Sekdek, Admin)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL," +
                    "full_name TEXT NOT NULL," +
                    "email TEXT," +
                    "phone TEXT," +
                    "role TEXT NOT NULL," + // MAHASISWA, SEKDEK, ADMIN
                    "nim TEXT," +
                    "study_program TEXT," +
                    "semester INTEGER," +
                    "employee_id TEXT," +
                    "department TEXT," +
                    "faculty_office TEXT," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 5. Tabel Jadwal Tetap Perkuliahan (Diinputkan Sekretaris Dekan)
            stmt.execute("CREATE TABLE IF NOT EXISTS regular_schedules (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "room_id INTEGER NOT NULL," +
                    "day_of_week TEXT NOT NULL," + // Senin, Selasa, Rabu, Kamis, Jumat, Sabtu
                    "start_time TEXT NOT NULL," +  // "08:00"
                    "end_time TEXT NOT NULL," +    // "10:30"
                    "course_code TEXT," +
                    "course_name TEXT NOT NULL," +
                    "lecturer_name TEXT," +
                    "class_name TEXT," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE" +
                    ");");

            // 6. Tabel Pengajuan Pemesanan Ruangan (Mahasiswa)
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "booking_code TEXT NOT NULL UNIQUE," +
                    "room_id INTEGER NOT NULL," +
                    "user_id INTEGER NOT NULL," +
                    "student_nim TEXT," +
                    "student_name TEXT," +
                    "student_phone TEXT," +
                    "course_name TEXT NOT NULL," +
                    "lecturer_name TEXT," +
                    "booking_date TEXT NOT NULL," + // "YYYY-MM-DD"
                    "start_time TEXT NOT NULL," +   // "08:00"
                    "end_time TEXT NOT NULL," +     // "10:30"
                    "purpose TEXT," +
                    "status TEXT NOT NULL DEFAULT 'MENUNGGU_SEKDEK'," + // MENUNGGU_SEKDEK, DIVALIDASI_SEKDEK, DISETUJUI, DITOLAK
                    "secretary_note TEXT," +
                    "admin_note TEXT," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ");");

            // 7. Tabel Blueprint Denah per Lantai (Multi-Floor Plan Layouts)
            stmt.execute("CREATE TABLE IF NOT EXISTS floor_plans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "building_id INTEGER NOT NULL," +
                    "floor INTEGER NOT NULL," +
                    "image_path TEXT NOT NULL," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(building_id, floor)," +
                    "FOREIGN KEY(building_id) REFERENCES buildings(id) ON DELETE CASCADE" +
                    ");");

            // Periksa jika tabel campuses kosong, lakukan seed data awal
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM campuses;");
            if (rs.next() && rs.getInt(1) == 0) {
                seedInitialData(conn);
            }

            // Periksa jika floor_plans kosong, lakukan seed denah per lantai
            ResultSet rsFp = stmt.executeQuery("SELECT COUNT(*) FROM floor_plans;");
            if (rsFp.next() && rsFp.getInt(1) == 0) {
                seedFloorPlans(conn);
            }

            // Periksa jika ruangan multi-lantai belum ada, lakukan seed ruangan lantai lainnya
            ResultSet rsRooms = stmt.executeQuery("SELECT COUNT(*) FROM rooms WHERE building_id = 1 AND floor = 1;");
            if (rsRooms.next() && rsRooms.getInt(1) == 0) {
                seedAdditionalFloorRooms(conn);
            }

            System.out.println("Inisialisasi Database SQLite Berhasil.");
        } catch (SQLException e) {
            System.err.println("Kesalahan inisialisasi database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        System.out.println("Memulai Seeding Data Awal CampusRoom...");

        // 1. Seed Kampus
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO campuses (code, name, address, city) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, "KMP-01");
            ps.setString(2, "Kampus Utama (Pusat)");
            ps.setString(3, "Jl. Ring Road Utara No. 1, Sleman");
            ps.setString(4, "Yogyakarta");
            ps.executeUpdate();

            ps.setString(1, "KMP-02");
            ps.setString(2, "Kampus Terpadu (Sains & Industri)");
            ps.setString(3, "Jl. Kaliurang KM 14.5, Sleman");
            ps.setString(4, "Yogyakarta");
            ps.executeUpdate();
        }

        // 2. Seed Gedung
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO buildings (campus_id, code, name, total_floors, floor_plan_image_path) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, "GDG-A");
            ps.setString(3, "Gedung A (Fakultas Teknik)");
            ps.setInt(4, 4);
            ps.setString(5, "images/floorplan_gedung_a.png");
            ps.executeUpdate();

            ps.setInt(1, 1);
            ps.setString(2, "GDG-B");
            ps.setString(3, "Gedung B (Laboratorium Terpadu)");
            ps.setInt(4, 3);
            ps.setString(5, "images/floorplan_gedung_b.png");
            ps.executeUpdate();

            ps.setInt(1, 2);
            ps.setString(2, "GDG-C");
            ps.setString(3, "Gedung Pascasarjana & Riset");
            ps.setInt(4, 3);
            ps.setString(5, "images/floorplan_gedung_a.png");
            ps.executeUpdate();
        }

        // 3. Seed Ruangan (Gedung A Lantai 2) dengan Koordinat Denah
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO rooms (building_id, code, name, floor, capacity, room_type, facilities, pos_x, pos_y, width, height) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Baris Atas Denah (Y = 70)
            insertRoom(ps, 1, "R. 201", "Ruang Kuliah Teori 1", 2, 40, "Kelas Teori", "LCD Proyektor, AC, Sound System, Whiteboard", 60, 70, 180, 150);
            insertRoom(ps, 1, "R. 202", "Smart Classroom Interactive", 2, 45, "Smart Classroom", "Smart TV 75\", AC 2 Unit, Microfon Nirkabel, Whiteboard", 260, 70, 180, 150);
            insertRoom(ps, 1, "R. 203", "Ruang Kuliah Teori 2", 2, 35, "Kelas Teori", "LCD Proyektor, AC, Whiteboard, Wi-Fi 6", 460, 70, 180, 150);
            insertRoom(ps, 1, "R. 204", "Studio Desain & Perancangan", 2, 30, "Studio", "Workstation PC, Layar Presentasi, AC, Meja Drafting", 660, 70, 180, 150);

            // Baris Bawah Denah (Y = 340)
            insertRoom(ps, 1, "R. 205", "Lab Pemrograman & AI", 2, 36, "Laboratorium", "36 Unit PC Core i7, LCD Proyektor, AC 2 Unit, LAN Gigabit", 60, 340, 180, 150);
            insertRoom(ps, 1, "R. 206", "Lab Jaringan & Keamanan Siber", 2, 36, "Laboratorium", "Cisco Router Rack, Switch, AC, Whiteboard", 260, 340, 180, 150);
            insertRoom(ps, 1, "R. 207", "Ruang Seminar Mahasiswa", 2, 50, "Ruang Seminar", "Dual Proyektor, Sound System, AC, Podium", 460, 340, 180, 150);
            insertRoom(ps, 1, "R. 208", "Auditorium Mini Teknik", 2, 85, "Auditorium", "Panggung, Kursi Teater, AC Sentral, Proyektor Laser", 660, 340, 180, 150);

            // Gedung B Ruangan
            insertRoom(ps, 2, "B. 101", "Lab Multimedia & VR", 1, 30, "Laboratorium", "VR Headsets, High-End Workstation, Green Screen", 60, 70, 240, 150);
            insertRoom(ps, 2, "B. 102", "Ruang Kolaborasi Inovasi", 1, 40, "Ruang Kolaborasi", "Movable Tables, Dual Screen, AC, Glassboard", 320, 70, 240, 150);
            insertRoom(ps, 2, "B. 103", "Ruang Ujian & Sertifikasi", 1, 50, "Ruang Ujian", "50 PC Terkunci Jaringan, CCTV, AC", 580, 70, 260, 150);
        }

        // 4. Seed Users (Mahasiswa, Sekdek, Admin)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password, full_name, email, phone, role, nim, study_program, semester, employee_id, department, faculty_office) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Mahasiswa (PJ Mata Kuliah)
            ps.setString(1, "mahasiswa");
            ps.setString(2, "123");
            ps.setString(3, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(4, "akbar@student.univ.ac.id");
            ps.setString(5, "081234567890");
            ps.setString(6, "MAHASISWA");
            ps.setString(7, "250111019");
            ps.setString(8, "Teknik Informatika");
            ps.setInt(9, 3);
            ps.setString(10, null);
            ps.setString(11, null);
            ps.setString(12, null);
            ps.executeUpdate();

            // Sekretaris Dekan (Sekdek)
            ps.setString(1, "sekdek");
            ps.setString(2, "123");
            ps.setString(3, "Ibu Nurul Hidayah, S.Kom.");
            ps.setString(4, "nurul.h@univ.ac.id");
            ps.setString(5, "081987654321");
            ps.setString(6, "SEKDEK");
            ps.setString(7, null);
            ps.setString(8, null);
            ps.setInt(9, 0);
            ps.setString(10, "SEK-FT-004");
            ps.setString(11, "Sekretariat Dekanat Teknik");
            ps.setString(12, "Fakultas Teknik Lantai 1");
            ps.executeUpdate();

            // Admin Master
            ps.setString(1, "admin");
            ps.setString(2, "123");
            ps.setString(3, "Bpk. Rahmat Santoso");
            ps.setString(4, "admin.campusroom@univ.ac.id");
            ps.setString(5, "081399887766");
            ps.setString(6, "ADMIN");
            ps.setString(7, null);
            ps.setString(8, null);
            ps.setInt(9, 0);
            ps.setString(10, "ADM-SYS-001");
            ps.setString(11, "Biro Sarana dan Prasarana Kampus");
            ps.setString(12, null);
            ps.executeUpdate();
        }

        // 5. Seed Jadwal Tetap Perkuliahan (Regular Schedules)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO regular_schedules (room_id, day_of_week, start_time, end_time, course_code, course_name, lecturer_name, class_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Senin
            insertSchedule(ps, 1, "Senin", "08:00", "10:30", "TIF-201", "Pemrograman Berorientasi Objek", "Dr. Ir. Hendra Saputra, M.T.", "TI-3A");
            insertSchedule(ps, 2, "Senin", "10:30", "13:00", "TIF-204", "Basis Data Lanjut", "Ibu Siti Rahma, M.Cs.", "TI-3B");
            insertSchedule(ps, 5, "Senin", "08:00", "11:30", "TIF-209", "Praktikum Kecerdasan Buatan", "Bpk. Agus Santosa, M.Kom.", "TI-3C");
            
            // Selasa
            insertSchedule(ps, 1, "Selasa", "13:00", "15:30", "TIF-301", "Rekayasa Perangkat Lunak", "Dr. Eng. Wahyu Wibowo", "TI-5A");
            insertSchedule(ps, 6, "Selasa", "08:00", "10:30", "TIF-207", "Jaringan Komputer", "Bpk. Dedi Supriyadi, M.T.", "TI-3A");

            // Rabu
            insertSchedule(ps, 3, "Rabu", "08:00", "10:30", "TIF-105", "Struktur Data & Algoritma", "Ibu Linda Marlina, M.Kom.", "TI-1A");
            insertSchedule(ps, 8, "Rabu", "13:00", "16:00", "GEN-101", "Kuliah Umum Industri 4.0", "Tim Dosen Tamu", "Angkatan 2024");
        }

        // 6. Seed Contoh Booking Pengajuan Mahasiswa
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bookings (booking_code, room_id, user_id, student_nim, student_name, student_phone, " +
                        "course_name, lecturer_name, booking_date, start_time, end_time, purpose, status, secretary_note, admin_note) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            String today = java.time.LocalDate.now().toString();

            // Booking 1: Status DISETUJUI (Hari ini jam 08:00 - 10:30 di R. 203) -> Ruang 203 akan MERAH!
            ps.setString(1, "BK-202610-001");
            ps.setInt(2, 3); // R. 203
            ps.setInt(3, 1);
            ps.setString(4, "250111019");
            ps.setString(5, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(6, "081234567890");
            ps.setString(7, "Kelas Pengganti Sistem Operasi");
            ps.setString(8, "Bpk. Joko Purnomo, M.Kom.");
            ps.setString(9, today);
            ps.setString(10, "08:00");
            ps.setString(11, "10:30");
            ps.setString(12, "Dosen berhalangan hadir pada pertemuan Senin lalu karena dinas luar");
            ps.setString(13, "DISETUJUI");
            ps.setString(14, "Jadwal tetap kosong pada slot ini, direkomendasikan.");
            ps.setString(15, "Disetujui. Kunci ruangan dapat diambil di pos satpam.");
            ps.executeUpdate();

            // Booking 2: Status DIVALIDASI_SEKDEK (Menunggu approval Admin)
            ps.setString(1, "BK-202610-002");
            ps.setInt(2, 7); // R. 207
            ps.setInt(3, 1);
            ps.setString(4, "250111019");
            ps.setString(5, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(6, "081234567890");
            ps.setString(7, "Kuliah Tamu Cyber Security");
            ps.setString(8, "Ibu Melani Siregar, Ph.D.");
            ps.setString(9, today);
            ps.setString(10, "13:00");
            ps.setString(11, "15:30");
            ps.setString(12, "Sesi sharing alumni praktisi industri IT");
            ps.setString(13, "DIVALIDASI_SEKDEK");
            ps.setString(14, "Sudah diverifikasi sekretaris dekan, ruangan kosong.");
            ps.setString(15, null);
            ps.executeUpdate();

            // Booking 3: Status MENUNGGU_SEKDEK (Pengajuan baru)
            ps.setString(1, "BK-202610-003");
            ps.setInt(2, 2); // R. 202
            ps.setInt(3, 1);
            ps.setString(4, "250111019");
            ps.setString(5, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(6, "081234567890");
            ps.setString(7, "Responsi & Mentoring Coding");
            ps.setString(8, "Asisten Lab PBO");
            ps.setString(9, today);
            ps.setString(10, "15:30");
            ps.setString(11, "17:30");
            ps.setString(12, "Persiapan latihan materi kelompok mahasiswa");
            ps.setString(13, "MENUNGGU_SEKDEK");
            ps.setString(14, null);
            ps.setString(15, null);
            ps.executeUpdate();
        }

        System.out.println("Seeding Data Awal Selesai.");
    }

    private static void seedFloorPlans(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO floor_plans (building_id, floor, image_path) VALUES (?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Gedung A (Fakultas Teknik) - Lantai 1 s/d 4
            for (int f = 1; f <= 4; f++) {
                ps.setInt(1, 1);
                ps.setInt(2, f);
                ps.setString(3, "images/floorplan_gedung_a.png");
                ps.executeUpdate();
            }
            // Gedung B (Laboratorium Terpadu) - Lantai 1 s/d 3
            for (int f = 1; f <= 3; f++) {
                ps.setInt(1, 2);
                ps.setInt(2, f);
                ps.setString(3, "images/floorplan_gedung_b.png");
                ps.executeUpdate();
            }
            // Gedung C (Pascasarjana) - Lantai 1 s/d 3
            for (int f = 1; f <= 3; f++) {
                ps.setInt(1, 3);
                ps.setInt(2, f);
                ps.setString(3, "images/floorplan_gedung_a.png");
                ps.executeUpdate();
            }
        }
    }

    private static void seedAdditionalFloorRooms(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO rooms (building_id, code, name, floor, capacity, room_type, facilities, pos_x, pos_y, width, height) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            // Gedung A Lantai 1
            insertRoom(ps, 1, "R. 101", "Tata Usaha & Pelayanan Akademik", 1, 25, "Administrasi", "Front Desk, Komputer Pelayanan, AC, Wi-Fi", 60, 70, 180, 150);
            insertRoom(ps, 1, "R. 102", "Ruang Rapat Senat Fakultas", 1, 30, "Ruang Rapat", "Meja Konferensi Bundar, Proyektor, AC, Mic", 260, 70, 180, 150);
            insertRoom(ps, 1, "R. 103", "Lab Komputasi Dasar & Pemrograman", 1, 35, "Laboratorium", "35 PC All-in-One, LAN, LCD Proyektor, AC", 460, 70, 180, 150);
            insertRoom(ps, 1, "R. 104", "Lounge Dosen & Ruang Konsultasi", 1, 20, "Ruang Konsultasi", "Sofa Diskusi, Wi-Fi 6, Dispenser, AC", 660, 70, 180, 150);

            // Gedung A Lantai 3
            insertRoom(ps, 1, "R. 301", "Ruang Kuliah Teori 3", 3, 40, "Kelas Teori", "LCD Proyektor, Whiteboard, AC", 60, 70, 180, 150);
            insertRoom(ps, 1, "R. 302", "Lab Robotika & Internet of Things (IoT)", 3, 32, "Laboratorium", "Kit Arduino & ESP32, 3D Printer, AC", 260, 70, 180, 150);
            insertRoom(ps, 1, "R. 303", "Studio Animasi & Game Development", 3, 30, "Studio", "PC GPU RTX 4070, Drawing Tablet, AC", 460, 70, 180, 150);
            insertRoom(ps, 1, "R. 304", "Ruang Kuliah Teori 4", 3, 40, "Kelas Teori", "Smart TV, AC, Sound System", 660, 70, 180, 150);

            // Gedung A Lantai 4
            insertRoom(ps, 1, "R. 401", "Lab Komputasi Awan & Server", 4, 25, "Laboratorium", "Server Rack, Fiber Switch, UPS, AC Dingin", 160, 180, 240, 160);
            insertRoom(ps, 1, "R. 402", "Inkubator Bisnis & Startup Kampus", 4, 35, "Ruang Kolaborasi", "Glassboard, Beanbags, Meja Kerja Fleksibel, AC", 460, 180, 260, 160);

            // Gedung B Lantai 2
            insertRoom(ps, 2, "B. 201", "Lab Elektronika & Mikroprosesor", 2, 30, "Laboratorium", "Oscilloscope, Solder Station, AC", 60, 70, 240, 150);
            insertRoom(ps, 2, "B. 202", "Lab Sistem Tertanam (Embedded)", 2, 30, "Laboratorium", "Workstation PC, Logic Analyzer, AC", 320, 70, 240, 150);
            insertRoom(ps, 2, "B. 203", "Workshop Mekatronika & Fabrikasi", 2, 25, "Workshop", "CNC Mini, Toolsets, AC, Exhaust Fan", 580, 70, 260, 150);

            // Gedung B Lantai 3
            insertRoom(ps, 2, "B. 301", "Lab Sains Data & Deep Learning", 3, 32, "Laboratorium", "High Performance PC Cluster, AC", 100, 150, 280, 180);
            insertRoom(ps, 2, "B. 302", "Ruang Presentasi & Riset Pasca", 3, 45, "Ruang Seminar", "Dual Screen Presenter, Sound System, AC", 440, 150, 280, 180);

            // Gedung C Lantai 1
            insertRoom(ps, 3, "C. 101", "Ruang Kuliah Magister A", 1, 30, "Kelas Teori", "Smart Board 85\", AC, Sound System", 100, 100, 240, 160);
            insertRoom(ps, 3, "C. 102", "Ruang Kuliah Magister B", 1, 30, "Kelas Teori", "Dual Proyektor, AC, Kursi Eksekutif", 380, 100, 240, 160);
            insertRoom(ps, 3, "C. 103", "Auditorium Pascasarjana", 1, 120, "Auditorium", "Panggung Besar, Tata Suara Profesional, AC Sentral", 150, 300, 440, 180);

            // Gedung C Lantai 2
            insertRoom(ps, 3, "C. 201", "Lab Riset Disertasi & Publikasi", 2, 20, "Laboratorium", "Workstation Riset, Akses Jurnal Scopus, AC", 120, 150, 260, 170);
            insertRoom(ps, 3, "C. 202", "Ruang Sidang Tesis & Disertasi", 2, 25, "Ruang Seminar", "Podium Penguji, Meja U, AC, Layar Teleconference", 420, 150, 260, 170);
        }

        // Tambah jadwal tetap contoh untuk ruangan-ruangan baru
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO regular_schedules (room_id, day_of_week, start_time, end_time, course_code, course_name, lecturer_name, class_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, code FROM rooms WHERE code IN ('R. 101', 'R. 302', 'B. 201', 'C. 101');")) {
                while (rs.next()) {
                    int rId = rs.getInt("id");
                    String code = rs.getString("code");
                    if ("R. 101".equals(code)) {
                        insertSchedule(ps, rId, "Senin", "08:00", "10:30", "ADM-101", "Rapat Koordinasi Akademik", "Dekan Fakultas Teknik", "Pimpinan");
                    } else if ("R. 302".equals(code)) {
                        insertSchedule(ps, rId, "Senin", "10:30", "13:00", "TIF-305", "Praktikum Sistem IoT", "Ir. Fajar Suryawan, M.Eng.", "TI-5B");
                    } else if ("B. 201".equals(code)) {
                        insertSchedule(ps, rId, "Senin", "08:00", "10:30", "ELK-201", "Praktikum Rangkaian Digital", "Ir. Tri Handoko, M.T.", "EL-3A");
                    } else if ("C. 101".equals(code)) {
                        insertSchedule(ps, rId, "Senin", "13:00", "15:30", "PAS-801", "Metodologi Penelitian Lanjut", "Prof. Dr. Suwandi, M.Sc.", "S2-TI");
                    }
                }
            }
        }
        System.out.println("Seeding Ruangan Multi-Lantai Berhasil.");
    }

    private static void insertRoom(PreparedStatement ps, int bId, String code, String name, int floor, int cap,
                                   String type, String fac, int x, int y, int w, int h) throws SQLException {
        ps.setInt(1, bId);
        ps.setString(2, code);
        ps.setString(3, name);
        ps.setInt(4, floor);
        ps.setInt(5, cap);
        ps.setString(6, type);
        ps.setString(7, fac);
        ps.setInt(8, x);
        ps.setInt(9, y);
        ps.setInt(10, w);
        ps.setInt(11, h);
        ps.executeUpdate();
    }

    private static void insertSchedule(PreparedStatement ps, int rId, String day, String start, String end,
                                       String cCode, String cName, String lec, String cls) throws SQLException {
        ps.setInt(1, rId);
        ps.setString(2, day);
        ps.setString(3, start);
        ps.setString(4, end);
        ps.setString(5, cCode);
        ps.setString(6, cName);
        ps.setString(7, lec);
        ps.setString(8, cls);
        ps.executeUpdate();
    }

    public static void main(String[] args) {
        initializeDatabase();
        System.out.println("Uji koneksi & seeding database selesai.");
    }
}
