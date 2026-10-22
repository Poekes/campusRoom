package com.campusroom.model;

/**
 * StudentUser - Subclass dari User untuk peran Mahasiswa (PJ Mata Kuliah / Public).
 * Menerapkan prinsip Inheritance dan Polymorphism.
 */
public class StudentUser extends User {
    private String nim;
    private String studyProgram;
    private int semester;

    public StudentUser() {
        super();
        this.role = "MAHASISWA";
    }

    public StudentUser(int id, String username, String password, String fullName, String email, String phone,
                       String nim, String studyProgram, int semester) {
        super(id, username, password, fullName, email, phone, "MAHASISWA");
        this.nim = nim;
        this.studyProgram = studyProgram;
        this.semester = semester;
    }

    @Override
    public String getRoleDisplayName() {
        return "Mahasiswa / PJ Mata Kuliah";
    }

    @Override
    public boolean canApproveBooking() {
        return false; // Mahasiswa hanya bisa mengajukan, bukan menyetujui
    }

    @Override
    public boolean canManageMasterData() {
        return false;
    }

    @Override
    public boolean canManageRegularSchedule() {
        return false;
    }

    @Override
    public String getIdentifierLabel() {
        return "NIM";
    }

    @Override
    public String getIdentifierValue() {
        return nim;
    }

    // Encapsulation
    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getStudyProgram() {
        return studyProgram;
    }

    public void setStudyProgram(String studyProgram) {
        this.studyProgram = studyProgram;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }
}
