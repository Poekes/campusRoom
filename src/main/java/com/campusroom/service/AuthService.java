package com.campusroom.service;

import com.campusroom.dao.UserDao;
import com.campusroom.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * AuthService - Mengelola sesi autentikasi dan otorisasi pengguna aktif.
 */
public class AuthService {
    private static AuthService instance;
    private final UserDao userDao;
    private User currentUser;
    private final List<AuthListener> listeners = new ArrayList<>();

    public interface AuthListener {
        void onUserChanged(User newUser);
    }

    private AuthService() {
        this.userDao = new UserDao();
        // Awalnya belum login, akan melalui LoginDialog
        this.currentUser = null;
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean login(String username, String password) {
        User user = userDao.authenticate(username, password);
        if (user != null) {
            this.currentUser = user;
            notifyListeners();
            return true;
        }
        return false;
    }

    public boolean quickLogin(String username) {
        User user = userDao.getUserByUsername(username);
        if (user != null) {
            this.currentUser = user;
            notifyListeners();
            return true;
        }
        return false;
    }

    public void switchUserById(int userId) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            this.currentUser = user;
            notifyListeners();
        }
    }

    public void logout() {
        this.currentUser = null;
        notifyListeners();
    }

    public void addListener(AuthListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AuthListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (AuthListener listener : listeners) {
            listener.onUserChanged(currentUser);
        }
    }
}
