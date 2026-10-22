package com.campusroom.service;

import com.campusroom.dao.BuildingDao;
import com.campusroom.dao.CampusDao;
import com.campusroom.dao.RoomDao;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.Room;
import java.util.List;

public class MasterDataService {
    private final CampusDao campusDao;
    private final BuildingDao buildingDao;
    private final RoomDao roomDao;

    public MasterDataService() {
        this.campusDao = new CampusDao();
        this.buildingDao = new BuildingDao();
        this.roomDao = new RoomDao();
    }

    public List<Campus> getAllCampuses() {
        return campusDao.getAllCampuses();
    }

    public List<Building> getBuildingsByCampus(int campusId) {
        return buildingDao.getBuildingsByCampusId(campusId);
    }

    public List<Room> getRoomsByBuilding(int buildingId) {
        return roomDao.getRoomsByBuildingId(buildingId);
    }

    public boolean addCampus(Campus campus) {
        return campusDao.addCampus(campus);
    }

    public boolean addBuilding(Building building) {
        return buildingDao.addBuilding(building);
    }

    public boolean addRoom(Room room) {
        return roomDao.addRoom(room);
    }

    public boolean updateRoom(Room room) {
        return roomDao.updateRoom(room);
    }

    public boolean deleteRoom(int roomId) {
        return roomDao.deleteRoom(roomId);
    }

    public boolean updateRoomCoordinates(int roomId, int posX, int posY, int width, int height) {
        return roomDao.updateRoomCoordinates(roomId, posX, posY, width, height);
    }

    public boolean updateBuildingFloorPlan(int buildingId, String imagePath) {
        return buildingDao.updateFloorPlanImage(buildingId, imagePath);
    }
}
