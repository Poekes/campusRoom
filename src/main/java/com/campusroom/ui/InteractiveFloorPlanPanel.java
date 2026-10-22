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

    private BufferedImage floorImage;
    private String currentImagePath;
    private List<Room> rooms = new ArrayList<>();
    private Room selectedRoom = null;
    private Room hoveredRoom = null;
    private FloorPlanListener listener;

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
                handleMousePressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedRoom != null && listener != null) {
                    listener.onRoomDoubleClicked(selectedRoom);
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setFloorPlanListener(FloorPlanListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = (rooms != null) ? rooms : new ArrayList<>();
        this.selectedRoom = null;
        this.hoveredRoom = null;
        repaint();
    }

    public void setSelectedRoom(Room room) {
        this.selectedRoom = room;
        repaint();
    }

    public Room getSelectedRoom() {
        return selectedRoom;
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
            setPreferredSize(new Dimension(1050, 650));
            revalidate();
            repaint();
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

        if (floorImage != null) {
            int nw = Math.max(1050, floorImage.getWidth());
            int nh = Math.max(650, floorImage.getHeight());
            setPreferredSize(new Dimension(nw, nh));
        } else {
            setPreferredSize(new Dimension(1050, 650));
        }
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
        if (layoutEditMode && selectedRoom != null && isNearBottomRightCorner(selectedRoom, e.getX(), e.getY())) {
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            setToolTipText("📐 Geser sudut untuk Mengubah Ukuran (Lebar × Tinggi)");
            return;
        }

        Room found = findRoomAt(e.getX(), e.getY());
        if (found != hoveredRoom) {
            hoveredRoom = found;
            if (hoveredRoom != null) {
                setCursor(Cursor.getPredefinedCursor(layoutEditMode ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR));
                String statusStr = hoveredRoom.isAvailable() 
                        ? "<span style='color:#059669; font-weight:bold;'>● TERSEDIA</span>" 
                        : "<span style='color:#dc2626; font-weight:bold;'>■ TERPAKAI</span>";
                String editHint = layoutEditMode ? "<br/><span style='color:#0284c7; font-size:10px;'>💡 <b>Double-click</b> untuk buka Properti Ruangan (Modal)</span>" : "";
                String tooltip = "<html><body style='font-family:sans-serif; padding:4px;'>" +
                        "<b style='font-size:12px; color:#1e293b;'>" + hoveredRoom.getCode() + " - " + hoveredRoom.getName() + "</b><br/>" +
                        "<b>Tipe:</b> " + hoveredRoom.getRoomType() + " | <b>Kapasitas:</b> " + hoveredRoom.getCapacity() + " Kursi<br/>" +
                        "<b>Fasilitas:</b> " + hoveredRoom.getFacilities() + "<br/>" +
                        "<b>Status:</b> " + statusStr + "<br/>" +
                        "<i style='color:#64748b; font-size:10px;'>" + hoveredRoom.getCurrentOccupantInfo() + "</i>" +
                        editHint +
                        "</body></html>";
                setToolTipText(tooltip);
            } else {
                setCursor(Cursor.getDefaultCursor());
                setToolTipText(null);
            }
            repaint();
        }
    }

    private void handleMousePressed(MouseEvent e) {
        if (layoutEditMode && selectedRoom != null && isNearBottomRightCorner(selectedRoom, e.getX(), e.getY())) {
            isResizing = true;
            isDragging = false;
            return;
        }

        Room found = findRoomAt(e.getX(), e.getY());
        selectedRoom = found;
        isResizing = false;
        if (selectedRoom != null) {
            if (listener != null) {
                listener.onRoomSelected(selectedRoom);
            }
            if (layoutEditMode) {
                dragOffset = new Point(e.getX() - selectedRoom.getPosX(), e.getY() - selectedRoom.getPosY());
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

        if (isResizing) {
            int newW = Math.max(70, Math.min(1000, e.getX() - selectedRoom.getPosX()));
            int newH = Math.max(50, Math.min(800, e.getY() - selectedRoom.getPosY()));
            selectedRoom.setWidth(newW);
            selectedRoom.setHeight(newH);
            if (listener != null) {
                listener.onRoomMoved(selectedRoom, selectedRoom.getPosX(), selectedRoom.getPosY());
            }
            repaint();
        } else if (isDragging && dragOffset != null) {
            int maxX = Math.max(1200, getWidth()) - selectedRoom.getWidth() - 10;
            int maxY = Math.max(800, getHeight()) - selectedRoom.getHeight() - 10;
            int newX = Math.max(10, Math.min(maxX, e.getX() - dragOffset.x));
            int newY = Math.max(10, Math.min(maxY, e.getY() - dragOffset.y));
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

        int w = getWidth();
        int h = getHeight();

        // 1. Gambar latar belakang (Blueprint image atau Grid fallback)
        if (floorImage != null) {
            g2.drawImage(floorImage, 0, 0, w, h, null);
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
        boolean isAvail = r.isAvailable();

        // Shadow halus
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRoundRect(rx + 3, ry + 3, rw, rh, 12, 12);

        // Fill background ruangan (Hijau vs Merah)
        Color fillColor = isAvail ? COLOR_AVAILABLE_BG : COLOR_OCCUPIED_BG;
        g2.setColor(fillColor);
        g2.fillRoundRect(rx, ry, rw, rh, 12, 12);

        // Border
        if (isSelected) {
            g2.setColor(COLOR_SELECTED_BORDER);
            g2.setStroke(new BasicStroke(3.0f));
        } else if (isHovered) {
            g2.setColor(COLOR_HOVER_BORDER);
            g2.setStroke(new BasicStroke(2.5f));
        } else {
            g2.setColor(isAvail ? COLOR_AVAILABLE_BORDER : COLOR_OCCUPIED_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
        }
        g2.drawRoundRect(rx, ry, rw, rh, 12, 12);

        // Label Ruangan di dalam Box
        // Header Strip
        g2.setColor(new Color(15, 23, 42, 140));
        g2.fillRoundRect(rx, ry, rw, 28, 12, 12);
        g2.fillRect(rx, ry + 16, rw, 12); // Menutupi rounded bawah pada strip atas

        // Teks Kode Ruangan (Contoh: R. 201)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(r.getCode(), rx + 10, ry + 19);

        // Badge Kapasitas di pojok kanan atas strip
        String capStr = r.getCapacity() + " Mhs";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        int capStrW = g2.getFontMetrics().stringWidth(capStr);
        g2.drawString(capStr, rx + rw - capStrW - 10, ry + 19);

        // Nama Ruangan
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        String nameStr = r.getName();
        if (g2.getFontMetrics().stringWidth(nameStr) > rw - 16) {
            nameStr = nameStr.substring(0, Math.min(18, nameStr.length())) + "...";
        }
        g2.drawString(nameStr, rx + 10, ry + 48);

        // Tipe Ruangan
        g2.setColor(new Color(241, 245, 249));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString(r.getRoomType(), rx + 10, ry + 64);

        // Badge Status Besar di Bagian Bawah Box
        int badgeH = 22;
        int badgeY = ry + rh - badgeH - 8;
        int badgeW = rw - 20;

        if (isAvail) {
            // Status Hijau: TERSEDIA
            g2.setColor(new Color(6, 78, 59, 210));
            g2.fillRoundRect(rx + 10, badgeY, badgeW, badgeH, 8, 8);
            g2.setColor(new Color(167, 243, 208));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String statusText = "● TERSEDIA";
            int stW = g2.getFontMetrics().stringWidth(statusText);
            g2.drawString(statusText, rx + 10 + Math.max(2, (badgeW - stW) / 2), badgeY + 15);
        } else {
            // Status Merah: TERPAKAI
            g2.setColor(new Color(127, 29, 29, 220));
            g2.fillRoundRect(rx + 10, badgeY, badgeW, badgeH, 8, 8);
            g2.setColor(new Color(254, 202, 202));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String statusText = "■ TERPAKAI";
            int stW = g2.getFontMetrics().stringWidth(statusText);
            g2.drawString(statusText, rx + 10 + Math.max(2, (badgeW - stW) / 2), badgeY + 15);
        }

        // Jika mode edit layout, gambar indikator titik drag koordinat (X, Y)
        if (layoutEditMode) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.drawString("📍 (" + rx + ", " + ry + ") [" + rw + "x" + rh + "]", rx + 10, ry + rh - 34);

            if (isSelected) {
                // Floating dimension chip di atas objek (ala Figma / Excalidraw)
                String dimPill = rw + " × " + rh + " px";
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                int dimW = g2.getFontMetrics().stringWidth(dimPill);
                int pillX = rx + Math.max(0, (rw - dimW) / 2 - 8);
                int pillY = Math.max(4, ry - 22);
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRoundRect(pillX, pillY, dimW + 16, 18, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(dimPill, pillX + 8, pillY + 13);

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
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("SELASAR / KORIDOR UTAMA", (w / 2) - 80, 285);
    }
}
