package com.campusroom.model;

/**
 * SecretaryUser - Subclass dari User untuk peran Sekretaris Dekan.
 * Bertanggung jawab menginputkan jadwal perkuliahan tetap dan memvalidasi pengajuan booking ruangan.
 */
public class SecretaryUser extends User {
    private String employeeId;
    private String facultyOffice;

    public SecretaryUser() {
        super();
        this.role = "SEKDEK";
    }

    public SecretaryUser(int id, String username, String password, String fullName, String email, String phone,
                         String employeeId, String facultyOffice) {
        super(id, username, password, fullName, email, phone, "SEKDEK");
        this.employeeId = employeeId;
        this.facultyOffice = facultyOffice;
    }

    @Override
    public String getRoleDisplayName() {
        return "Sekretaris Dekan";
    }

    @Override
    public boolean canApproveBooking() {
        return true; // Validasi tahap 1 terhadap jadwal tetap
    }

    @Override
    public boolean canManageMasterData() {
        return false; // Master data dikelola Admin
    }

    @Override
    public boolean canManageRegularSchedule() {
        return true; // Sekretaris Dekan menginputkan jadwal tetap perkuliahan
    }

    @Override
    public String getIdentifierLabel() {
        return "NIP / ID Dekanat";
    }

    @Override
    public String getIdentifierValue() {
        return employeeId;
    }

    // Encapsulation
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFacultyOffice() {
        return facultyOffice;
    }

    public void setFacultyOffice(String facultyOffice) {
        this.facultyOffice = facultyOffice;
    }
}
