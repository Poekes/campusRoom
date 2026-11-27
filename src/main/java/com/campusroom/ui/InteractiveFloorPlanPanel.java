package com.campusroom.ui;

import com.campusroom.model.Room;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * InteractiveFloorPlanPanel - Komponen visual Swing kustom untuk menampilkan denah lantai interaktif.
 * Fitur:
 * 1. Menggambar latar belakang blueprint / gambar denah.
 * 2. Menggambar ruangan dengan pewarnaan dinamis:
 *    - HIJAU (Emerald) = Ruangan KOSONG / TERSEDIA
 *    - MERAH (Crimson) = Ruangan TERPAKAI / SUDAH DIPAKAI
 * 3. Menampilkan kapasitas, nama, dan kode ruangan secara informatif.
 * 4. Interaksi hover dengan tooltip detail dan klik untuk memilih/booking ruangan.
 * 5. Mode Edit Layout: Admin dapat menggeser (drag & drop) posisi ruangan (X, Y) secara langsung.
 */
public class InteractiveFloorPlanPanel extends JPanel {

    public interface FloorPlanListener {
        void onRoomSelected(Room room);
        void onRoomDoubleClicked(Room room);
        void onRoomMoved(Room room, int newX, int newY);
    }

    public interface ZoomChangeListener {
        void onZoomChanged(double newZoom);
    }

    public interface RotationChangeListener {
        void onRotationChanged(int newDegrees);
    }

    private BufferedImage floorImage;
    private String currentImagePath;
    private List<Room> rooms = new ArrayList<>();
    private Room selectedRoom = null;
    private Room hoveredRoom = null;
    private Room searchHighlightRoom = null;
    private FloorPlanListener listener;

    // Canvas Dimension System
    private int logicalWidth = 1050;
    private int logicalHeight = 650;

    // POV Rotation System (0, 90, 180, 270 degrees)
    private int rotationDegrees = 0;
    private final List<RotationChangeListener> rotationListeners = new ArrayList<>();

    // Zoom & Pan System (0.4x - 2.5x)
    private double zoomLevel = 1.0;
    public static final double MIN_ZOOM = 0.40;
    public static final double MAX_ZOOM = 2.50;
    public static final double ZOOM_STEP = 0.15;
    private final List<ZoomChangeListener> zoomListeners = new ArrayList<>();
    private Point lastPanPoint = null;

    private boolean layoutEditMode = false;
    private Point dragOffset = null;
    private boolean isDragging = false;
    private boolean isResizing = false;

    // Palet Warna UI Modern
    private static final Color COLOR_AVAILABLE_BG = new Color(16, 185, 129, 185); // Emerald Green
    private static final Color COLOR_AVAILABLE_BORDER = new Color(5, 150, 105);
    private static final Color COLOR_OCCUPIED_BG = new Color(239, 68, 68, 195);  // Crimson Red
    private static final Color COLOR_OCCUPIED_BORDER = new Color(185, 28, 28);
    private static final Color COLOR_HOVER_BORDER = new Color(56, 189, 248);     // Sky Blue Glow
    private static final Color COLOR_SELECTED_BORDER = new Color(245, 158, 11);  // Amber Gold Focus

