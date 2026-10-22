package com.campusroom.dao;

import com.campusroom.model.AdminUser;
import com.campusroom.model.SecretaryUser;
import com.campusroom.model.StudentUser;
import com.campusroom.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) AND password = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role ASC, full_name ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Factory Method untuk instansiasi objek User secara polimorfik
     */
    private User mapUserFromResultSet(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String pass = rs.getString("password");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");

        if ("MAHASISWA".equalsIgnoreCase(role)) {
            String nim = rs.getString("nim");
            String studyProg = rs.getString("study_program");
            int sem = rs.getInt("semester");
            return new StudentUser(id, username, pass, fullName, email, phone, nim, studyProg, sem);
        } else if ("SEKDEK".equalsIgnoreCase(role)) {
            String empId = rs.getString("employee_id");
            String office = rs.getString("faculty_office");
            return new SecretaryUser(id, username, pass, fullName, email, phone, empId, office);
        } else {
            // ADMIN
            String empId = rs.getString("employee_id");
            String dept = rs.getString("department");
            return new AdminUser(id, username, pass, fullName, email, phone, empId, dept);
        }
    }
}
