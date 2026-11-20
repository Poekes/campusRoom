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

    private BufferedImage floorImage;
    private String currentImagePath;
    private List<Room> rooms = new ArrayList<>();
    private Room selectedRoom = null;
    private Room hoveredRoom = null;
    private FloorPlanListener listener;

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

    public void setFloorPlanListener(FloorPlanListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = (rooms != null) ? rooms : new ArrayList<>();
        this.selectedRoom = null;
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
        int scaledW = (int) Math.round(targetW * zoomLevel);
        int scaledH = (int) Math.round(targetH * zoomLevel);
        setPreferredSize(new Dimension(scaledW, scaledH));
        revalidate();
        repaint();
    }

    private boolean isNearBottomRightCorner(Room r, int x, int y) {
        if (r == null) return false;
        int cornerX = r.getPosX() + r.getWidth();
        int cornerY = r.getPosY() + r.getHeight();
        return (x >= cornerX - 10 && x <= cornerX + 6 && y >= cornerY - 10 && y <= cornerY + 6);
    }

    private void handleMouseMoved(MouseEvent e) {
        int logicalX = (int) Math.round(e.getX() / zoomLevel);
        int logicalY = (int) Math.round(e.getY() / zoomLevel);

        if (layoutEditMode && selectedRoom != null && isNearBottomRightCorner(selectedRoom, logicalX, logicalY)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            setToolTipText("📐 Geser sudut untuk Mengubah Ukuran (Lebar × Tinggi)");
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
                String editHint = layoutEditMode ? "<div style='margin-top:6px; padding-top:4px; border-top:1px solid #e2e8f0; color:#0284c7; font-size:10px;'>💡 <b>Double-click</b> untuk buka Properti Ruangan (Modal)</div>" : "";
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
        int logicalX = (int) Math.round(e.getX() / zoomLevel);
        int logicalY = (int) Math.round(e.getY() / zoomLevel);

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

        int logicalX = (int) Math.round(e.getX() / zoomLevel);
        int logicalY = (int) Math.round(e.getY() / zoomLevel);

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

        int unscaledW = (int) Math.ceil(getWidth() / zoomLevel);
        int unscaledH = (int) Math.ceil(getHeight() / zoomLevel);

        // 1. Gambar latar belakang denah (Blueprint atau fallback grid)
        if (floorImage != null) {
            int imgW = floorImage.getWidth();
            int imgH = floorImage.getHeight();

            // Jika canvas Swing lebih lebar/tinggi dari dimensi gambar, gambar grid canvas arsitektur
            if (unscaledW > imgW || unscaledH > imgH) {
                drawFallbackGrid(g2, Math.max(unscaledW, imgW), Math.max(unscaledH, imgH));
            }

            // Gambar denah blueprint dalam dimensi alaminya (1:1 pixel-perfect)
            g2.drawImage(floorImage, 0, 0, imgW, imgH, null);

            // Garis batas blueprint denah
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(0, 0, imgW, imgH);
        } else {
            drawFallbackGrid(g2, unscaledW, unscaledH);
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
        boolean isAvail = r.isAvailable();

        // 1. Soft Layered Shadows (Modern Elevation)
        if (isSelected) {
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
        if (isSelected) {
            g2.setColor(COLOR_SELECTED_BORDER);
            g2.setStroke(new BasicStroke(2.5f));
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

        // 4. Header Glass Strip
        g2.setColor(new Color(15, 23, 42, 150));
        g2.fillRoundRect(rx, ry, rw, 28, 12, 12);
        g2.fillRect(rx, ry + 16, rw, 12); // Menutupi rounded bawah pada strip atas

        // Teks Kode Ruangan (Contoh: R. 201)
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(13));
        g2.drawString(r.getCode(), rx + 10, ry + 19);

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

        // 5. Nama Ruangan
        g2.setColor(Color.WHITE);
        g2.setFont(ModernUIHelper.fontBold(11));
        String nameStr = r.getName();
        if (g2.getFontMetrics().stringWidth(nameStr) > rw - 20) {
            nameStr = nameStr.substring(0, Math.min(16, nameStr.length())) + "...";
        }
        g2.drawString(nameStr, rx + 10, ry + 48);

        // Tipe Ruangan
        g2.setColor(new Color(241, 245, 249, 210));
        g2.setFont(ModernUIHelper.fontPlain(10));
        g2.drawString(r.getRoomType(), rx + 10, ry + 64);

        // 6. Modern Status Badge di Bagian Bawah Box (dengan Geometric Dot anti-aliased)
        int badgeH = 22;
        int badgeY = ry + rh - badgeH - 8;
        int badgeW = rw - 20;

        if (isAvail) {
            // Status Hijau: TERSEDIA
            g2.setColor(new Color(6, 78, 59, 220));
            g2.fillRoundRect(rx + 10, badgeY, badgeW, badgeH, 8, 8);

            int dotSize = 6;
            String text = "TERSEDIA";
            g2.setFont(ModernUIHelper.fontBold(10));
            int textW = g2.getFontMetrics().stringWidth(text);
            int contentW = dotSize + 6 + textW;
            int startX = rx + 10 + Math.max(4, (badgeW - contentW) / 2);

            g2.setColor(new Color(52, 211, 153)); // Emerald 400
            g2.fillOval(startX, badgeY + (badgeH - dotSize) / 2, dotSize, dotSize);
            g2.setColor(new Color(209, 250, 229)); // Emerald 100
            g2.drawString(text, startX + dotSize + 6, badgeY + 15);
        } else {
            // Status Merah: TERPAKAI
            g2.setColor(new Color(127, 29, 29, 230));
            g2.fillRoundRect(rx + 10, badgeY, badgeW, badgeH, 8, 8);

            int dotSize = 6;
            String text = "TERPAKAI";
            g2.setFont(ModernUIHelper.fontBold(10));
            int textW = g2.getFontMetrics().stringWidth(text);
            int contentW = dotSize + 6 + textW;
            int startX = rx + 10 + Math.max(4, (badgeW - contentW) / 2);

            g2.setColor(new Color(248, 113, 113)); // Rose 400
            g2.fillOval(startX, badgeY + (badgeH - dotSize) / 2, dotSize, dotSize);
            g2.setColor(new Color(254, 226, 226)); // Rose 100
            g2.drawString(text, startX + dotSize + 6, badgeY + 15);
        }

        // Jika mode edit layout, gambar indikator titik drag koordinat (X, Y)
        if (layoutEditMode) {
            g2.setColor(Color.WHITE);
            g2.setFont(ModernUIHelper.fontBold(9));
            g2.drawString("📍 (" + rx + ", " + ry + ") [" + rw + "x" + rh + "]", rx + 10, ry + rh - 34);

            if (isSelected) {
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
