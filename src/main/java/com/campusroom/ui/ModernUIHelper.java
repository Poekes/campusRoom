package com.campusroom.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * ModernUIHelper - Utilitas styling terpadu untuk estetika modern, minimalis, dan elegan.
 * Menyediakan palet warna standar SaaS/Enterprise, icon vector kustom,
 * avatar inisial anti-aliased, renderer tabel badge pill, dan factory komponen modern.
 */
public class ModernUIHelper {

    // Palet Warna UI Modern Minimalis (Tailwind Slate & Vibrant Accent Theme)
    public static final Color SIDEBAR_BG = new Color(11, 15, 25);          // Deep Slate 950
    public static final Color SIDEBAR_CARD = new Color(19, 26, 42);        // Rich Slate Card
    public static final Color SIDEBAR_BORDER = new Color(30, 41, 59, 180); // Slate 800 subtle border
    public static final Color SIDEBAR_ACTIVE = new Color(56, 189, 248);    // Sky 400
    public static final Color SIDEBAR_ACTIVE_BG = new Color(30, 41, 59);    // Slate 800 active background
    public static final Color SIDEBAR_ACTIVE_BORDER = new Color(59, 130, 246, 100);
    public static final Color SIDEBAR_HOVER = new Color(255, 255, 255, 12);

