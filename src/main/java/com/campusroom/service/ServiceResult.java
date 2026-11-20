package com.campusroom.service;

/**
 * ServiceResult - Representasi generik hasil operasi pada Service Layer.
 * Mengemas status keberhasilan (success), pesan informatif/kesalahan, dan objek data hasil.
 *
 * @param <T> Tipe data yang dihasilkan oleh operasi bisnis
 */
public class ServiceResult<T> {
    private final boolean success;
    private final String message;
    private final T data;

    public ServiceResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public static <T> ServiceResult<T> success(String message, T data) {
        return new ServiceResult<>(true, message, data);
    }

    public static <T> ServiceResult<T> success(String message) {
        return new ServiceResult<>(true, message, null);
    }

    public static <T> ServiceResult<T> failure(String message) {
        return new ServiceResult<>(false, message, null);
    }
}
