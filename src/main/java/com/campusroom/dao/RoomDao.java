package com.campusroom.dao;

import com.campusroom.model.Room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDao {

    public List<Room> getRoomsByBuildingId(int buildingId) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE building_id = ? ORDER BY floor ASC, code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room r = new Room(
                            rs.getInt("id"),
                            rs.getInt("building_id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("floor"),
                            rs.getInt("capacity"),
                            rs.getString("room_type"),
                            rs.getString("facilities"),
                            rs.getInt("pos_x"),
                            rs.getInt("pos_y"),
                            rs.getInt("width"),
                            rs.getInt("height")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Room> getRoomsByBuildingAndFloor(int buildingId, int floor) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE building_id = ? AND floor = ? ORDER BY code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buildingId);
            ps.setInt(2, floor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room r = new Room(
                            rs.getInt("id"),
                            rs.getInt("building_id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("floor"),
                            rs.getInt("capacity"),
                            rs.getString("room_type"),
                            rs.getString("facilities"),
                            rs.getInt("pos_x"),
                            rs.getInt("pos_y"),
                            rs.getInt("width"),
                            rs.getInt("height")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Integer> getDistinctFloorsByBuilding(int buildingId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT DISTINCT floor FROM rooms WHERE building_id = ? ORDER BY floor ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("floor"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Room> getAllRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY building_id ASC, code ASC;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Room r = new Room(
                        rs.getInt("id"),
                        rs.getInt("building_id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("floor"),
                        rs.getInt("capacity"),
                        rs.getString("room_type"),
                        rs.getString("facilities"),
                        rs.getInt("pos_x"),
                        rs.getInt("pos_y"),
                        rs.getInt("width"),
                        rs.getInt("height")
                );
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Room getRoomById(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                            rs.getInt("id"),
                            rs.getInt("building_id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("floor"),
                            rs.getInt("capacity"),
                            rs.getString("room_type"),
                            rs.getString("facilities"),
                            rs.getInt("pos_x"),
                            rs.getInt("pos_y"),
                            rs.getInt("width"),
                            rs.getInt("height")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addRoom(Room r) {
        String sql = "INSERT INTO rooms (building_id, code, name, floor, capacity, room_type, facilities, pos_x, pos_y, width, height) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getBuildingId());
            ps.setString(2, r.getCode());
            ps.setString(3, r.getName());
            ps.setInt(4, r.getFloor());
            ps.setInt(5, r.getCapacity());
            ps.setString(6, r.getRoomType());
            ps.setString(7, r.getFacilities());
            ps.setInt(8, r.getPosX());
            ps.setInt(9, r.getPosY());
            ps.setInt(10, r.getWidth());
            ps.setInt(11, r.getHeight());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateRoom(Room r) {
        String sql = "UPDATE rooms SET building_id = ?, code = ?, name = ?, floor = ?, capacity = ?, " +
                "room_type = ?, facilities = ?, pos_x = ?, pos_y = ?, width = ?, height = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getBuildingId());
            ps.setString(2, r.getCode());
            ps.setString(3, r.getName());
            ps.setInt(4, r.getFloor());
            ps.setInt(5, r.getCapacity());
            ps.setString(6, r.getRoomType());
            ps.setString(7, r.getFacilities());
            ps.setInt(8, r.getPosX());
            ps.setInt(9, r.getPosY());
            ps.setInt(10, r.getWidth());
            ps.setInt(11, r.getHeight());
            ps.setInt(12, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateRoomCoordinates(int roomId, int posX, int posY, int width, int height) {
        String sql = "UPDATE rooms SET pos_x = ?, pos_y = ?, width = ?, height = ? WHERE id = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, posX);
            ps.setInt(2, posY);
            ps.setInt(3, width);
            ps.setInt(4, height);
            ps.setInt(5, roomId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRoom(int id) {
        String sql = "DELETE FROM rooms WHERE id = ?;";
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
