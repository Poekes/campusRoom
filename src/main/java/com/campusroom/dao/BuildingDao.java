package com.campusroom.dao;

import com.campusroom.model.Building;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BuildingDao {

    public List<Building> getBuildingsByCampusId(int campusId) {
        List<Building> list = new ArrayList<>();
        String sql = "SELECT * FROM buildings WHERE campus_id = ? ORDER BY code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, campusId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Building b = new Building(
                            rs.getInt("id"),
                            rs.getInt("campus_id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("total_floors"),
                            rs.getString("floor_plan_image_path")
                    );
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Building> getAllBuildings() {
        List<Building> list = new ArrayList<>();
        String sql = "SELECT * FROM buildings ORDER BY code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Building b = new Building(
                        rs.getInt("id"),
                        rs.getInt("campus_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("total_floors"),
                        rs.getString("floor_plan_image_path")
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Building getBuildingById(int id) {
        String sql = "SELECT * FROM buildings WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Building(
                            rs.getInt("id"),
                            rs.getInt("campus_id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("total_floors"),
                            rs.getString("floor_plan_image_path")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addBuilding(Building building) {
        String sql = "INSERT INTO buildings (campus_id, code, name, total_floors, floor_plan_image_path) VALUES (?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, building.getCampusId());
            ps.setString(2, building.getCode());
            ps.setString(3, building.getName());
            ps.setInt(4, building.getTotalFloors());
            ps.setString(5, building.getFloorPlanImagePath());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateFloorPlanImage(int buildingId, String imagePath) {
        String sql = "UPDATE buildings SET floor_plan_image_path = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imagePath);
            ps.setInt(2, buildingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTotalFloors(int buildingId, int totalFloors) {
        String sql = "UPDATE buildings SET total_floors = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalFloors);
            ps.setInt(2, buildingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBuilding(Building building) {
        String sql = "UPDATE buildings SET campus_id = ?, code = ?, name = ?, total_floors = ?, floor_plan_image_path = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, building.getCampusId());
            ps.setString(2, building.getCode());
            ps.setString(3, building.getName());
            ps.setInt(4, building.getTotalFloors());
            ps.setString(5, building.getFloorPlanImagePath());
            ps.setInt(6, building.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBuilding(int id) {
        String sql = "DELETE FROM buildings WHERE id = ?;";
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
