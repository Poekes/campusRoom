package com.campusroom.model;

/**
 * Campus - Entitas yang merepresentasikan unit Kampus.
 * Mendukung arsitektur multi-kampus.
 */
public class Campus extends BaseEntity {
    private String code;
    private String name;
    private String address;
    private String city;

    public Campus() {
        super();
    }

    public Campus(int id, String code, String name, String address, String city) {
        super(id);
        this.code = code;
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public Campus(String code, String name, String address, String city) {
        super();
        this.code = code;
        this.name = name;
        this.address = address;
        this.city = city;
    }

    // Encapsulation
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
