package com.campusroom.dao;

import com.campusroom.model.Campus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampusDao {

    public List<Campus> getAllCampuses() {
        List<Campus> list = new ArrayList<>();
        String sql = "SELECT * FROM campuses ORDER BY code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Campus c = new Campus(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getString("city")
                );
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Campus getCampusById(int id) {
        String sql = "SELECT * FROM campuses WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Campus(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("address"),
                            rs.getString("city")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addCampus(Campus campus) {
        String sql = "INSERT INTO campuses (code, name, address, city) VALUES (?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, campus.getCode());
            ps.setString(2, campus.getName());
            ps.setString(3, campus.getAddress());
            ps.setString(4, campus.getCity());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCampus(Campus campus) {
        String sql = "UPDATE campuses SET code = ?, name = ?, address = ?, city = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, campus.getCode());
            ps.setString(2, campus.getName());
            ps.setString(3, campus.getAddress());
            ps.setString(4, campus.getCity());
            ps.setInt(5, campus.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCampus(int id) {
        String sql = "DELETE FROM campuses WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
