package com.campusroom.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FloorPlanDao - Akses database untuk layout denah blueprint per gedung dan per lantai.
 * Mendukung penyimpanan blueprint denah yang berbeda untuk setiap tingkat lantai gedung.
 */
public class FloorPlanDao {
    private final BuildingDao buildingDao = new BuildingDao();

    /**
     * Mengambil path gambar denah untuk gedung dan lantai tertentu.
     * Jika lantai belum memiliki gambar khusus, menggunakan gambar default gedung.
     */
    public String getFloorPlanImage(int buildingId, int floor) {
        String sql = "SELECT image_path FROM floor_plans WHERE building_id = ? AND floor = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buildingId);
            ps.setInt(2, floor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String path = rs.getString("image_path");
                    if (path != null && !path.trim().isEmpty()) {
                        return path;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Fallback ke gambar default gedung jika tersedia
        try {
            com.campusroom.model.Building b = buildingDao.getBuildingById(buildingId);
            if (b != null && b.getFloorPlanImagePath() != null && !b.getFloorPlanImagePath().trim().isEmpty()) {
                return b.getFloorPlanImagePath();
            }
        } catch (Exception ignored) {
        }

        return "images/floorplan_gedung_a.png";
    }

    /**
     * Menyimpan atau memperbarui path gambar denah untuk gedung dan lantai tertentu.
     */
    public boolean saveFloorPlanImage(int buildingId, int floor, String imagePath) {
        String checkSql = "SELECT id FROM floor_plans WHERE building_id = ? AND floor = ?;";
        String updateSql = "UPDATE floor_plans SET image_path = ? WHERE building_id = ? AND floor = ?;";
        String insertSql = "INSERT INTO floor_plans (building_id, floor, image_path) VALUES (?, ?, ?);";

        try (Connection conn = DatabaseHelper.getConnection()) {
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, buildingId);
                checkPs.setInt(2, floor);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setString(1, imagePath);
                    updatePs.setInt(2, buildingId);
                    updatePs.setInt(3, floor);
                    return updatePs.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, buildingId);
                    insertPs.setInt(2, floor);
                    insertPs.setString(3, imagePath);
                    return insertPs.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
