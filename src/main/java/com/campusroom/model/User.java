package com.campusroom.model;

/**
 * User - Kelas Abstrak yang merepresentasikan pengguna sistem CampusRoom.
 * Menerapkan prinsip Abstraction, Encapsulation, dan Polymorphism.
 */
public abstract class User extends BaseEntity {
    protected String username;
    protected String password;
    protected String fullName;
    protected String email;
    protected String phone;
    protected String role;

    public User() {
        super();
    }

    public User(int id, String username, String password, String fullName, String email, String phone, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    // Abstract methods untuk Polymorphism
    public abstract String getRoleDisplayName();
    public abstract boolean canApproveBooking();
    public abstract boolean canManageMasterData();
    public abstract boolean canManageRegularSchedule();
    public abstract String getIdentifierLabel();
    public abstract String getIdentifierValue();

    // Encapsulation: Getter dan Setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return fullName + " (" + getRoleDisplayName() + ")";
    }
}
