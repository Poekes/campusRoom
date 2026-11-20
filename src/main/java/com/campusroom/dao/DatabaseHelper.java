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
 * Memastikan data denah gedung, blueprint per lantai, dan koordinat riil ruangan tersimpan permanen
 * pada seed sejati sehingga tidak terganti saat masa pengembangan.
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

            // Periksa jika tabel campuses kosong, lakukan seed data awal lengkap
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM campuses;");
            if (rs.next() && rs.getInt(1) == 0) {
                seedInitialData(conn);
            } else {
                // Database sudah ada, sinkronisasi data riil terbaru jika diperlukan
                syncRealData(conn);
            }

            System.out.println("Inisialisasi Database SQLite Berhasil.");
        } catch (SQLException e) {
            System.err.println("Kesalahan inisialisasi database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        System.out.println("Memulai Seeding Data Awal Riil CampusRoom...");

        // 1. Seed Kampus (Default hanya Kampus 2)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO campuses (id, code, name, address, city) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, 2);
            ps.setString(2, "KMP-02");
            ps.setString(3, "Kampus 2");
            ps.setString(4, "Jl. Kaliurang KM 14.5, Sleman");
            ps.setString(5, "Yogyakarta");
            ps.executeUpdate();
        }

        // 2. Seed Gedung
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO buildings (id, campus_id, code, name, total_floors, floor_plan_image_path) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setInt(2, 1);
            ps.setString(3, "GDG-A");
            ps.setString(4, "Gedung A (Fakultas Teknik)");
            ps.setInt(5, 5);
            ps.setString(6, "images/floorplan_gedung_a.png");
            ps.executeUpdate();

            ps.setInt(1, 2);
            ps.setInt(2, 1);
            ps.setString(3, "GDG-B");
            ps.setString(4, "Gedung B (Laboratorium Terpadu)");
            ps.setInt(5, 3);
            ps.setString(6, "images/floorplan_gedung_b.png");
            ps.executeUpdate();

            ps.setInt(1, 3);
            ps.setInt(2, 2);
            ps.setString(3, "GDG-K2");
            ps.setString(4, "Gedung Kampus 2");
            ps.setInt(5, 3);
            ps.setString(6, "images/kampus2_lantai_1.png");
            ps.executeUpdate();
        }

        // 3. Seed Blueprint Denah per Lantai
        seedFloorPlans(conn);

        // 4. Seed Seluruh Ruangan Riil (dengan koordinat presisi denah)
        seedRooms(conn);

        // 5. Seed Pengguna (Mahasiswa, Sekdek, Admin)
        seedUsers(conn);

        // 6. Seed Jadwal Tetap Perkuliahan
        seedSchedules(conn);

        // 7. Seed Contoh Booking Pengajuan Mahasiswa
        seedBookings(conn);

        // Atur SQLite auto-increment counter
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'rooms';");
            st.executeUpdate("INSERT INTO sqlite_sequence (name, seq) VALUES ('rooms', 123);");
        }

        System.out.println("Seeding Data Awal Riil Selesai.");
    }

    private static void seedFloorPlans(Connection conn) throws SQLException {
        String sql = "INSERT OR REPLACE INTO floor_plans (building_id, floor, image_path) VALUES (?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Gedung A (Fakultas Teknik) - Lantai 1 s/d 5
            for (int f = 1; f <= 5; f++) {
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
            // Gedung Kampus 2 - Lantai 1 s/d 3
            ps.setInt(1, 3);
            ps.setInt(2, 1);
            ps.setString(3, "images/kampus2_lantai_1.png");
            ps.executeUpdate();

            ps.setInt(1, 3);
            ps.setInt(2, 2);
            ps.setString(3, "images/kampus2_lantai_2.png");
            ps.executeUpdate();

            ps.setInt(1, 3);
            ps.setInt(2, 3);
            ps.setString(3, "images/kampus2_lantai_3.png");
            ps.executeUpdate();
        }
    }

    private static void seedRooms(Connection conn) throws SQLException {
        String sql = "INSERT INTO rooms (id, building_id, code, name, floor, capacity, room_type, facilities, pos_x, pos_y, width, height) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // === GEDUNG A (building_id = 1) ===
            // Lantai 1
            insertRoomWithId(ps, 12, 1, "R. 101", "Tata Usaha & Pelayanan Akademik", 1, 25, "Administrasi", "Front Desk, Komputer Pelayanan, AC, Wi-Fi", 60, 70, 180, 150);
            insertRoomWithId(ps, 13, 1, "R. 102", "Ruang Rapat Senat Fakultas", 1, 30, "Ruang Rapat", "Meja Konferensi Bundar, Proyektor, AC, Mic", 260, 70, 180, 150);
            insertRoomWithId(ps, 14, 1, "R. 103", "Lab Komputasi Dasar & Pemrograman", 1, 35, "Laboratorium", "35 PC All-in-One, LAN, LCD Proyektor, AC", 460, 70, 180, 150);
            insertRoomWithId(ps, 15, 1, "R. 104", "Lounge Dosen & Ruang Konsultasi", 1, 20, "Ruang Konsultasi", "Sofa Diskusi, Wi-Fi 6, Dispenser, AC", 660, 70, 180, 150);

            // Lantai 2 (Koordinat Riil)
            insertRoomWithId(ps, 1, 1, "R. 201", "Ruang Kuliah Teori 1", 2, 40, "Kelas Teori", "LCD Proyektor, AC, Sound System, Whiteboard", 10, 10, 200, 160);
            insertRoomWithId(ps, 2, 1, "R. 202", "Smart Classroom Interactive", 2, 45, "Smart Classroom", "Smart TV 75\", AC 2 Unit, Microfon Nirkabel, Whiteboard", 338, 93, 180, 150);
            insertRoomWithId(ps, 3, 1, "R. 203", "Ruang Kuliah Teori 2", 2, 35, "Kelas Teori", "LCD Proyektor, AC, Whiteboard, Wi-Fi 6", 547, 87, 180, 150);
            insertRoomWithId(ps, 4, 1, "R. 204", "Studio Desain & Perancangan", 2, 30, "Studio", "Workstation PC, Layar Presentasi, AC, Meja Drafting", 762, 86, 180, 150);
            insertRoomWithId(ps, 5, 1, "R. 205", "Lab Pemrograman & AI", 2, 36, "Laboratorium", "36 Unit PC Core i7, LCD Proyektor, AC 2 Unit, LAN Gigabit", 60, 340, 180, 150);
            insertRoomWithId(ps, 6, 1, "R. 206", "Lab Jaringan & Keamanan Siber", 2, 36, "Laboratorium", "Cisco Router Rack, Switch, AC, Whiteboard", 260, 340, 180, 150);
            insertRoomWithId(ps, 7, 1, "R. 207", "Ruang Seminar Mahasiswa", 2, 50, "Ruang Seminar", "Dual Proyektor, Sound System, AC, Podium", 460, 340, 180, 150);
            insertRoomWithId(ps, 8, 1, "R. 208", "Auditorium Mini Teknik", 2, 85, "Auditorium", "Panggung, Kursi Teater, AC Sentral, Proyektor Laser", 663, 340, 180, 150);

            // Lantai 3
            insertRoomWithId(ps, 16, 1, "R. 301", "Ruang Kuliah Teori 3", 3, 40, "Kelas Teori", "LCD Proyektor, Whiteboard, AC", 60, 70, 180, 150);
            insertRoomWithId(ps, 17, 1, "R. 302", "Lab Robotika & Internet of Things (IoT)", 3, 32, "Laboratorium", "Kit Arduino & ESP32, 3D Printer, AC", 260, 70, 180, 150);
            insertRoomWithId(ps, 18, 1, "R. 303", "Studio Animasi & Game Development", 3, 30, "Studio", "PC GPU RTX 4070, Drawing Tablet, AC", 460, 70, 180, 150);
            insertRoomWithId(ps, 19, 1, "R. 304", "Ruang Kuliah Teori 4", 3, 40, "Kelas Teori", "Smart TV, AC, Sound System", 660, 70, 180, 150);

            // Lantai 4 (Koordinat Riil)
            insertRoomWithId(ps, 20, 1, "R. 401", "Lab Komputasi Awan & Server", 4, 25, "Laboratorium", "Server Rack, Fiber Switch, UPS, AC Dingin", 86, 84, 240, 160);
            insertRoomWithId(ps, 21, 1, "R. 402", "Inkubator Bisnis & Startup Kampus", 4, 35, "Ruang Kolaborasi", "Glassboard, Beanbags, Meja Kerja Fleksibel, AC", 387, 90, 260, 160);

            // === GEDUNG B (building_id = 2) ===
            // Lantai 1
            insertRoomWithId(ps, 9, 2, "B. 101", "Lab Multimedia & VR", 1, 30, "Laboratorium", "VR Headsets, High-End Workstation, Green Screen", 60, 70, 240, 150);
            insertRoomWithId(ps, 10, 2, "B. 102", "Ruang Kolaborasi Inovasi", 1, 40, "Ruang Kolaborasi", "Movable Tables, Dual Screen, AC, Glassboard", 320, 70, 240, 150);
            insertRoomWithId(ps, 11, 2, "B. 103", "Ruang Ujian & Sertifikasi", 1, 50, "Ruang Ujian", "50 PC Terkunci Jaringan, CCTV, AC", 580, 70, 260, 150);

            // Lantai 2
            insertRoomWithId(ps, 22, 2, "B. 201", "Lab Elektronika & Mikroprosesor", 2, 30, "Laboratorium", "Oscilloscope, Solder Station, AC", 60, 70, 240, 150);
            insertRoomWithId(ps, 23, 2, "B. 202", "Lab Sistem Tertanam (Embedded)", 2, 30, "Laboratorium", "Workstation PC, Logic Analyzer, AC", 320, 70, 240, 150);
            insertRoomWithId(ps, 24, 2, "B. 203", "Workshop Mekatronika & Fabrikasi", 2, 25, "Workshop", "CNC Mini, Toolsets, AC, Exhaust Fan", 580, 70, 260, 150);

            // Lantai 3
            insertRoomWithId(ps, 25, 2, "B. 301", "Lab Sains Data & Deep Learning", 3, 32, "Laboratorium", "High Performance PC Cluster, AC", 100, 150, 280, 180);
            insertRoomWithId(ps, 26, 2, "B. 302", "Ruang Presentasi & Riset Pasca", 3, 45, "Ruang Seminar", "Dual Screen Presenter, Sound System, AC", 440, 150, 280, 180);

            // === GEDUNG KAMPUS 2 (building_id = 3) ===
            // Lantai 1 (Koordinat Riil Sesuai Denah)
            insertRoomWithId(ps, 27, 3, "C. 101", "Lab Komputer Dasar", 1, 30, "Kelas Teori", "Smart Board 85\", AC, Sound System", 444, 200, 132, 118);

            // Lantai 2 (Koordinat Riil Sesuai Denah)
            insertRoomWithId(ps, 30, 3, "R. lab-lanjut", "Lab Pemrograman", 2, 20, "Laboratorium", "Workstation Riset, Akses Jurnal Scopus, AC", 893, 220, 237, 101);
            insertRoomWithId(ps, 119, 3, "R. jaringan", "lab jaringan", 2, 40, "Laboratorium", "LCD Proyektor, AC, Whiteboard", 587, 251, 91, 175);
            insertRoomWithId(ps, 120, 3, "R. microticing", "microticing", 2, 40, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 493, 250, 83, 176);

            // Lantai 3 (Seluruh 13 Ruangan Riil R. 1 s/d R. 13 Sesuai Blueprint Denah)
            insertRoomWithId(ps, 103, 3, "R. 1", "Ruang Kuliah 1", 3, 40, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 47, 85, 128, 200);
            insertRoomWithId(ps, 104, 3, "R. 2", "Ruang Kuliah 2", 3, 40, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 47, 305, 88, 166);
            insertRoomWithId(ps, 105, 3, "R. 3", "Ruang Kuliah 3", 3, 35, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 148, 346, 80, 124);
            insertRoomWithId(ps, 106, 3, "R. 4", "Ruang Kuliah 4", 3, 35, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 241, 291, 90, 182);
            insertRoomWithId(ps, 107, 3, "R. 5", "Ruang Kuliah 5", 3, 35, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 337, 293, 91, 179);
            insertRoomWithId(ps, 108, 3, "R. 6", "Ruang Kuliah 6", 3, 40, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 517, 294, 89, 178);
            insertRoomWithId(ps, 109, 3, "R. 7", "Ruang Kuliah 7", 3, 40, "Kelas Teori", "LCD Proyektor, AC, Whiteboard", 611, 292, 92, 179);
            insertRoomWithId(ps, 110, 3, "R. 8", "Ruang Kuliah 8", 3, 50, "Smart Classroom", "Smart Board, AC 2 Unit, Sound System", 773, 64, 145, 145);
            insertRoomWithId(ps, 111, 3, "R. 9", "Ruang Kuliah 9", 3, 50, "Smart Classroom", "Smart Board, AC 2 Unit, Sound System", 928, 70, 130, 134);
            insertRoomWithId(ps, 112, 3, "R. 10", "Ruang Kuliah 10", 3, 45, "Smart Classroom", "Smart TV, AC, Sound System", 926, 294, 126, 184);
            insertRoomWithId(ps, 123, 3, "R. 11", "Ruang Kuliah 11", 3, 40, "Smart Classroom", "LCD Proyektor, AC, Whiteboard", 745, 293, 150, 180);
            insertRoomWithId(ps, 122, 3, "R. 12", "Ruang kuliah", 3, 40, "Smart Classroom", "LCD Proyektor, AC, Whiteboard", 1104, 214, 104, 147);
            insertRoomWithId(ps, 121, 3, "R. 13", "Ruang kuliah", 3, 40, "Smart Classroom", "LCD Proyektor, AC, Whiteboard", 1068, 59, 146, 139);
        }
    }

    private static void seedUsers(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id, username, password, full_name, email, phone, role, nim, study_program, semester, employee_id, department, faculty_office) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Mahasiswa (PJ Mata Kuliah)
            ps.setInt(1, 1);
            ps.setString(2, "mahasiswa");
            ps.setString(3, "123");
            ps.setString(4, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(5, "akbar@student.univ.ac.id");
            ps.setString(6, "081234567890");
            ps.setString(7, "MAHASISWA");
            ps.setString(8, "250111019");
            ps.setString(9, "Teknik Informatika");
            ps.setInt(10, 3);
            ps.setString(11, null);
            ps.setString(12, null);
            ps.setString(13, null);
            ps.executeUpdate();

            // Sekretaris Dekan (Sekdek)
            ps.setInt(1, 2);
            ps.setString(2, "sekdek");
            ps.setString(3, "123");
            ps.setString(4, "Ibu Nurul Hidayah, S.Kom.");
            ps.setString(5, "nurul.h@univ.ac.id");
            ps.setString(6, "081987654321");
            ps.setString(7, "SEKDEK");
            ps.setString(8, null);
            ps.setString(9, null);
            ps.setInt(10, 0);
            ps.setString(11, "SEK-FT-004");
            ps.setString(12, "Sekretariat Dekanat Teknik");
            ps.setString(13, "Fakultas Teknik Lantai 1");
            ps.executeUpdate();

            // Admin Master
            ps.setInt(1, 3);
            ps.setString(2, "admin");
            ps.setString(3, "123");
            ps.setString(4, "Bpk. Rahmat Santoso");
            ps.setString(5, "admin.campusroom@univ.ac.id");
            ps.setString(6, "081399887766");
            ps.setString(7, "ADMIN");
            ps.setString(8, null);
            ps.setString(9, null);
            ps.setInt(10, 0);
            ps.setString(11, "ADM-SYS-001");
            ps.setString(12, "Biro Sarana dan Prasarana Kampus");
            ps.setString(13, null);
            ps.executeUpdate();
        }
    }

    private static void seedSchedules(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO regular_schedules (id, room_id, day_of_week, start_time, end_time, course_code, course_name, lecturer_name, class_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Senin
            insertSchedule(ps, 1, 1, "Senin", "08:00", "10:30", "TIF-201", "Pemrograman Berorientasi Objek", "Dr. Ir. Hendra Saputra, M.T.", "TI-3A");
            insertSchedule(ps, 2, 2, "Senin", "10:30", "13:00", "TIF-204", "Basis Data Lanjut", "Ibu Siti Rahma, M.Cs.", "TI-3B");
            insertSchedule(ps, 3, 5, "Senin", "08:00", "11:30", "TIF-209", "Praktikum Kecerdasan Buatan", "Bpk. Agus Santosa, M.Kom.", "TI-3C");
            insertSchedule(ps, 8, 12, "Senin", "08:00", "10:30", "ADM-101", "Rapat Koordinasi Akademik", "Dekan Fakultas Teknik", "Pimpinan");
            insertSchedule(ps, 9, 17, "Senin", "10:30", "13:00", "TIF-305", "Praktikum Sistem IoT", "Ir. Fajar Suryawan, M.Eng.", "TI-5B");
            insertSchedule(ps, 10, 22, "Senin", "08:00", "10:30", "ELK-201", "Praktikum Rangkaian Digital", "Ir. Tri Handoko, M.T.", "EL-3A");
            insertSchedule(ps, 11, 27, "Senin", "13:00", "15:30", "PAS-801", "Metodologi Penelitian Lanjut", "Prof. Dr. Suwandi, M.Sc.", "S2-TI");
            insertSchedule(ps, 12, 12, "Senin", "07:00", "10:00", "TIF-215", "Kecerdasan Buatan & Sistem Pakar", "Prof. Dr. Suwandi, M.Sc.", "TI-3A");

            // Selasa
            insertSchedule(ps, 4, 1, "Selasa", "13:00", "15:30", "TIF-301", "Rekayasa Perangkat Lunak", "Dr. Eng. Wahyu Wibowo", "TI-5A");
            insertSchedule(ps, 5, 6, "Selasa", "08:00", "10:30", "TIF-207", "Jaringan Komputer", "Bpk. Dedi Supriyadi, M.T.", "TI-3A");

            // Rabu
            insertSchedule(ps, 6, 3, "Rabu", "08:00", "10:30", "TIF-105", "Struktur Data & Algoritma", "Ibu Linda Marlina, M.Kom.", "TI-1A");
            insertSchedule(ps, 7, 8, "Rabu", "13:00", "16:00", "GEN-101", "Kuliah Umum Industri 4.0", "Tim Dosen Tamu", "Angkatan 2024");
        }
    }

    private static void seedBookings(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bookings (id, booking_code, room_id, user_id, student_nim, student_name, student_phone, " +
                        "course_name, lecturer_name, booking_date, start_time, end_time, purpose, status, secretary_note, admin_note) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            String today = java.time.LocalDate.now().toString();

            // Booking 1: Status DISETUJUI
            ps.setInt(1, 1);
            ps.setString(2, "BK-202610-001");
            ps.setInt(3, 3); // R. 203
            ps.setInt(4, 1);
            ps.setString(5, "250111019");
            ps.setString(6, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(7, "081234567890");
            ps.setString(8, "Kelas Pengganti Sistem Operasi");
            ps.setString(9, "Bpk. Joko Purnomo, M.Kom.");
            ps.setString(10, today);
            ps.setString(11, "08:00");
            ps.setString(12, "10:30");
            ps.setString(13, "Dosen berhalangan hadir pada pertemuan Senin lalu karena dinas luar");
            ps.setString(14, "DISETUJUI");
            ps.setString(15, "Jadwal tetap kosong pada slot ini, direkomendasikan.");
            ps.setString(16, "Disetujui. Kunci ruangan dapat diambil di pos satpam.");
            ps.executeUpdate();

            // Booking 2: Status DIVALIDASI_SEKDEK
            ps.setInt(1, 2);
            ps.setString(2, "BK-202610-002");
            ps.setInt(3, 7); // R. 207
            ps.setInt(4, 1);
            ps.setString(5, "250111019");
            ps.setString(6, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(7, "081234567890");
            ps.setString(8, "Kuliah Tamu Cyber Security");
            ps.setString(9, "Ibu Melani Siregar, Ph.D.");
            ps.setString(10, today);
            ps.setString(11, "13:00");
            ps.setString(12, "15:30");
            ps.setString(13, "Sesi sharing alumni praktisi industri IT");
            ps.setString(14, "DIVALIDASI_SEKDEK");
            ps.setString(15, "Sudah diverifikasi sekretaris dekan, ruangan kosong.");
            ps.setString(16, null);
            ps.executeUpdate();

            // Booking 3: Status MENUNGGU_SEKDEK
            ps.setInt(1, 3);
            ps.setString(2, "BK-202610-003");
            ps.setInt(3, 2); // R. 202
            ps.setInt(4, 1);
            ps.setString(5, "250111019");
            ps.setString(6, "Muhamad Nur Akbar (PJ Kuliah)");
            ps.setString(7, "081234567890");
            ps.setString(8, "Responsi & Mentoring Coding");
            ps.setString(9, "Asisten Lab PBO");
            ps.setString(10, today);
            ps.setString(11, "15:30");
            ps.setString(12, "17:30");
            ps.setString(13, "Persiapan latihan materi kelompok mahasiswa");
            ps.setString(14, "MENUNGGU_SEKDEK");
            ps.setString(15, null);
            ps.setString(16, null);
            ps.executeUpdate();
        }
    }

    /**
     * Memastikan data riil tersinkronisasi jika database sebelumnya sudah ada.
     */
    private static void syncRealData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Pastikan hanya lokasi Kampus 2 yang aktif secara default (kampus lain dapat ditambah lewat Admin)
            stmt.executeUpdate("DELETE FROM campuses WHERE name LIKE '%Kampus Utama%';");
            stmt.executeUpdate("INSERT OR IGNORE INTO campuses (id, code, name, address, city) VALUES (2, 'KMP-02', 'Kampus 2', 'Jl. Kaliurang KM 14.5, Sleman', 'Yogyakarta');");
            stmt.executeUpdate("UPDATE campuses SET name = 'Kampus 2', code = 'KMP-02' WHERE id = 2;");

            // Pastikan blueprint Kampus 2 sesuai file gambar denah visual
            stmt.executeUpdate("UPDATE buildings SET name = 'Gedung Kampus 2', code = 'GDG-K2', total_floors = 3, floor_plan_image_path = 'images/kampus2_lantai_1.png' WHERE id = 3;");
            stmt.executeUpdate("INSERT OR REPLACE INTO floor_plans (building_id, floor, image_path) VALUES (3, 1, 'images/kampus2_lantai_1.png');");
            stmt.executeUpdate("INSERT OR REPLACE INTO floor_plans (building_id, floor, image_path) VALUES (3, 2, 'images/kampus2_lantai_2.png');");
            stmt.executeUpdate("INSERT OR REPLACE INTO floor_plans (building_id, floor, image_path) VALUES (3, 3, 'images/kampus2_lantai_3.png');");

            // Perbaiki posisi R. 11 di Kampus 2 Lantai 3 jika pos_y masih 880 (di luar kanvas)
            stmt.executeUpdate("UPDATE rooms SET pos_x = 745, pos_y = 293, width = 150, height = 180, name = 'Ruang Kuliah 11' WHERE building_id = 3 AND floor = 3 AND code = 'R. 11' AND pos_y > 600;");

            // Hapus duplikat R.11 jika ada
            stmt.executeUpdate("DELETE FROM rooms WHERE building_id = 3 AND floor = 3 AND code = 'R.11' AND id = 124;");
        } catch (SQLException e) {
            System.err.println("Sinkronisasi data riil: " + e.getMessage());
        }
    }

    private static void insertRoomWithId(PreparedStatement ps, int id, int bId, String code, String name, int floor,
                                         int cap, String type, String fac, int x, int y, int w, int h) throws SQLException {
        ps.setInt(1, id);
        ps.setInt(2, bId);
        ps.setString(3, code);
        ps.setString(4, name);
        ps.setInt(5, floor);
        ps.setInt(6, cap);
        ps.setString(7, type);
        ps.setString(8, fac);
        ps.setInt(9, x);
        ps.setInt(10, y);
        ps.setInt(11, w);
        ps.setInt(12, h);
        ps.executeUpdate();
    }

    private static void insertSchedule(PreparedStatement ps, int id, int rId, String day, String start, String end,
                                       String cCode, String cName, String lec, String cls) throws SQLException {
        ps.setInt(1, id);
        ps.setInt(2, rId);
        ps.setString(3, day);
        ps.setString(4, start);
        ps.setString(5, end);
        ps.setString(6, cCode);
        ps.setString(7, cName);
        ps.setString(8, lec);
        ps.setString(9, cls);
        ps.executeUpdate();
    }

    public static void main(String[] args) {
        initializeDatabase();
        System.out.println("Uji koneksi & seeding database selesai.");
    }
}
