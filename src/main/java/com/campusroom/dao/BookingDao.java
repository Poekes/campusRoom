package com.campusroom.dao;

import com.campusroom.model.Booking;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDao {

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, r.code AS room_code, r.name AS room_name FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "ORDER BY b.booking_date DESC, b.start_time DESC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Booking> getBookingsByRoomAndDate(int roomId, String date) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, r.code AS room_code, r.name AS room_name FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "WHERE b.room_id = ? AND b.booking_date = ? AND b.status IN ('DIVALIDASI_SEKDEK', 'DISETUJUI') " +
                "ORDER BY b.start_time ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Booking> getBookingsByStatus(String status) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, r.code AS room_code, r.name AS room_name FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "WHERE b.status = ? " +
                "ORDER BY b.booking_date ASC, b.start_time ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, r.code AS room_code, r.name AS room_name FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.id DESC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addBooking(Booking b) {
        String sql = "INSERT INTO bookings (booking_code, room_id, user_id, student_nim, student_name, " +
                "student_phone, course_name, lecturer_name, booking_date, start_time, end_time, purpose, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBookingCode());
            ps.setInt(2, b.getRoomId());
            ps.setInt(3, b.getUserId());
            ps.setString(4, b.getStudentNim());
            ps.setString(5, b.getStudentName());
            ps.setString(6, b.getStudentPhone());
            ps.setString(7, b.getCourseName());
            ps.setString(8, b.getLecturerName());
            ps.setString(9, b.getBookingDate().toString());
            ps.setString(10, b.getStartTime());
            ps.setString(11, b.getEndTime());
            ps.setString(12, b.getPurpose());
            ps.setString(13, b.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSecretaryValidation(int bookingId, String status, String note) {
        String sql = "UPDATE bookings SET status = ?, secretary_note = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, note);
            ps.setInt(3, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateAdminApproval(int bookingId, String status, String note) {
        String sql = "UPDATE bookings SET status = ?, admin_note = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, note);
            ps.setInt(3, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking(
                rs.getInt("id"),
                rs.getString("booking_code"),
                rs.getInt("room_id"),
                rs.getInt("user_id"),
                rs.getString("student_nim"),
                rs.getString("student_name"),
                rs.getString("student_phone"),
                rs.getString("course_name"),
                rs.getString("lecturer_name"),
                LocalDate.parse(rs.getString("booking_date")),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("purpose"),
                rs.getString("status"),
                rs.getString("secretary_note"),
                rs.getString("admin_note")
        );
        b.setRoomCode(rs.getString("room_code"));
        b.setRoomName(rs.getString("room_name"));
        return b;
    }
}