    public InteractiveFloorPlanPanel() {
        setBackground(new Color(241, 245, 249));
        setPreferredSize(new Dimension(920, 580));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    lastPanPoint = e.getLocationOnScreen();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                handleMousePressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    if (lastPanPoint != null && getParent() instanceof JViewport) {
                        JViewport vp = (JViewport) getParent();
                        Point current = e.getLocationOnScreen();
                        int dx = current.x - lastPanPoint.x;
                        int dy = current.y - lastPanPoint.y;
                        Point viewPos = vp.getViewPosition();
                        int maxViewX = Math.max(0, getWidth() - vp.getWidth());
                        int maxViewY = Math.max(0, getHeight() - vp.getHeight());
                        vp.setViewPosition(new Point(Math.max(0, Math.min(maxViewX, viewPos.x - dx)), Math.max(0, Math.min(maxViewY, viewPos.y - dy))));
                        lastPanPoint = current;
                    }
                    return;
                }
                handleMouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    lastPanPoint = null;
                    setCursor(layoutEditMode ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) : Cursor.getDefaultCursor());
                    return;
                }
                handleMouseReleased(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && selectedRoom != null && listener != null) {
                    listener.onRoomDoubleClicked(selectedRoom);
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Zoom via Ctrl + Mouse Wheel (Centered to Cursor)
        addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                Point mousePos = e.getPoint();
                double oldZoom = zoomLevel;
                double targetZoom = (e.getWheelRotation() < 0)
                        ? Math.min(MAX_ZOOM, zoomLevel + ZOOM_STEP)
                        : Math.max(MIN_ZOOM, zoomLevel - ZOOM_STEP);
                targetZoom = Math.round(targetZoom * 100.0) / 100.0;

                if (Math.abs(oldZoom - targetZoom) > 0.001) {
                    if (getParent() instanceof JViewport) {
                        JViewport vp = (JViewport) getParent();
                        Point viewPos = vp.getViewPosition();
                        double logicalX = mousePos.x / oldZoom;
                        double logicalY = mousePos.y / oldZoom;

                        setZoomLevel(targetZoom);

                        int newViewX = (int) Math.round(logicalX * targetZoom - (mousePos.x - viewPos.x));
                        int newViewY = (int) Math.round(logicalY * targetZoom - (mousePos.y - viewPos.y));
                        int maxViewX = Math.max(0, getWidth() - vp.getWidth());
                        int maxViewY = Math.max(0, getHeight() - vp.getHeight());
                        vp.setViewPosition(new Point(Math.max(0, Math.min(maxViewX, newViewX)), Math.max(0, Math.min(maxViewY, newViewY))));
                    } else {
                        setZoomLevel(targetZoom);
                    }
                }
                e.consume();
            }
        });
    }

    public void addZoomChangeListener(ZoomChangeListener listener) {
        if (listener != null) {
            zoomListeners.add(listener);
        }
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoom) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        clamped = Math.round(clamped * 100.0) / 100.0;
        if (Math.abs(this.zoomLevel - clamped) > 0.001) {
            this.zoomLevel = clamped;
            updateCanvasDimension();
            for (ZoomChangeListener l : zoomListeners) {
                l.onZoomChanged(this.zoomLevel);
            }
        }
    }

    public void zoomIn() {
        setZoomLevel(zoomLevel + ZOOM_STEP);
    }

    public void zoomOut() {
        setZoomLevel(zoomLevel - ZOOM_STEP);
    }

    public void resetZoom() {
        setZoomLevel(1.0);
    }

    public void addRotationChangeListener(RotationChangeListener listener) {
        if (listener != null) {
            rotationListeners.add(listener);
        }
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        normalized = (normalized / 90) * 90;
        if (this.rotationDegrees != normalized) {
            this.rotationDegrees = normalized;
            updateCanvasDimension();
            repaint();
            for (RotationChangeListener l : rotationListeners) {
                l.onRotationChanged(this.rotationDegrees);
            }
        }
    }

    public void rotateClockwise() {
        setRotationDegrees(rotationDegrees + 90);
    }

    public void rotateCounterClockwise() {
        setRotationDegrees(rotationDegrees - 90);
    }

    public void resetRotation() {
        setRotationDegrees(0);
    }

    public void setFloorPlanListener(FloorPlanListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<Room> rooms) {
        int prevSelectedId = (this.selectedRoom != null) ? this.selectedRoom.getId() : -1;
        int prevHighlightId = (this.searchHighlightRoom != null) ? this.searchHighlightRoom.getId() : -1;
        this.rooms = (rooms != null) ? rooms : new ArrayList<>();

        Room restoredSelected = null;
        Room restoredHighlight = null;
        for (Room r : this.rooms) {
            if (prevSelectedId != -1 && r.getId() == prevSelectedId) {
                restoredSelected = r;
            }
            if (prevHighlightId != -1 && r.getId() == prevHighlightId) {
                restoredHighlight = r;
            }
        }
        this.selectedRoom = restoredSelected;
        this.searchHighlightRoom = restoredHighlight;
        this.hoveredRoom = null;
        updateCanvasDimension();
        repaint();
    }

    public void setSelectedRoom(Room room) {
        this.selectedRoom = room;
        repaint();
    }

    public Room getSelectedRoom() {
        return selectedRoom;
    }

    public void setSearchHighlightRoom(Room room) {
        this.searchHighlightRoom = room;
        repaint();
    }

    public Room getSearchHighlightRoom() {
        return searchHighlightRoom;
    }

    /**
     * Menghitung batas layar (koordinat viewport setelah zoom dan rotasi) untuk sebuah ruangan.
     */
    public Rectangle getScreenBoundsForRoom(Room r) {
        if (r == null) return null;
        int rx = r.getPosX();
        int ry = r.getPosY();
        int rw = r.getWidth();
        int rh = r.getHeight();
        int w = (logicalWidth > 0) ? logicalWidth : 1050;
        int h = (logicalHeight > 0) ? logicalHeight : 650;

        double sx, sy, sw, sh;
        if (rotationDegrees == 90) {
            sx = h - (ry + rh);
            sy = rx;
            sw = rh;
            sh = rw;
        } else if (rotationDegrees == 180) {
            sx = w - (rx + rw);
            sy = h - (ry + rh);
            sw = rw;
            sh = rh;
        } else if (rotationDegrees == 270) {
            sx = ry;
            sy = w - (rx + rw);
            sw = rh;
            sh = rw;
        } else {
            sx = rx;
            sy = ry;
            sw = rw;
            sh = rh;
        }

        return new Rectangle(
                (int) Math.round(sx * zoomLevel),
                (int) Math.round(sy * zoomLevel),
                (int) Math.round(sw * zoomLevel),
                (int) Math.round(sh * zoomLevel)
        );
    }

    public BufferedImage getFloorImage() {
        return floorImage;
    }

    public void setLayoutEditMode(boolean editMode) {
        this.layoutEditMode = editMode;
        setCursor(editMode ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    public boolean isLayoutEditMode() {
        return layoutEditMode;
    }

    /**
     * Memuat gambar denah dari file path absolut atau classpath resource
     */
    public void loadFloorImage(String path) {
        this.currentImagePath = path;
        floorImage = null;
        if (path == null || path.trim().isEmpty()) {
            updateCanvasDimension();
            return;
        }

        try {
            // Coba dari file sistem langsung
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                floorImage = ImageIO.read(file);
            } else {
                // Coba dari resource classpath
                String resPath = path.startsWith("/") ? path : "/" + path;
                try (InputStream is = getClass().getResourceAsStream(resPath)) {
                    if (is != null) {
                        floorImage = ImageIO.read(is);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat gambar denah: " + e.getMessage());
        }

        updateCanvasDimension();
    }

    public void updateCanvasDimension() {
        int baseW = (floorImage != null) ? floorImage.getWidth() : 1050;
        int baseH = (floorImage != null) ? floorImage.getHeight() : 650;

        int maxRoomX = 0;
        int maxRoomY = 0;
        if (rooms != null) {
            for (Room r : rooms) {
                maxRoomX = Math.max(maxRoomX, r.getPosX() + r.getWidth() + 30);
                maxRoomY = Math.max(maxRoomY, r.getPosY() + r.getHeight() + 30);
            }
        }

        int targetW = Math.max(baseW, maxRoomX);
        int targetH = Math.max(baseH, maxRoomY);
        this.logicalWidth = targetW;
        this.logicalHeight = targetH;

        int scaledW;
        int scaledH;
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            scaledW = (int) Math.round(targetH * zoomLevel);
            scaledH = (int) Math.round(targetW * zoomLevel);
        } else {
            scaledW = (int) Math.round(targetW * zoomLevel);
            scaledH = (int) Math.round(targetH * zoomLevel);
        }

        setPreferredSize(new Dimension(scaledW, scaledH));
        revalidate();
        repaint();
    }

    public Point screenToLogical(int screenX, int screenY) {
        double unscaledX = screenX / zoomLevel;
        double unscaledY = screenY / zoomLevel;
        int w = (logicalWidth > 0) ? logicalWidth : 1050;
        int h = (logicalHeight > 0) ? logicalHeight : 650;

        int lx;
        int ly;

        if (rotationDegrees == 90) {
            lx = (int) Math.round(unscaledY);
            ly = (int) Math.round(h - unscaledX);
        } else if (rotationDegrees == 180) {
            lx = (int) Math.round(w - unscaledX);
            ly = (int) Math.round(h - unscaledY);
        } else if (rotationDegrees == 270) {
            lx = (int) Math.round(w - unscaledY);
            ly = (int) Math.round(unscaledX);
        } else {
            lx = (int) Math.round(unscaledX);
            ly = (int) Math.round(unscaledY);
        }

        return new Point(lx, ly);
    }

    private boolean isNearBottomRightCorner(Room r, int x, int y) {
        if (r == null) return false;
        int cornerX = r.getPosX() + r.getWidth();
        int cornerY = r.getPosY() + r.getHeight();
        return (x >= cornerX - 14 && x <= cornerX + 8 && y >= cornerY - 14 && y <= cornerY + 8);
    }

    private void handleMouseMoved(MouseEvent e) {
        Point logical = screenToLogical(e.getX(), e.getY());
        int logicalX = logical.x;
        int logicalY = logical.y;

        if (layoutEditMode && selectedRoom != null && isNearBottomRightCorner(selectedRoom, logicalX, logicalY)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            setToolTipText("Tarik sudut oranye ini untuk mengubah lebar & panjang ruangan");
            return;
        }

        Room found = findRoomAt(logicalX, logicalY);
        if (found != hoveredRoom) {
            hoveredRoom = found;
            if (hoveredRoom != null) {
                setCursor(Cursor.getPredefinedCursor(layoutEditMode ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR));
                String statusBadgeHtml = hoveredRoom.isAvailable()
                        ? "<span style='background:#dcfce7; color:#166534; font-weight:bold; padding:2px 8px; border-radius:4px;'>● TERSEDIA</span>"
                        : "<span style='background:#fee2e2; color:#991b1b; font-weight:bold; padding:2px 8px; border-radius:4px;'>■ TERPAKAI</span>";
                String editHint = layoutEditMode ? "<div style='margin-top:6px; padding-top:4px; border-top:1px solid #e2e8f0; color:#0284c7; font-size:10px;'>Klik ganda untuk properti</div>" : "";
                String tooltip = "<html><div style='font-family:sans-serif; padding:6px; color:#1e293b;'>" +
                        "<div style='font-size:13px; font-weight:bold; color:#0f172a; margin-bottom:4px;'>" + hoveredRoom.getCode() + " • " + hoveredRoom.getName() + "</div>" +
                        "<div style='color:#475569; font-size:11px; margin-bottom:4px;'>" + hoveredRoom.getRoomType() + " &nbsp;•&nbsp; <b>" + hoveredRoom.getCapacity() + " Kursi</b></div>" +
                        "<div style='font-size:11px; color:#334155; margin-bottom:6px;'>Fasilitas: " + hoveredRoom.getFacilities() + "</div>" +
                        "<div style='margin-bottom:4px;'>" + statusBadgeHtml + "</div>" +
                        "<div style='color:#64748b; font-size:10px; font-style:italic;'>" + hoveredRoom.getCurrentOccupantInfo() + "</div>" +
                        editHint +
                        "</div></html>";
                setToolTipText(tooltip);
            } else {
                setCursor(Cursor.getDefaultCursor());
                setToolTipText(null);
            }
            repaint();
        }
    }

    private void handleMousePressed(MouseEvent e) {
        Point logical = screenToLogical(e.getX(), e.getY());
        int logicalX = logical.x;
        int logicalY = logical.y;

        if (layoutEditMode && selectedRoom != null && isNearBottomRightCorner(selectedRoom, logicalX, logicalY)) {
            isResizing = true;
            isDragging = false;
            return;
        }

        Room found = findRoomAt(logicalX, logicalY);
        selectedRoom = found;
        isResizing = false;
        if (selectedRoom != null) {
            if (listener != null) {
                listener.onRoomSelected(selectedRoom);
            }
            if (layoutEditMode) {
                dragOffset = new Point(logicalX - selectedRoom.getPosX(), logicalY - selectedRoom.getPosY());
                isDragging = true;
            }
        } else {
            isDragging = false;
            if (listener != null) {
                listener.onRoomSelected(null);
            }
        }
        repaint();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (!layoutEditMode || selectedRoom == null) return;

        Point logical = screenToLogical(e.getX(), e.getY());
        int logicalX = logical.x;
        int logicalY = logical.y;

        if (isResizing) {
            int newW = Math.max(70, Math.min(1000, logicalX - selectedRoom.getPosX()));
            int newH = Math.max(50, Math.min(800, logicalY - selectedRoom.getPosY()));
            selectedRoom.setWidth(newW);
            selectedRoom.setHeight(newH);
            if (listener != null) {
                listener.onRoomMoved(selectedRoom, selectedRoom.getPosX(), selectedRoom.getPosY());
            }
            repaint();
        } else if (isDragging && dragOffset != null) {
            int canvasLimitW = (floorImage != null) ? Math.max(floorImage.getWidth(), 1200) : 1200;
            int canvasLimitH = (floorImage != null) ? Math.max(floorImage.getHeight(), 800) : 800;
            int maxX = canvasLimitW - selectedRoom.getWidth() - 10;
            int maxY = canvasLimitH - selectedRoom.getHeight() - 10;
            int newX = Math.max(10, Math.min(maxX, logicalX - dragOffset.x));
            int newY = Math.max(10, Math.min(maxY, logicalY - dragOffset.y));
            selectedRoom.setPosX(newX);
            selectedRoom.setPosY(newY);
            if (listener != null) {
                listener.onRoomMoved(selectedRoom, newX, newY);
            }
            repaint();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (layoutEditMode && (isDragging || isResizing) && selectedRoom != null) {
            isDragging = false;
            isResizing = false;
            updateCanvasDimension();
            if (listener != null) {
                listener.onRoomMoved(selectedRoom, selectedRoom.getPosX(), selectedRoom.getPosY());
            }
            repaint();
        }
    }

    private Room findRoomAt(int x, int y) {
        // Cari dari belakang agar ruangan yang berada di atas mendapat prioritas
        for (int i = rooms.size() - 1; i >= 0; i--) {
            Room r = rooms.get(i);
            if (r.containsPoint(x, y)) {
                return r;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Terapkan Zoom Scaling
        g2.scale(zoomLevel, zoomLevel);

        int w = (logicalWidth > 0) ? logicalWidth : 1050;
        int h = (logicalHeight > 0) ? logicalHeight : 650;

        // Terapkan Rotasi POV Pengguna (0, 90, 180, 270 derajat)
        if (rotationDegrees == 90) {
            g2.translate(h, 0);
            g2.rotate(Math.toRadians(90));
        } else if (rotationDegrees == 180) {
            g2.translate(w, h);
            g2.rotate(Math.toRadians(180));
        } else if (rotationDegrees == 270) {
            g2.translate(0, w);
            g2.rotate(Math.toRadians(270));
        }

        // 1. Gambar latar belakang denah (Blueprint atau fallback grid)
        if (floorImage != null) {
            int imgW = floorImage.getWidth();
            int imgH = floorImage.getHeight();

            // Jika canvas Swing lebih lebar/tinggi dari dimensi gambar, gambar grid canvas arsitektur
            if (w > imgW || h > imgH) {
                drawFallbackGrid(g2, Math.max(w, imgW), Math.max(h, imgH));
            }

            // Gambar denah blueprint dalam dimensi alaminya (1:1 pixel-perfect)
            g2.drawImage(floorImage, 0, 0, imgW, imgH, null);

            // Garis batas blueprint denah
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(0, 0, imgW, imgH);
        } else {
            drawFallbackGrid(g2, w, h);
        }

        // 2. Gambar setiap Ruangan
        for (Room room : rooms) {
            drawRoomBox(g2, room);
        }

        g2.dispose();
    }

    private void drawRoomBox(Graphics2D g2, Room r) {
        int rx = r.getPosX();
        int ry = r.getPosY();
        int rw = r.getWidth();
        int rh = r.getHeight();

        boolean isHovered = (r == hoveredRoom);
        boolean isSelected = (r == selectedRoom);
        boolean isSearchHighlighted = (r == searchHighlightRoom);
        boolean isAvail = r.isAvailable();

        // 1. Soft Layered Shadows (Modern Elevation)
        if (isSearchHighlighted) {
            g2.setColor(new Color(245, 158, 11, 80));
            g2.fillRoundRect(rx - 6, ry - 6, rw + 12, rh + 12, 18, 18);
            g2.setColor(new Color(234, 88, 12, 140));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawRoundRect(rx - 5, ry - 5, rw + 10, rh + 10, 16, 16);
        } else if (isSelected) {
            g2.setColor(new Color(245, 158, 11, 40));
            g2.fillRoundRect(rx - 3, ry - 3, rw + 6, rh + 6, 16, 16);
            g2.setColor(new Color(15, 23, 42, 25));
            g2.fillRoundRect(rx, ry + 4, rw, rh + 2, 14, 14);
        } else if (isHovered) {
            g2.setColor(new Color(56, 189, 248, 45));
            g2.fillRoundRect(rx - 2, ry - 2, rw + 4, rh + 4, 14, 14);
            g2.setColor(new Color(15, 23, 42, 28));
            g2.fillRoundRect(rx, ry + 4, rw, rh + 2, 14, 14);
        } else {
            g2.setColor(new Color(15, 23, 42, 14));
            g2.fillRoundRect(rx, ry + 3, rw, rh + 1, 14, 14);
            g2.setColor(new Color(15, 23, 42, 20));
            g2.fillRoundRect(rx, ry + 1, rw, rh, 12, 12);
        }

        // 2. Fill background ruangan (Hijau vs Merah)
        Color fillColor = isAvail ? COLOR_AVAILABLE_BG : COLOR_OCCUPIED_BG;
        g2.setColor(fillColor);
        g2.fillRoundRect(rx, ry, rw, rh, 12, 12);

        // 3. Borders & Focus Rings
        if (isSearchHighlighted || isSelected) {
            g2.setColor(COLOR_SELECTED_BORDER);
            g2.setStroke(new BasicStroke(isSearchHighlighted ? 3.0f : 2.5f));
            g2.drawRoundRect(rx, ry, rw, rh, 12, 12);
        } else if (isHovered) {
            g2.setColor(COLOR_HOVER_BORDER);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(rx, ry, rw, rh, 12, 12);
        } else {
            g2.setColor(isAvail ? COLOR_AVAILABLE_BORDER : COLOR_OCCUPIED_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(rx, ry, rw, rh, 12, 12);
        }

        // 4. Level-of-Detail (LOD) Adaptive Content Rendering
        RoomRenderTier tier = determineRenderTier(r);
        Shape previousClip = g2.getClip();
        g2.clip(new java.awt.geom.RoundRectangle2D.Float(rx, ry, rw, rh, 12, 12));

        switch (tier) {
            case COMPACT:
                renderCompactRoomContent(g2, r, rx, ry, rw, rh, isAvail);
                break;
            case MEDIUM:
                renderMediumRoomContent(g2, r, rx, ry, rw, rh, isAvail);
                break;
            case FULL:
            default:
                renderFullRoomContent(g2, r, rx, ry, rw, rh, isAvail);
                break;
        }

        // Jika mode edit layout, gambar indikator koordinat hanya jika muat tanpa tabrakan
        if (layoutEditMode && rh >= 95 && rw >= 120) {
            g2.setColor(Color.WHITE);
            g2.setFont(ModernUIHelper.fontBold(9));
            String coordStr = fitTextToWidth(g2, "(" + rx + "," + ry + ") " + rw + "x" + rh, rw - 16);
            g2.drawString(coordStr, rx + 8, ry + rh - 33);
        }

        g2.setClip(previousClip);

        // Selection floating dimension chip & corner handles (digambar di luar clip kotak)
        if (layoutEditMode && isSelected) {
            // Floating dimension chip di atas objek (ala Figma / Excalidraw)
            String dimPill = rw + " × " + rh + " px";
            g2.setFont(ModernUIHelper.fontBold(10));
            int dimW = g2.getFontMetrics().stringWidth(dimPill);
            int dimPillX = rx + Math.max(0, (rw - dimW) / 2 - 8);
            int dimPillY = Math.max(4, ry - 22);
            g2.setColor(new Color(15, 23, 42, 220));
            g2.fillRoundRect(dimPillX, dimPillY, dimW + 16, 18, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(dimPill, dimPillX + 8, dimPillY + 13);

            // Gambar corner handles modern
            g2.setColor(COLOR_SELECTED_BORDER);
            int handleSize = 8;
            g2.fillRect(rx - 4, ry - 4, handleSize, handleSize);
            g2.fillRect(rx + rw - 4, ry - 4, handleSize, handleSize);
            g2.fillRect(rx - 4, ry + rh - 4, handleSize, handleSize);
            // Handle resize di sudut kanan bawah diberi aksen kontras
            g2.setColor(new Color(234, 88, 12));
            g2.fillRect(rx + rw - 5, ry + rh - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRect(rx + rw - 5, ry + rh - 5, 10, 10);
        }
    }

    public enum RoomRenderTier {
        COMPACT, MEDIUM, FULL
    }

    public RoomRenderTier determineRenderTier(Room r) {
        if (r == null) return RoomRenderTier.COMPACT;
        int rw = r.getWidth();
        int rh = r.getHeight();
        if (rh < 68 || rw < 95) {
            return RoomRenderTier.COMPACT;
        }
        if (rh < 110 || rw < 155) {
            return RoomRenderTier.MEDIUM;
        }
        return RoomRenderTier.FULL;
    }

    private void renderCompactRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
        // Status Dot di pojok kanan atas
        int dotSize = 8;
        g2.setColor(isAvail ? new Color(52, 211, 153) : new Color(248, 113, 113));
        g2.fillOval(rx + rw - dotSize - 6, ry + 6, dotSize, dotSize);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawOval(rx + rw - dotSize - 6, ry + 6, dotSize, dotSize);

        // Hanya kode / nama ruangan di tengah kotak dengan font adaptif
        int fontSize = Math.max(9, Math.min(12, Math.min(rw / 7, rh / 4)));
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String label = (r.getCode() != null && !r.getCode().trim().isEmpty()) ? r.getCode() : r.getName();
        String fittedCode = fitTextToWidth(g2, label, rw - 16);
        int textW = fm.stringWidth(fittedCode);
        int textX = rx + Math.max(4, (rw - textW) / 2);

        if (rh >= 95 && r.getName() != null && !r.getName().equalsIgnoreCase(label)) {
            int centerY = ry + rh / 2;
            g2.drawString(fittedCode, textX, centerY - 2);
            g2.setFont(ModernUIHelper.fontPlain(Math.max(8, fontSize - 2)));
            FontMetrics fmSub = g2.getFontMetrics();
            String fittedName = fitTextToWidth(g2, r.getName(), rw - 12);
            int subX = rx + Math.max(4, (rw - fmSub.stringWidth(fittedName)) / 2);
            g2.setColor(new Color(241, 245, 249, 225));
            g2.drawString(fittedName, subX, centerY + fmSub.getAscent() + 2);
        } else {
            int textY = ry + ((rh - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(fittedCode, textX, textY);
        }
    }

    private void renderMediumRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
        // 1. Header Strip Minimalis (tanpa pill kapasitas agar tidak bertabrakan)
        int headerH = 22;
        g2.setColor(new Color(15, 23, 42, 155));
        g2.fillRoundRect(rx, ry, rw, headerH, 12, 12);
        g2.fillRect(rx, ry + 12, rw, 10);

        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(11));
        String fittedCode = fitTextToWidth(g2, r.getCode(), rw - 24);
        g2.drawString(fittedCode, rx + 8, ry + 15);

        // Mini status dot di pojok kanan header
        int dotSize = 6;
        g2.setColor(isAvail ? new Color(52, 211, 153) : new Color(248, 113, 113));
        g2.fillOval(rx + rw - dotSize - 8, ry + (headerH - dotSize) / 2, dotSize, dotSize);

        // 2. Nama Ruangan (1 Baris terpotong aman)
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(10));
        String fittedName = fitTextToWidth(g2, r.getName(), rw - 16);
        g2.drawString(fittedName, rx + 8, ry + headerH + 15);

        // 3. Status Ringkas di Bagian Bawah (bila tinggi mencukupi)
        if (rh >= 86 && rw >= 110) {
            drawStatusBadge(g2, rx + 8, ry + rh - 24, rw - 16, 18, isAvail, 9);
        } else {
            g2.setFont(ModernUIHelper.fontBold(9));
            g2.setColor(isAvail ? new Color(209, 250, 229) : new Color(254, 226, 226));
            g2.drawString(isAvail ? "● TERSEDIA" : "■ TERPAKAI", rx + 8, ry + rh - 7);
        }
    }

    private void renderFullRoomContent(Graphics2D g2, Room r, int rx, int ry, int rw, int rh, boolean isAvail) {
        // 1. Header Glass Strip
        g2.setColor(new Color(15, 23, 42, 150));
        g2.fillRoundRect(rx, ry, rw, 28, 12, 12);
        g2.fillRect(rx, ry + 16, rw, 12);

        // Badge Kapasitas di pojok kanan atas strip
        String capStr = r.getCapacity() + " Kursi";
        g2.setFont(ModernUIHelper.fontBold(9));
        FontMetrics fmCap = g2.getFontMetrics();
        int capW = fmCap.stringWidth(capStr);
        int pillX = rx + rw - capW - 16;
        int pillY = ry + 5;
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRoundRect(pillX, pillY, capW + 10, 18, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString(capStr, pillX + 5, pillY + 13);

        // Teks Kode Ruangan (dibatasi agar tidak menabrak badge kapasitas)
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(13));
        int maxCodeW = Math.max(30, pillX - rx - 14);
        g2.drawString(fitTextToWidth(g2, r.getCode(), maxCodeW), rx + 10, ry + 19);

        // 2. Nama Ruangan
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(11));
        g2.drawString(fitTextToWidth(g2, r.getName(), rw - 20), rx + 10, ry + 48);

        // 3. Tipe Ruangan
        g2.setColor(new Color(241, 245, 249, 210));
        g2.setFont(ModernUIHelper.fontPlain(10));
        g2.drawString(fitTextToWidth(g2, r.getRoomType(), rw - 20), rx + 10, ry + 64);

        // 4. Modern Status Badge di Bagian Bawah Box
        drawStatusBadge(g2, rx + 10, ry + rh - 30, rw - 20, 22, isAvail, 10);
    }

    private void drawStatusBadge(Graphics2D g2, int bx, int by, int bw, int bh, boolean isAvail, int fontSize) {
        g2.setColor(isAvail ? new Color(6, 78, 59, 220) : new Color(127, 29, 29, 230));
        g2.fillRoundRect(bx, by, bw, bh, 8, 8);

        int dotSize = 6;
        String text = isAvail ? "TERSEDIA" : "TERPAKAI";
        g2.setFont(ModernUIHelper.fontBold(fontSize));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int contentW = dotSize + 6 + textW;
        int startX = bx + Math.max(4, (bw - contentW) / 2);

        g2.setColor(isAvail ? new Color(52, 211, 153) : new Color(248, 113, 113));
        g2.fillOval(startX, by + (bh - dotSize) / 2, dotSize, dotSize);
        g2.setColor(isAvail ? new Color(209, 250, 229) : new Color(254, 226, 226));
        g2.drawString(text, startX + dotSize + 6, by + ((bh - fm.getHeight()) / 2) + fm.getAscent());
    }

    private String fitTextToWidth(Graphics2D g2, String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (maxWidth <= 10) return "";
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        if (fm.stringWidth(ellipsis) > maxWidth) return "";
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i) + ellipsis;
            if (fm.stringWidth(sub) <= maxWidth) {
                return sub;
            }
        }
        return text.substring(0, 1);
    }

    private void drawFallbackGrid(Graphics2D g2, int w, int h) {
        // Excalidraw-inspired subtle dot/architect canvas backdrop
        g2.setColor(new Color(248, 250, 252));
        g2.fillRect(0, 0, w, h);

        // Dot grid pattern
        g2.setColor(new Color(218, 226, 236));
        for (int x = 20; x < w; x += 20) {
            for (int y = 20; y < h; y += 20) {
                g2.fillRect(x, y, 1, 1);
            }
        }

        // Major grid lines
        g2.setColor(new Color(226, 232, 240, 120));
        for (int x = 0; x < w; x += 100) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 100) {
            g2.drawLine(0, y, w, y);
        }

        // Gambar koridor tengah blueprint
        g2.setColor(new Color(226, 232, 240, 180));
        g2.fillRect(40, 240, w - 80, 80);
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(ModernUIHelper.fontBold(12));
        g2.drawString("SELASAR / KORIDOR UTAMA", (w / 2) - 80, 285);
    }
}
