package com.campusroom.model;

/**
 * AdminUser - Subclass dari User untuk peran Admin Master.
 * Memiliki hak akses penuh untuk kelola master data (kampus, gedung, ruangan, denah layout)
 * dan approval akhir booking ruangan.
 */
public class AdminUser extends User {
    private String employeeId;
    private String department;

    public AdminUser() {
        super();
        this.role = "ADMIN";
    }

    public AdminUser(int id, String username, String password, String fullName, String email, String phone,
                     String employeeId, String department) {
        super(id, username, password, fullName, email, phone, "ADMIN");
        this.employeeId = employeeId;
        this.department = department;
    }

    @Override
    public String getRoleDisplayName() {
        return "Administrator Master";
    }

    @Override
    public boolean canApproveBooking() {
        return true; // Final approval & rescheduling
    }

    @Override
    public boolean canManageMasterData() {
        return true; // Kelola Kampus, Gedung, Ruangan, Layout Denah
    }

    @Override
    public boolean canManageRegularSchedule() {
        return true;
    }

    @Override
    public String getIdentifierLabel() {
        return "NIP / ID Pegawai";
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