    public static final Color TEXT_WHITE = Color.WHITE;
    public static final Color TEXT_MUTED = new Color(148, 163, 184);       // Slate 400
    public static final Color TEXT_DARK = new Color(15, 23, 42);           // Slate 900
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);   // Slate 500

    public static final Color BG_PAGE = new Color(248, 250, 252);          // Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER_LIGHT = new Color(226, 232, 240);     // Slate 200
    public static final Color BORDER_SUBTLE = new Color(241, 245, 249);    // Slate 100

    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Royal Blue 600
    public static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);// Royal Blue 700
    public static final Color COLOR_PRIMARY_LIGHT = new Color(239, 246, 255);// Blue 50
    public static final Color COLOR_SUCCESS = new Color(16, 185, 129);     // Emerald 500
    public static final Color COLOR_SUCCESS_LIGHT = new Color(236, 253, 245);// Emerald 50
    public static final Color COLOR_WARNING = new Color(245, 158, 11);     // Amber 500
    public static final Color COLOR_WARNING_LIGHT = new Color(254, 243, 199);// Amber 100
    public static final Color COLOR_DANGER = new Color(239, 68, 68);       // Rose 500
    public static final Color COLOR_DANGER_LIGHT = new Color(254, 242, 242); // Rose 50
    public static final Color COLOR_PURPLE = new Color(124, 58, 237);      // Purple 600
    public static final Color COLOR_PURPLE_LIGHT = new Color(243, 232, 255);// Purple 100
    public static final Color COLOR_SKY = new Color(2, 132, 199);          // Sky 600
    public static final Color COLOR_SKY_LIGHT = new Color(224, 242, 254);   // Sky 100

    /**
     * Terapkan styling modern ke JTable:
     * - Row height lega (34px)
     * - Header styled minimalis dengan font bold dan latar Slate 100
     * - Striping baris lembut
     * - Selection warna lembut (Indigo 100)
     * - Garis horizontal tipis
     */
    public static void styleModernTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER_SUBTLE);
        table.setSelectionBackground(new Color(224, 231, 255)); // Indigo 100
        table.setSelectionForeground(TEXT_DARK);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBackground(new Color(241, 245, 249)); // Slate 100
        header.setForeground(new Color(71, 85, 105));   // Slate 600
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        // Center / Left cell padding
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    lbl.setBackground((row % 2 == 0) ? Color.WHITE : new Color(248, 250, 252));
                    lbl.setForeground(TEXT_DARK);
                }
                return lbl;
            }
        });
    }

    /**
     * Custom TableCellRenderer untuk kolom status agar ditampilkan sebagai badge pill elegan
     * dengan titik indikator (dot) geometris anti-aliased bebas dari bug emoji hilang di OS Linux.
     */
    public static DefaultTableCellRenderer createStatusBadgeRenderer() {
        return new DefaultTableCellRenderer() {
            private final JPanel pillPanel = new JPanel(new GridBagLayout());
            private Color dotColor = null;
            private final JLabel lblPill = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                    if (dotColor != null) {
                        g2.setColor(dotColor);
                        int dotSize = 6;
                        int dotY = (getHeight() - dotSize) / 2;
                        g2.fillOval(8, dotY, dotSize, dotSize);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            {
                pillPanel.setOpaque(true);
                lblPill.setOpaque(false);
                lblPill.setHorizontalAlignment(SwingConstants.CENTER);
                lblPill.setFont(new Font("SansSerif", Font.BOLD, 10));
                lblPill.setBorder(new EmptyBorder(3, 18, 3, 10));
                pillPanel.add(lblPill);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String raw = (value != null) ? value.toString() : "-";
                // Bersihkan karakter emoji jika ada agar layout dot konsisten
                String val = raw.replace("🟢", "").replace("🔴", "").replace("🟡", "").replace("■", "").replace("●", "").trim();
                String displayVal = val;
                String upper = val.toUpperCase();
                if (upper.contains("KOSONG / TERSEDIA") || "KOSONG".equals(upper)) {
                    displayVal = "TERSEDIA";
                } else if (upper.contains("TERPAKAI / DIPAKAI")) {
                    displayVal = "TERPAKAI";
                } else if (upper.contains("DIVALIDASI") && upper.contains("ADMIN")) {
                    displayVal = "Divalidasi (Menunggu Admin)";
                } else if (upper.contains("MENUNGGU") && upper.contains("SEKDEK")) {
                    displayVal = "Menunggu Validasi Sekdek";
                }
                lblPill.setText(displayVal);
                pillPanel.setToolTipText(raw);
                lblPill.setToolTipText(raw);

                Color bgPill;
                Color fgPill;
                if (upper.contains("KOSONG") || upper.contains("TERSEDIA") || upper.contains("DISETUJUI") || upper.contains("APPROVED")) {
                    bgPill = new Color(220, 252, 231); // Green 100
                    fgPill = new Color(22, 101, 52);   // Green 800
                    dotColor = new Color(16, 185, 129); // Green 500
                } else if (upper.contains("TERPAKAI") || upper.contains("DITOLAK") || upper.contains("REJECTED")) {
                    bgPill = new Color(254, 226, 226); // Red 100
                    fgPill = new Color(153, 27, 27);   // Red 800
                    dotColor = new Color(239, 68, 68);  // Red 500
                } else if (upper.contains("SEKDEK") || upper.contains("VALIDASI") || upper.contains("MENUNGGU")) {
                    bgPill = new Color(254, 243, 199); // Amber 100
                    fgPill = new Color(146, 64, 14);   // Amber 800
                    dotColor = new Color(245, 158, 11); // Amber 500
                } else {
                    bgPill = new Color(241, 245, 249); // Slate 100
                    fgPill = new Color(71, 85, 105);   // Slate 600
                    dotColor = new Color(148, 163, 184); // Slate 400
                }

                lblPill.setBackground(bgPill);
                lblPill.setForeground(fgPill);

                Color rowBg = isSelected ? table.getSelectionBackground() : ((row % 2 == 0) ? Color.WHITE : new Color(248, 250, 252));
                pillPanel.setBackground(rowBg);

                return pillPanel;
            }
        };
    }

    /**
     * Membuat Badge Pill bergaya modern dengan titik indikator geometris anti-aliased.
     */
    public static JLabel createIndicatorPill(String text, Color dotColor, Color bgPill, Color fgPill) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                if (dotColor != null) {
                    g2.setColor(dotColor);
                    int dotSize = 6;
                    int dotY = (getHeight() - dotSize) / 2;
                    g2.fillOval(8, dotY, dotSize, dotSize);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setOpaque(false);
        lbl.setBackground(bgPill);
        lbl.setForeground(fgPill);
        lbl.setBorder(new EmptyBorder(3, dotColor != null ? 18 : 10, 3, 10));
        return lbl;
    }

    /**
     * Membuat Banner / Callout Card modern dengan garis aksen di sisi kiri.
     */
    public static JPanel createCalloutCard(String title, String message, Color accentColor, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1),
                        new EmptyBorder(10, 14, 10, 14)
                )
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(accentColor.darker());

        JLabel lblMsg = new JLabel("<html>" + message + "</html>");
        lblMsg.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblMsg.setForeground(new Color(51, 65, 85));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblMsg, BorderLayout.CENTER);
        return card;
    }

    /**
     * Membuat Tombol Aksi bergaya modern Flat dengan warna, border halus, dan hover visual interaktif.
     */
    public static JButton createStyledButton(String text, Color bg, Color fg, int fontSize, boolean bold) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color currentBg = getBackground();
                if (!isEnabled()) {
                    currentBg = new Color(203, 213, 225); // Slate 300 disabled
                } else if (getModel().isPressed()) {
                    currentBg = currentBg.darker();
                } else if (getModel().isRollover()) {
                    currentBg = new Color(
                            Math.min(255, (int)(currentBg.getRed() * 0.93)),
                            Math.min(255, (int)(currentBg.getGreen() * 0.93)),
                            Math.min(255, (int)(currentBg.getBlue() * 0.93))
                    );
                }
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, fontSize));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 18), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        return btn;
    }

    /**
     * Membuat Page Header modern untuk setiap view: Title, subtitle, dan wadah aksi di kanan.
     */
    public static JPanel createPageHeader(String title, String subtitle, JComponent rightComponent) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 4, 12, 4));

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTitle.setForeground(TEXT_DARK);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSub.setForeground(TEXT_SECONDARY);

        titleCol.add(lblTitle);
        titleCol.add(Box.createVerticalStrut(2));
        titleCol.add(lblSub);

        panel.add(titleCol, BorderLayout.CENTER);
        if (rightComponent != null) {
            panel.add(rightComponent, BorderLayout.EAST);
        }

        return panel;
    }

    /**
     * Membuat Avatar Bulat dengan inisial nama dan warna aksen peran pengguna.
     * Mengeliminasi ketergantungan pada font emoji OS yang sering hilang di Linux.
     */
    public static JComponent createAvatarComponent(String fullName, String role, int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setMinimumSize(new Dimension(size, size));
                setMaximumSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color c1;
                Color c2;
                if ("MAHASISWA".equalsIgnoreCase(role)) {
                    c1 = new Color(2, 132, 199);   // Sky 600
                    c2 = new Color(56, 189, 248);  // Sky 400
                } else if ("SEKDEK".equalsIgnoreCase(role)) {
                    c1 = new Color(124, 58, 237);  // Purple 600
                    c2 = new Color(168, 85, 247);  // Purple 400
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    c1 = new Color(16, 185, 129);  // Emerald 500
                    c2 = new Color(52, 211, 153);  // Emerald 400
                } else {
                    c1 = new Color(71, 85, 105);   // Slate 600
                    c2 = new Color(100, 116, 139); // Slate 500
                }

                // Lingkaran avatar dengan gradient diagonal
                GradientPaint gp = new GradientPaint(0, 0, c1, size, size, c2);
                g2.setPaint(gp);
                g2.fillOval(1, 1, size - 2, size - 2);

                // Subtle inner border
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawOval(1, 1, size - 3, size - 3);

                // Inisial nama
                String initials = getInitials(fullName);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, size * 2 / 5));
                FontMetrics fm = g2.getFontMetrics();
                int x = (size - fm.stringWidth(initials)) / 2;
                int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, x, y);

                g2.dispose();
            }
        };
    }

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "CR";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /**
     * Vector Icon Painter untuk icon sidebar modern yang tajam di seluruh sistem operasi.
     */
    public static class VectorIcon implements Icon {
        public enum Type {
            FLOOR_PLAN, CATALOG, BOOKING, VALIDATION, SCHEDULE, APPROVAL, CAMPUS, ROOMS, EDITOR
        }

        private final Type type;
        private final int size;
        private Color color = new Color(203, 213, 225);

        public VectorIcon(Type type, int size) {
            this.type = type;
            this.size = size;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int s = size;
            int pad = 2;

            switch (type) {
                case FLOOR_PLAN:
                    // Peta lantai 4 kuadran
                    g2.drawRoundRect(x + pad, y + pad, s - pad * 2, s - pad * 2, 4, 4);
                    g2.drawLine(x + s / 2, y + pad, x + s / 2, y + s - pad);
                    g2.drawLine(x + pad, y + s / 2, x + s - pad, y + s / 2);
                    break;
                case CATALOG:
                    // Grid / List tabel
                    g2.drawRoundRect(x + pad, y + pad, s - pad * 2, s - pad * 2, 3, 3);
                    g2.drawLine(x + pad + 3, y + pad + 4, x + s - pad - 3, y + pad + 4);
                    g2.drawLine(x + pad + 3, y + s / 2, x + s - pad - 3, y + s / 2);
                    g2.drawLine(x + pad + 3, y + s - pad - 4, x + s - pad - 3, y + s - pad - 4);
                    break;
                case BOOKING:
                    // Dokumen pensil
                    g2.drawRoundRect(x + pad + 1, y + pad, s - pad * 2 - 3, s - pad * 2, 3, 3);
                    g2.drawLine(x + pad + 4, y + pad + 5, x + s - pad - 5, y + pad + 5);
                    g2.drawLine(x + pad + 4, y + pad + 9, x + s - pad - 5, y + pad + 9);
                    g2.drawLine(x + pad + 4, y + pad + 13, x + pad + 8, y + pad + 13);
                    break;
                case VALIDATION:
                    // Clipboard dengan centang
                    g2.drawRoundRect(x + pad + 1, y + pad + 2, s - pad * 2 - 2, s - pad * 2 - 2, 3, 3);
                    g2.drawLine(x + pad + 4, y + pad, x + s - pad - 4, y + pad);
                    // Checkmark
                    g2.drawLine(x + pad + 4, y + s / 2 + 1, x + pad + 7, y + s - pad - 4);
                    g2.drawLine(x + pad + 7, y + s - pad - 4, x + s - pad - 3, y + pad + 6);
                    break;
                case SCHEDULE:
                    // Kalender / Jam
                    g2.drawRoundRect(x + pad, y + pad + 2, s - pad * 2, s - pad * 2 - 2, 3, 3);
                    g2.drawLine(x + pad, y + pad + 6, x + s - pad, y + pad + 6);
                    g2.drawLine(x + pad + 4, y + pad, x + pad + 4, y + pad + 3);
                    g2.drawLine(x + s - pad - 4, y + pad, x + s - pad - 4, y + pad + 3);
                    break;
                case APPROVAL:
                    // Perisai / Badge persetujuan
                    int[] xPoints = {x + s / 2, x + s - pad - 1, x + s - pad - 1, x + s / 2, x + pad + 1, x + pad + 1};
                    int[] yPoints = {y + pad, y + pad + 3, y + s - pad - 5, y + s - pad, y + s - pad - 5, y + pad + 3};
                    g2.drawPolygon(xPoints, yPoints, 6);
                    // Checkmark inside
                    g2.drawLine(x + s / 2 - 3, y + s / 2, x + s / 2 - 1, y + s / 2 + 3);
                    g2.drawLine(x + s / 2 - 1, y + s / 2 + 3, x + s / 2 + 4, y + s / 2 - 2);
                    break;
                case CAMPUS:
                    // Gedung pilar kampus
                    g2.drawLine(x + pad, y + pad + 4, x + s / 2, y + pad);
                    g2.drawLine(x + s / 2, y + pad, x + s - pad, y + pad + 4);
                    g2.drawLine(x + pad, y + pad + 4, x + s - pad, y + pad + 4);
                    // Pilar
                    g2.drawLine(x + pad + 2, y + pad + 5, x + pad + 2, y + s - pad);
                    g2.drawLine(x + s / 2, y + pad + 5, x + s / 2, y + s - pad);
                    g2.drawLine(x + s - pad - 2, y + pad + 5, x + s - pad - 2, y + s - pad);
                    g2.drawLine(x + pad, y + s - pad, x + s - pad, y + s - pad);
                    break;
                case ROOMS:
                    // Pintu terbuka / Ruangan
                    g2.drawRoundRect(x + pad, y + pad, s - pad * 2, s - pad * 2, 2, 2);
                    g2.drawLine(x + pad + 3, y + pad + 2, x + pad + 3, y + s - pad - 2);
                    g2.drawOval(x + s - pad - 6, y + s / 2, 2, 2);
                    break;
                case EDITOR:
                    // Penggaris / Desainer
                    g2.drawRoundRect(x + pad, y + pad + 3, s - pad * 2, s - pad * 2 - 6, 2, 2);
                    g2.drawLine(x + pad + 4, y + pad + 3, x + pad + 4, y + pad + 7);
                    g2.drawLine(x + pad + 8, y + pad + 3, x + pad + 8, y + pad + 6);
                    g2.drawLine(x + pad + 12, y + pad + 3, x + pad + 12, y + pad + 7);
                    break;
            }

            g2.dispose();
        }
    }
}
