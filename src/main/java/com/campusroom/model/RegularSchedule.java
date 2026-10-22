package com.campusroom.model;

/**
 * RegularSchedule - Jadwal perkuliahan tetap reguler yang diinputkan oleh Sekretaris Dekan.
 * Digunakan sebagai acuan ketersediaan ruangan mingguan.
 */
public class RegularSchedule extends BaseEntity {
    private int roomId;
    private String dayOfWeek;     // Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, Minggu
    private String startTime;     // format "HH:mm" misal "08:00"
    private String endTime;       // format "HH:mm" misal "10:30"
    private String courseCode;    // misal "TIF210"
    private String courseName;    // misal "Pemrograman Berorientasi Objek"
    private String lecturerName;  // misal "Dr. Ir. Hendra Saputra, M.T."
    private String className;     // misal "TI-3A"
    private String roomCode;      // helper display

    public RegularSchedule() {
        super();
    }

    public RegularSchedule(int id, int roomId, String dayOfWeek, String startTime, String endTime,
                           String courseCode, String courseName, String lecturerName, String className) {
        super(id);
        this.roomId = roomId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
        this.className = className;
    }

    public RegularSchedule(int roomId, String dayOfWeek, String startTime, String endTime,
                           String courseCode, String courseName, String lecturerName, String className) {
        super();
        this.roomId = roomId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
        this.className = className;
    }

    // Encapsulation
    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    @Override
    public String toString() {
        return dayOfWeek + " (" + startTime + " - " + endTime + "): " + courseName + " [" + className + "]";
    }
}
