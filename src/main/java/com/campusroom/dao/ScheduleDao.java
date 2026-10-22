package com.campusroom.dao;

import com.campusroom.model.RegularSchedule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {

    public List<RegularSchedule> getAllSchedules() {
        List<RegularSchedule> list = new ArrayList<>();
        String sql = "SELECT s.*, r.code AS room_code FROM regular_schedules s " +
                "JOIN rooms r ON s.room_id = r.id " +
                "ORDER BY CASE s.day_of_week " +
                "WHEN 'Senin' THEN 1 WHEN 'Selasa' THEN 2 WHEN 'Rabu' THEN 3 " +
                "WHEN 'Kamis' THEN 4 WHEN 'Jumat' THEN 5 WHEN 'Sabtu' THEN 6 ELSE 7 END, " +
                "s.start_time ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                RegularSchedule s = mapSchedule(rs);
                s.setRoomCode(rs.getString("room_code"));
                list.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<RegularSchedule> getSchedulesByRoomAndDay(int roomId, String dayOfWeek) {
        List<RegularSchedule> list = new ArrayList<>();
        String sql = "SELECT s.*, r.code AS room_code FROM regular_schedules s " +
                "JOIN rooms r ON s.room_id = r.id " +
                "WHERE s.room_id = ? AND LOWER(s.day_of_week) = LOWER(?) " +
                "ORDER BY s.start_time ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegularSchedule s = mapSchedule(rs);
                    s.setRoomCode(rs.getString("room_code"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addSchedule(RegularSchedule s) {
        String sql = "INSERT INTO regular_schedules (room_id, day_of_week, start_time, end_time, " +
                "course_code, course_name, lecturer_name, class_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getRoomId());
            ps.setString(2, s.getDayOfWeek());
            ps.setString(3, s.getStartTime());
            ps.setString(4, s.getEndTime());
            ps.setString(5, s.getCourseCode());
            ps.setString(6, s.getCourseName());
            ps.setString(7, s.getLecturerName());
            ps.setString(8, s.getClassName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSchedule(int id) {
        String sql = "DELETE FROM regular_schedules WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private RegularSchedule mapSchedule(ResultSet rs) throws SQLException {
        return new RegularSchedule(
                rs.getInt("id"),
                rs.getInt("room_id"),
                rs.getString("day_of_week"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getString("lecturer_name"),
                rs.getString("class_name")
        );
    }
}
