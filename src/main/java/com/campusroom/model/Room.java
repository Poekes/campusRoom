package com.campusroom.model;

/**
 * Room - Entitas Ruangan Kampus.
 * Menyimpan data kapasitas, fasilitas, serta koordinat layout visual pada denah (posX, posY, width, height).
 */
public class Room extends BaseEntity {
    private int buildingId;
    private String code;
    private String name;
    private int floor;
    private int capacity;
    private String roomType;
    private String facilities;
    
    // Koordinat Denah Visual (Layouting)
    private int posX;
    private int posY;
    private int width;
    private int height;

    // Runtime state (tidak disimpan permanen di tabel rooms, dihitung secara dinamis)
    private boolean available = true;
    private String currentOccupantInfo = "";

    public Room() {
        super();
        this.width = 160;
        this.height = 120;
    }

    public Room(int id, int buildingId, String code, String name, int floor, int capacity,
                String roomType, String facilities, int posX, int posY, int width, int height) {
        super(id);
        this.buildingId = buildingId;
        this.code = code;
        this.name = name;
        this.floor = floor;
        this.capacity = capacity;
        this.roomType = roomType;
        this.facilities = facilities;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    public Room(int buildingId, String code, String name, int floor, int capacity,
                String roomType, String facilities, int posX, int posY, int width, int height) {
        super();
        this.buildingId = buildingId;
        this.code = code;
        this.name = name;
        this.floor = floor;
        this.capacity = capacity;
        this.roomType = roomType;
        this.facilities = facilities;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    // Encapsulation dengan validasi
    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
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

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Kapasitas ruangan harus lebih dari 0!");
        }
        this.capacity = capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = Math.max(0, posX);
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = Math.max(0, posY);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(40, width);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(30, height);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getCurrentOccupantInfo() {
        return currentOccupantInfo;
    }

    public void setCurrentOccupantInfo(String currentOccupantInfo) {
        this.currentOccupantInfo = currentOccupantInfo;
    }

    /**
     * Memeriksa apakah suatu koordinat (point) berada di dalam batas ruangan ini pada denah
     */
    public boolean containsPoint(int x, int y) {
        return x >= posX && x <= (posX + width) && y >= posY && y <= (posY + height);
    }

    @Override
    public String toString() {
        return code + " - " + name + " (Kapasitas: " + capacity + " org)";
    }
}
