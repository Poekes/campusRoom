package com.campusroom.model;

import java.time.LocalDate;

/**
 * Booking - Entitas Pengajuan Pemesanan Ruangan oleh Mahasiswa/PJ Mata Kuliah.
 * Menyimpan alur validasi bertingkat: Mahasiswa -> Sekretaris Dekan -> Admin Master.
 */
public class Booking extends BaseEntity {
    private String bookingCode;
    private int roomId;
    private int userId;
    private String studentNim;
    private String studentName;
    private String studentPhone;
    private String courseName;
    private String lecturerName;
    private LocalDate bookingDate;
    private String startTime; // "HH:mm"
    private String endTime;   // "HH:mm"
    private String purpose;   // Alasan: Perkuliahan Pengganti, Ujian Susulan, dsb.
    private String status;    // MENUNGGU_SEKDEK, DIVALIDASI_SEKDEK, DISETUJUI, DITOLAK
    private String secretaryNote;
    private String adminNote;

    // Helper display fields
    private String roomCode;
    private String roomName;

    public Booking() {
        super();
        this.status = "MENUNGGU_SEKDEK";
    }

    public Booking(int id, String bookingCode, int roomId, int userId, String studentNim, String studentName,
                   String studentPhone, String courseName, String lecturerName, LocalDate bookingDate,
                   String startTime, String endTime, String purpose, String status,
                   String secretaryNote, String adminNote) {
        super(id);
        this.bookingCode = bookingCode;
        this.roomId = roomId;
        this.userId = userId;
        this.studentNim = studentNim;
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
        this.secretaryNote = secretaryNote;
        this.adminNote = adminNote;
    }

    // Encapsulation
    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStudentNim() {
        return studentNim;
    }

    public void setStudentNim(String studentNim) {
        this.studentNim = studentNim;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
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

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSecretaryNote() {
        return secretaryNote;
    }

    public void setSecretaryNote(String secretaryNote) {
        this.secretaryNote = secretaryNote;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getStatusBadgeText() {
        switch (status) {
            case "MENUNGGU_SEKDEK":
                return "Menunggu Validasi Sekdek";
            case "DIVALIDASI_SEKDEK":
                return "Divalidasi Sekdek (Menunggu Admin)";
            case "DISETUJUI":
                return "Disetujui (Aktif)";
            case "DITOLAK":
                return "Ditolak";
            default:
                return status;
        }
    }
}
