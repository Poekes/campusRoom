package com.campusroom.model;

/**
 * Building - Entitas Gedung dalam suatu Kampus.
 * Menyimpan informasi gedung serta path gambar denah lantai.
 */
public class Building extends BaseEntity {
    private int campusId;
    private String code;
    private String name;
    private int totalFloors;
    private String floorPlanImagePath;

    public Building() {
        super();
    }

    public Building(int id, int campusId, String code, String name, int totalFloors, String floorPlanImagePath) {
        super(id);
        this.campusId = campusId;
        this.code = code;
        this.name = name;
        this.totalFloors = totalFloors;
        this.floorPlanImagePath = floorPlanImagePath;
    }

    public Building(int campusId, String code, String name, int totalFloors, String floorPlanImagePath) {
        super();
        this.campusId = campusId;
        this.code = code;
        this.name = name;
        this.totalFloors = totalFloors;
        this.floorPlanImagePath = floorPlanImagePath;
    }

    // Encapsulation
    public int getCampusId() {
        return campusId;
    }

    public void setCampusId(int campusId) {
        this.campusId = campusId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public void setTotalFloors(int totalFloors) {
        if (totalFloors < 1) {
            throw new IllegalArgumentException("Jumlah lantai minimal 1");
        }
        this.totalFloors = totalFloors;
    }

    public String getFloorPlanImagePath() {
        return floorPlanImagePath;
    }

    public void setFloorPlanImagePath(String floorPlanImagePath) {
        this.floorPlanImagePath = floorPlanImagePath;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
